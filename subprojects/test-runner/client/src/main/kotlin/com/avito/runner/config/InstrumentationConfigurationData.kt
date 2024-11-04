package com.avito.runner.config

import com.avito.android.runner.devices.model.DeviceType
import com.avito.instrumentation.reservation.request.Device
import com.avito.runner.model.InstrumentationParameters
import com.avito.runner.scheduler.suite.config.InstrumentationFilterData
import java.io.Serializable
import java.time.Duration

public data class InstrumentationConfigurationData(
    val name: String,
    val instrumentationParams: InstrumentationParameters,
    val reportSkippedTests: Boolean,
    val targets: List<TargetConfigurationData>,
    val testRunnerExecutionTimeout: Duration,
    val instrumentationTaskTimeout: Duration,
    val singleTestRunTimeout: Duration,
    val filter: InstrumentationFilterData,
    val reportConfig: RunnerReportConfig,
) : Serializable {

    val requestedDeviceType: DeviceType = determineRequestedDeviceType(targets.map { it.reservation.device })

    override fun toString(): String = "$name, targets: $targets, filter: $filter "

    private fun determineRequestedDeviceType(requestedDevices: List<Device>): DeviceType {
        return when {
            requestedDevices.all { it is Device.LocalEmulator } -> DeviceType.LOCAL
            requestedDevices.all { it is Device.CloudEmulator } -> DeviceType.CLOUD
            requestedDevices.all { it is Device.MockEmulator } -> DeviceType.MOCK
            else -> {
                val deviceTypesNames = DeviceType.entries.map { it.name }
                throw IllegalStateException(
                    "Targeting different type of emulators($deviceTypesNames) " +
                        "in the same configuration is not supported; " +
                        "Affected configuration: $name"
                )
            }
        }
    }

    public companion object
}
