package com.avito.runner.service.worker

import com.avito.android.Result
import com.avito.coroutines.extensions.Dispatchers
import com.avito.report.ApplicationDirProviderFactory
import com.avito.report.TestArtifactsProviderFactory
import com.avito.runner.model.TestCaseRun
import com.avito.runner.model.TestCaseRun.Result.Failed
import com.avito.runner.service.IntentionsRouter
import com.avito.runner.service.listener.TestListener
import com.avito.runner.service.model.DeviceTestCaseRun
import com.avito.runner.service.model.intention.InstrumentationTestRunAction
import com.avito.runner.service.model.intention.Intention
import com.avito.runner.service.model.intention.State
import com.avito.runner.service.worker.device.Device
import com.avito.runner.service.worker.device.model.getData
import com.avito.runner.service.worker.listener.DeviceListener
import com.avito.time.TimeProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.coroutineContext

internal class DeviceWorker(
    private val intentionsRouter: IntentionsRouter,
    private val device: Device,
    private val outputDirectory: File,
    private val testListener: TestListener,
    private val deviceListener: DeviceListener,
    private val timeProvider: TimeProvider,
    dispatchers: Dispatchers
) {

    private val dispatcher: CoroutineDispatcher = dispatchers.dispatcher()

    private val stateWorker: DeviceStateWorker = DeviceStateWorker(device)

    suspend fun run() = with(CoroutineScope(coroutineContext)) {
        launch(dispatcher + CoroutineName("device-worker")) {

            var state: State = when (val status = device.deviceStatus()) {

                is Device.DeviceStatus.Freeze -> {
                    deviceListener.onDeviceDied(
                        device = device,
                        message = "DeviceWorker died. Device status is `Freeze`",
                        reason = status.reason
                    )
                    return@launch
                }

                is Device.DeviceStatus.Alive -> device.state()
            }

            deviceListener.onDeviceCreated(device, state)

            try {

                for (intention in intentionsRouter.observeIntentions(state)) {

                    deviceListener.onIntentionReceived(device, intention)

                    when (val status = device.deviceStatus()) {

                        is Device.DeviceStatus.Freeze -> {
                            onDeviceDieWhenPrepareState(intention, status.reason)
                            return@launch
                        }

                        is Device.DeviceStatus.Alive -> prepareDeviceState(
                            currentState = state,
                            intendedState = intention.state
                        ).onFailure { reason ->
                            onDeviceDieWhenPrepareState(intention, reason)
                            return@launch
                        }.onSuccess { newState ->
                            state = newState
                            deviceListener.onStatePrepared(device, newState)
                            deviceListener.onTestStarted(device, intention)
                            executeAction(action = intention.action)
                                .onSuccess { result ->
                                    deviceListener.onTestCompleted(
                                        device = device,
                                        intention = intention,
                                        result = result
                                    )
                                }.onFailure { failure ->
                                    onDeviceDieWhenExecutingTest(intention, failure)
                                    return@launch
                                }
                        }
                    }
                }
            } finally {
                deviceListener.onFinished(device)
            }
        }
    }

    private suspend fun prepareDeviceState(
        currentState: State,
        intendedState: State
    ): Result<State> = if (intendedState.digest != currentState.digest) {

        device.logger.info(
            "Current state=${currentState.digest}, " +
                "intended=${intendedState.digest}. Preparing new state..."
        )

        stateWorker.installApplications(
            state = intendedState,
            onAllSucceeded = { installations ->
                installations.forEach { installation ->
                    deviceListener.onApplicationInstalled(
                        device = device,
                        installation = installation
                    )
                }
            }
        )
    } else {
        device.logger.info(
            "Current state=${currentState.digest}, " +
                "intended=${intendedState.digest}. Clearing packages..."
        )

        stateWorker.clearPackages(currentState)
    }.map { intendedState }

    private suspend fun executeAction(action: InstrumentationTestRunAction): Result<DeviceTestCaseRun> {
        return Result.tryCatch {
            try {

                testListener.started(
                    device = device,
                    targetPackage = action.targetPackage,
                    test = action.test,
                    executionNumber = action.executionNumber
                )

                device.runIsolatedTest(
                    action = action,
                    outputDir = outputDirectory
                )
            } catch (t: Throwable) {
                val now = timeProvider.nowInMillis()

                DeviceTestCaseRun(
                    testCaseRun = TestCaseRun(
                        test = action.test,
                        result = Failed.InfrastructureError.Unexpected(
                            error = RuntimeException("Unexpected infrastructure error", t),
                        ),
                        timestampStartedMilliseconds = now,
                        timestampCompletedMilliseconds = now
                    ),
                    device = device.getData()
                )
            }
        }.flatMap { deviceTestCaseRun ->

            val appDirProvider = ApplicationDirProviderFactory.create(
                api = device.api,
                packageName = action.testArtifactsDirectoryPackage,
            )
            val reportFileProvider = TestArtifactsProviderFactory.createForAdb(
                appDirProvider = appDirProvider,
                name = action.test.name
            )

            val testArtifactsDir = reportFileProvider.provideReportDir()

            testListener.finished(
                device = device,
                test = action.test,
                targetPackage = action.targetPackage,
                result = deviceTestCaseRun.testCaseRun.result,
                durationMilliseconds = deviceTestCaseRun.testCaseRun.durationMilliseconds,
                executionNumber = action.executionNumber,
                testArtifactsDir = testArtifactsDir
            )

            testArtifactsDir.flatMap { dir ->
                device.clearDirectory(remotePath = dir.toPath())
            }.map {
                deviceTestCaseRun
            }
        }
    }

    private suspend fun onDeviceDieWhenPrepareState(
        intention: Intention,
        reason: Throwable
    ) {
        deviceListener.onDeviceDied(
            device = device,
            message = "DeviceWorker died when prepare device for test execution. Can't process intention: $intention",
            reason = reason
        )
        deviceListener.onIntentionFail(
            device = device,
            intention = intention,
            reason = reason
        )
    }

    private suspend fun onDeviceDieWhenExecutingTest(
        intention: Intention,
        reason: Throwable
    ) {
        deviceListener.onDeviceDied(
            device = device,
            message = "DeviceWorker died when executed intention: $intention",
            reason = reason
        )
        deviceListener.onTestCompleted(
            device = device,
            intention = intention,
            result = DeviceTestCaseRun(
                device = device.getData(),
                testCaseRun = TestCaseRun(
                    test = intention.action.test,
                    result = Failed.InfrastructureError.Unexpected(
                        error = reason
                    ),
                    timestampCompletedMilliseconds = timeProvider.nowInSeconds(),
                    timestampStartedMilliseconds = timeProvider.nowInSeconds()
                )
            )
        )
    }

    /**
     * layers order matters
     */
    private fun Device.state(): State =
        State(
            layers = listOf(
                State.Layer.ApiLevel(api = api),
                State.Layer.Model(model = model)
            )
        )
}
