package com.avito.runner.scheduler.runner.scheduler

import com.avito.runner.scheduler.runner.model.TestRunRequest
import com.avito.runner.scheduler.runner.scheduler.retry.RetryManager
import com.avito.runner.service.model.DeviceTestCaseRun
import com.avito.runner.service.model.intention.InstrumentationTestRunAction
import com.avito.runner.service.model.intention.Intention
import com.avito.runner.service.model.intention.State

internal interface TestExecutionState {

    val request: TestRunRequest

    fun verdict(incomingTestCaseRun: DeviceTestCaseRun?): Verdict

    sealed class Verdict {
        data class Run(
            val intentions: List<Intention>
        ) : Verdict()

        object DoNothing : Verdict()

        data class SendResult(
            val results: List<DeviceTestCaseRun>
        ) : Verdict()
    }
}

internal class TestExecutionStateImplementation(
    override val request: TestRunRequest,
    private val retryManager: RetryManager,
) : TestExecutionState {

    private val history: MutableList<DeviceTestCaseRun> = mutableListOf()

    private var executionsInProgress = 0

    override fun verdict(incomingTestCaseRun: DeviceTestCaseRun?): TestExecutionState.Verdict {
        if (incomingTestCaseRun != null) {
            addResult(incomingTestCaseRun)
        }

        val retryRemains = retryManager.retryCount(history = history)
        val isCompleted = retryRemains == 0

        return if (isCompleted) {
            TestExecutionState.Verdict.SendResult(
                results = history
            )
        } else {
            val runCount = retryRemains - executionsInProgress

            if (runCount > 0) {
                TestExecutionState.Verdict.Run(
                    intentions = nextRunIntentions(runCount = runCount)
                ).also {
                    executionsInProgress += runCount
                }
            } else {
                TestExecutionState.Verdict.DoNothing
            }
        }
    }

    private fun addResult(result: DeviceTestCaseRun) {
        executionsInProgress--
        history.add(result)
    }

    private fun nextRunIntentions(runCount: Int): List<Intention> {
        return (0 until runCount)
            .map { index -> nextRunIntention(history.size + executionsInProgress + index + 1) }
            .toList()
    }

    private fun nextRunIntention(executionNumber: Int): Intention {

        val layers = mutableListOf(
            State.Layer.ApiLevel(api = request.configuration.api),
            State.Layer.Model(model = request.configuration.model),
            State.Layer.InstalledApplication(
                applicationPath = request.testApplication,
                applicationPackage = request.testPackage
            )
        )

        layers.add(
            State.Layer.InstalledApplication(
                applicationPath = request.application,
                applicationPackage = request.applicationPackage
            )
        )

        return Intention(
            state = State(layers = layers),
            action = InstrumentationTestRunAction(
                test = request.testCase,
                testPackage = request.testPackage,
                targetPackage = request.applicationPackage,
                testArtifactsDirectoryPackage = request.testArtifactsDirectoryPackage,
                testRunner = request.testRunner,
                instrumentationParams = request.instrumentationParameters,
                timeoutMinutes = request.timeoutMinutes,
                executionNumber = executionNumber,
                enableDeviceDebug = request.enableDeviceDebug
            )
        )
    }
}
