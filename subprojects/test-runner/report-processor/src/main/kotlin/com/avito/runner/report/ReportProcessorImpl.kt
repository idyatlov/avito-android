package com.avito.runner.report

import com.avito.android.Problem
import com.avito.android.asPlainText
import com.avito.android.asRuntimeException
import com.avito.logger.LoggerFactory
import com.avito.logger.create
import com.avito.report.model.AndroidTest
import com.avito.report.model.Incident
import com.avito.report.model.IncidentElement
import com.avito.report.model.TestStaticData
import com.avito.runner.artifacts.TestArtifactsProcessor
import com.avito.runner.logcat.LogcatAccessor
import com.avito.runner.logcat.LogcatProcessor
import com.avito.runner.model.TestCaseRun
import com.avito.runner.model.TestResult
import com.avito.test.model.TestCase
import com.avito.time.TimeProvider
import com.avito.utils.stackTraceToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

internal class ReportProcessorImpl(
    loggerFactory: LoggerFactory,
    private val testSuite: Map<TestCase, TestStaticData>,
    private val testArtifactsProcessor: TestArtifactsProcessor,
    private val logcatProcessor: LogcatProcessor,
    private val timeProvider: TimeProvider,
    private val dispatcher: CoroutineDispatcher
) : ReportProcessor {

    private val logger = loggerFactory.create<ReportProcessorImpl>()

    override fun createTestReport(
        result: TestResult,
        test: TestCase,
        executionNumber: Int,
        logcatAccessor: LogcatAccessor
    ): AndroidTest {

        val testFromSuite = requireNotNull(testSuite[test]) { "Can't find test in suite: $test" }

        return when (result) {
            is TestResult.Complete ->
                testArtifactsProcessor.process(
                    reportDir = result.artifacts,
                    testStaticData = testFromSuite,
                    logcatAccessor = logcatAccessor
                ).getOrElse { throwable ->
                    recoverWithInfrastructureFailure(
                        problem = Problem(
                            shortDescription = "Can't process test artifacts",
                            context = "Processing test artifacts caused unexpected exception",
                            because = "Needs investigation, see cause",
                            throwable = throwable,
                        ),
                        testStaticData = testFromSuite,
                        logcatAccessor = logcatAccessor,
                    )
                }

            is TestResult.Incomplete ->
                with(result.infraError) {

                    val problemBuilder = Problem.Builder(
                        shortDescription = "Can't complete test execution",
                        context = "ReportProcessor handling incomplete test result",
                    )

                    problemBuilder.throwable(error)

                    when (this) {
                        is TestCaseRun.Result.Failed.InfrastructureError.FailedOnParsing ->
                            problemBuilder.because("Can't parse instrumentation output, see underlying exception")

                        is TestCaseRun.Result.Failed.InfrastructureError.FailedOnStart ->
                            problemBuilder.because("Can't start test")

                        is TestCaseRun.Result.Failed.InfrastructureError.Timeout ->
                            problemBuilder.because(
                                "Test didn't finish in time. " +
                                    "Test Runner has hardcoded timeout of $timeoutMin minutes"
                            )

                        is TestCaseRun.Result.Failed.InfrastructureError.FailOnPullingArtifacts ->
                            problemBuilder
                                .because("Can't get report artifacts")
                                .addSolution("MBS-11281 to return tests with such errors back to retry queue")

                        is TestCaseRun.Result.Failed.InfrastructureError.Unexpected -> {
                        }
                    }

                    val problem = problemBuilder.build()

                    logger.warn(problem.asPlainText(), this.error)

                    recoverWithInfrastructureFailure(
                        problem = problem,
                        testStaticData = testFromSuite,
                        logcatAccessor = logcatAccessor
                    )
                }
        }
    }

    private fun recoverWithInfrastructureFailure(
        problem: Problem,
        testStaticData: TestStaticData,
        logcatAccessor: LogcatAccessor
    ): AndroidTest {
        val scope = CoroutineScope(CoroutineName("test-artifacts-failure-${testStaticData.name}") + dispatcher)

        return runBlocking {
            withContext(scope.coroutineContext) {

                val logcat = async {
                    logcatProcessor.process(logcatAccessor, isUploadNeeded = true)
                }

                val now = timeProvider.nowInSeconds()

                AndroidTest.Lost.fromTestStaticData(
                    testStaticData,
                    startTime = now,
                    lastSignalTime = now,
                    logcat = logcat.await(),
                    incident = Incident(
                        type = Incident.Type.INFRASTRUCTURE_ERROR,
                        timestamp = now,
                        trace = problem.throwable?.stackTraceToList()
                            ?: problem.asRuntimeException().stackTraceToList(),
                        chain = listOf(
                            IncidentElement(
                                message = problem.asPlainText()
                            )
                        ),
                        entryList = emptyList()
                    )
                )
            }
        }
    }
}
