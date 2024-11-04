package com.avito.runner.scheduler

import com.avito.android.runner.devices.DevicesProviderFactory
import com.avito.logger.LoggerFactory
import com.avito.report.model.TestStaticData
import com.avito.runner.config.RunnerInputParams
import com.avito.runner.config.TargetConfigurationData
import com.avito.runner.listener.LogListener
import com.avito.runner.listener.ReportArtifactsTestListenerProvider
import com.avito.runner.listener.TestMetricsListener
import com.avito.runner.reservation.DeviceReservationWatcher
import com.avito.runner.scheduler.metrics.InstrumentationMetricsSender
import com.avito.runner.scheduler.metrics.TestSuiteListener
import com.avito.runner.scheduler.report.CompositeReporter
import com.avito.runner.scheduler.report.SummaryReportMakerImpl
import com.avito.runner.scheduler.report.trace.TraceReporter
import com.avito.runner.scheduler.runner.TestRunner
import com.avito.runner.scheduler.runner.TestRunnerExecutionState
import com.avito.runner.scheduler.runner.TestRunnerImpl
import com.avito.runner.scheduler.runner.model.TestRunRequestFactory
import com.avito.runner.scheduler.runner.scheduler.TestExecutionScheduler
import com.avito.runner.service.DeviceWorkerPoolProvider
import com.avito.runner.service.listener.CompositeListener
import com.avito.runner.service.listener.TestListener
import com.avito.runner.service.worker.listener.DeviceListener
import com.avito.time.TimeProvider
import java.io.File

internal class TestRunnerFactoryImpl(
    private val testRunnerOutputDir: File,
    private val timeProvider: TimeProvider,
    private val loggerFactory: LoggerFactory,
    private val testSuiteListener: TestSuiteListener,
    private val deviceListener: DeviceListener,
    private val devicesProviderFactory: DevicesProviderFactory,
    private val testRunnerRequestFactory: TestRunRequestFactory,
    private val executionState: TestRunnerExecutionState,
    private val params: RunnerInputParams,
    private val tempLogcatDir: File,
    private val metricsSender: InstrumentationMetricsSender,
    private val targets: List<TargetConfigurationData>,
    private val artifactsTestListenerProvider: ReportArtifactsTestListenerProvider,
) : TestRunnerFactory {

    override fun createTestRunner(
        tests: List<TestStaticData>
    ): TestRunner {
        val devicesProvider = devicesProviderFactory.create(
            tempLogcatDir = tempLogcatDir,
            deviceWorkerPoolProvider = devicesWorkerPoolProvider(
                testListener(tests)
            )
        )
        return TestRunnerImpl(
            scheduler = TestExecutionScheduler(
                results = executionState.results,
                intentions = executionState.intentions,
                intentionResults = executionState.intentionResults,
            ),
            devicesProvider = devicesProvider,
            reservationWatcher = DeviceReservationWatcher.create(
                reservation = devicesProvider
            ),
            loggerFactory = loggerFactory,
            state = executionState,
            summaryReportMaker = SummaryReportMakerImpl(),
            reporter = CompositeReporter(
                reporters = setOf(
                    TraceReporter(
                        runName = "Tests",
                        outputDirectory = testRunnerOutputDir
                    )
                )
            ),
            testSuiteListener = testSuiteListener,
            testRunRequestFactory = testRunnerRequestFactory,
            targets = targets,
            executionTimeout = params.instrumentationConfiguration.testRunnerExecutionTimeout
        )
    }

    private fun testListener(tests: List<TestStaticData>) = CompositeListener(
        listeners = mutableListOf<TestListener>().apply {
            add(LogListener())
            add(artifactsTestListenerProvider.provide(tests))
            add(TestMetricsListener(metricsSender))
        }
    )

    private fun devicesWorkerPoolProvider(
        testListener: TestListener
    ): DeviceWorkerPoolProvider {
        return DeviceWorkerPoolProvider(
            testRunnerOutputDir = testRunnerOutputDir,
            timeProvider = timeProvider,
            loggerFactory = loggerFactory,
            deviceListener = deviceListener,
            intentions = executionState.intentions,
            intentionResults = executionState.intentionResults,
            deviceSignals = executionState.deviceSignals,
            testListener = testListener
        )
    }
}
