package com.avito.runner.scheduler.runner.model

import com.avito.runner.config.TargetConfigurationData
import com.avito.runner.service.worker.device.model.DeviceConfiguration
import com.avito.test.model.DeviceName
import com.avito.test.model.TestCase
import java.io.File
import java.time.Duration

internal class TestRunRequestFactory(
    private val application: File,
    private val testApplication: File,
    private val deviceDebug: Boolean,
    private val executionParameters: ExecutionParameters,
    private val targets: Map<DeviceName, TargetConfigurationData>,
    private val testRunTimeout: Duration,
) {

    fun create(test: TestCase): TestRunRequest {
        val target = requireNotNull(targets[test.deviceName]) {
            "Can't find target ${test.deviceName}"
        }
        val reservation = target.reservation
        val quota = target.reservation.quota
        return TestRunRequest(
            testCase = TestCase(
                name = test.name,
                deviceName = test.deviceName
            ),
            configuration = DeviceConfiguration(
                api = reservation.device.api,
                model = reservation.device.model
            ),
            scheduling = TestRunRequest.Scheduling(
                retryCount = quota.retryCount,
                minimumFailedCount = quota.minimumFailedCount,
                minimumSuccessCount = quota.minimumSuccessCount
            ),
            application = application.absolutePath,
            applicationPackage = executionParameters.applicationPackageName,
            testApplication = testApplication.absolutePath,
            testPackage = executionParameters.applicationTestPackageName,
            testArtifactsDirectoryPackage = executionParameters.testArtifactsDirectoryPackageName,
            testRunner = executionParameters.testRunner,
            timeoutMinutes = testRunTimeout.toMinutes(),
            instrumentationParameters = target.instrumentationParams,
            enableDeviceDebug = deviceDebug
        )
    }
}
