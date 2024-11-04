package com.avito.runner.service.worker.device.adb

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.DdmPreferences
import com.android.ddmlib.IDevice
import com.avito.android.Result
import com.avito.android.asRuntimeException
import com.avito.cli.Notification
import com.avito.cli.RxCommandLine
import com.avito.logger.Logger
import com.avito.runner.model.TestCaseRun
import com.avito.runner.model.TestCaseRun.Result.Failed
import com.avito.runner.service.model.DeviceTestCaseRun
import com.avito.runner.service.model.intention.InstrumentationTestRunAction
import com.avito.runner.service.worker.device.Device
import com.avito.runner.service.worker.device.DeviceCoordinate
import com.avito.runner.service.worker.device.adb.instrumentation.InstrumentationTestCaseRunParser
import com.avito.runner.service.worker.device.adb.listener.AdbDeviceEventsListener
import com.avito.runner.service.worker.device.adb.request.AdbRequest
import com.avito.runner.service.worker.device.adb.request.adb.LogcatAdbRequest
import com.avito.runner.service.worker.device.adb.request.adb.PullAdbRequest
import com.avito.runner.service.worker.device.adb.request.shell.ClearDirectoryAdbShellRequest
import com.avito.runner.service.worker.device.adb.request.shell.ClearPackageAdbShellRequest
import com.avito.runner.service.worker.device.adb.request.shell.GetPropAdbShellRequest
import com.avito.runner.service.worker.device.adb.request.shell.ListDirectoryAdbShellRequest
import com.avito.runner.service.worker.device.adb.request.shell.RunTestsAdbShellRequest
import com.avito.runner.service.worker.device.model.getData
import com.avito.runner.service.worker.model.DeviceInstallation
import com.avito.runner.service.worker.model.Installation
import com.avito.runner.service.worker.model.InstrumentationTestCaseRun
import com.avito.test.model.TestCase
import com.avito.time.TimeProvider
import rx.Observable
import rx.Single
import java.io.File
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.io.path.div

public data class AdbDevice(
    override val coordinate: DeviceCoordinate,
    override val model: String,
    override val online: Boolean,
    override val api: Int,
    private val adb: Adb,
    private val timeProvider: TimeProvider,
    // MBS-8531: don't use "ADB" here to avoid possible recursion
    override val logger: Logger,
    private val eventsListener: AdbDeviceEventsListener,
    private val adbPullTimeout: Duration
) : Device {

    private val instrumentationParser: InstrumentationTestCaseRunParser = InstrumentationTestCaseRunParser.Impl()

    private val retryAction: RetryAction = RetryAction(timeProvider)

    override fun installApplication(applicationPackage: String): Result<DeviceInstallation> {
        var installStartedTimestamp: Long
        return getAdbDevice().flatMap { adbDevice ->

            installStartedTimestamp = timeProvider.nowInMillis()

            retryAction.retry(
                retriesCount = 10,
                delaySeconds = 5,
                action = {
                    adbDevice.installPackage(applicationPackage, true)
                },
                onError = { attempt: Int, throwable: Throwable, durationMs: Long ->
                    eventsListener.onInstallApplicationError(
                        device = this,
                        attempt = attempt,
                        applicationPackage = applicationPackage,
                        throwable = throwable,
                        durationMs = durationMs
                    )
                },
                onFailure = { throwable: Throwable, durationMs: Long ->
                    eventsListener.onInstallApplicationFailure(
                        device = this,
                        applicationPackage = applicationPackage,
                        throwable = throwable,
                        durationMs = durationMs
                    )
                },
                onSuccess = { attempt: Int, _: Unit, durationMs: Long ->
                    eventsListener.onInstallApplicationSuccess(
                        device = this,
                        attempt = attempt,
                        applicationPackage = applicationPackage,
                        durationMs = durationMs
                    )
                }
            )
                .map {
                    DeviceInstallation(
                        installation = Installation(
                            application = applicationPackage,
                            timestampStartedMilliseconds = installStartedTimestamp,
                            timestampCompletedMilliseconds = timeProvider.nowInMillis()
                        ),
                        device = this.getData()
                    )
                }
        }
    }

    override suspend fun runIsolatedTest(
        action: InstrumentationTestRunAction,
        outputDir: File
    ): DeviceTestCaseRun {

        val finalInstrumentationArguments = action.instrumentationParams.plus(
            "class" to "${action.test.name.className}#${action.test.name.methodName}"
        )

        val startTime = timeProvider.nowInMillis()

        return runTest(
            test = action.test,
            testPackageName = action.testPackage,
            testRunnerClass = action.testRunner,
            instrumentationArguments = finalInstrumentationArguments,
            outputDir = outputDir,
            timeoutMinutes = action.timeoutMinutes,
            enableDeviceDebug = action.enableDeviceDebug
        )
            .map {
                when (it) {
                    is InstrumentationTestCaseRun.CompletedTestCaseRun -> {
                        when (it.result) {
                            TestCaseRun.Result.Passed.Regular,
                            is TestCaseRun.Result.Passed.WithMacrobenchmarkOutputs ->
                                eventsListener.onRunTestPassed(
                                    device = this,
                                    testName = it.name.toString(),
                                    durationMs = timeProvider.nowInMillis() - startTime
                                )
                            TestCaseRun.Result.Ignored ->
                                eventsListener.onRunTestIgnored(
                                    device = this,
                                    testName = it.name.toString(),
                                    durationMs = timeProvider.nowInMillis() - startTime
                                )
                            is Failed.InRun ->
                                eventsListener.onRunTestRunError(
                                    device = this,
                                    testName = it.name.toString(),
                                    errorMessage = it.result.errorMessage,
                                    durationMs = timeProvider.nowInMillis() - startTime
                                )
                            is Failed.InfrastructureError ->
                                eventsListener.onRunTestInfrastructureError(
                                    device = this,
                                    testName = it.name.toString(),
                                    errorMessage = it.result.error.message ?: "Empty error message",
                                    throwable = it.result.error,
                                    durationMs = timeProvider.nowInMillis() - startTime
                                )
                        }
                        DeviceTestCaseRun(
                            testCaseRun = TestCaseRun(
                                test = TestCase(
                                    name = it.name,
                                    deviceName = action.test.deviceName
                                ),
                                result = it.result,
                                timestampStartedMilliseconds = it.timestampStartedMilliseconds,
                                timestampCompletedMilliseconds = it.timestampCompletedMilliseconds
                            ),
                            device = this.getData()
                        )
                    }
                    is InstrumentationTestCaseRun.FailedOnStartTestCaseRun -> {
                        eventsListener.onRunTestFailedOnStart(
                            device = this,
                            message = it.message,
                            durationMs = timeProvider.nowInMillis() - startTime
                        )
                        DeviceTestCaseRun(
                            testCaseRun = TestCaseRun(
                                test = action.test,
                                result = Failed.InfrastructureError.FailedOnStart(
                                    error = RuntimeException("Failed on start test case: ${it.message}")
                                ),
                                timestampStartedMilliseconds = timeProvider.nowInMillis(),
                                timestampCompletedMilliseconds = timeProvider.nowInMillis()
                            ),
                            device = this.getData()
                        )
                    }
                    is InstrumentationTestCaseRun.FailedOnInstrumentationParsing -> {
                        eventsListener.onRunTestFailedOnInstrumentationParse(
                            device = this,
                            message = it.message,
                            throwable = it.throwable,
                            durationMs = timeProvider.nowInMillis()
                        )
                        DeviceTestCaseRun(
                            testCaseRun = TestCaseRun(
                                test = action.test,
                                result = Failed.InfrastructureError.FailedOnParsing(
                                    error = RuntimeException("Failed on instrumentation parsing", it.throwable),
                                ),
                                timestampStartedMilliseconds = timeProvider.nowInMillis(),
                                timestampCompletedMilliseconds = timeProvider.nowInMillis()
                            ),
                            device = this.getData()
                        )
                    }
                }
            }
            .toBlocking()
            .value()
    }

    override fun deviceStatus(): Device.DeviceStatus = retryAction.retry(
        retriesCount = 15,
        delaySeconds = 5,
        action = {
            val bootCompleted: Boolean = loadProperty(
                key = "sys.boot_completed",
                cast = { output -> output == "1" }
            )

            if (!bootCompleted) {
                throw IllegalStateException("sys.boot_completed isn't '1'")
            }

            bootCompleted
        },
        onError = { attempt: Int, _: Throwable, durationMs: Long ->
            eventsListener.onGetAliveDeviceError(this, attempt, durationMs)
        },
        onFailure = { throwable, durationMs ->
            eventsListener.onGetAliveDeviceFailed(this, throwable, durationMs)
        },
        onSuccess = { attempt: Int, _: Boolean, durationMs: Long ->
            eventsListener.onGetAliveDeviceSuccess(this, attempt, durationMs)
        }
    )
        .fold(
            { Device.DeviceStatus.Alive },
            { throwable: Throwable -> Device.DeviceStatus.Freeze(reason = throwable) }
        )

    override fun clearPackage(name: String): Result<Unit> = retryAction.retry(
        retriesCount = 10,
        delaySeconds = 1,
        action = {
            val result = executeBlockingAdbRequest(
                request = ClearPackageAdbShellRequest(name),
                // was seeing ~20% error rate at 5s
                timeoutSeconds = 20
            )

            if (!result.output.contains("success", ignoreCase = true)) {
                throw IllegalStateException("Fail to clear package $name; output=${result.output}")
            }
        },
        onError = { attempt: Int, throwable: Throwable, durationMs: Long ->
            eventsListener.onClearPackageError(
                device = this,
                attempt = attempt,
                name = name,
                throwable = throwable,
                durationMs = durationMs
            )
        },
        onFailure = { throwable: Throwable, durationMs: Long ->
            eventsListener.onClearPackageFailure(
                device = this,
                name = name,
                throwable = throwable,
                durationMs = durationMs
            )
        },
        onSuccess = { attempt: Int, _: Any, durationMs: Long ->
            eventsListener.onClearPackageSuccess(
                device = this,
                attempt = attempt,
                name = name,
                durationMs = durationMs
            )
        }
    )

    override fun pull(from: Path, to: Path): Result<File> = pullInternal(
        from = from,
        to = to,
        validator = AlwaysSuccessPullValidator
    )

    override fun pullDir(deviceDir: Path, hostDir: Path, validator: PullValidator): Result<File> {
        // last /. means to adb to copy recursively, and do not copy the last
        // example:
        //  - from: /sdcard/Android/someDir/ to: /xx ; will copy to /xx/someDir/ and not recursive
        //  - from: /sdcard/android/someDir/. to: /xx ; will copy to /xx and recursive
        return pullInternal(
            from = deviceDir / ".",
            to = hostDir,
            validator = validator
        )
    }

    override fun pullFile(deviceFile: Path, hostDir: Path, validator: PullValidator): Result<File> {
        return pullInternal(
            from = deviceFile,
            to = hostDir,
            validator = validator
        )
    }

    override fun clearDirectory(remotePath: Path): Result<Unit> = retryAction.retry(
        retriesCount = DEFAULT_RETRY_COUNT,
        delaySeconds = DEFAULT_DELAY_SEC,
        action = {
            executeBlockingAdbRequest(request = ClearDirectoryAdbShellRequest(remotePath))
        },
        onError = { attempt: Int, throwable: Throwable, durationMs: Long ->
            eventsListener.onClearDirectoryError(
                device = this,
                attempt = attempt,
                remotePath = remotePath,
                throwable = throwable,
                durationMs = durationMs
            )
        },
        onFailure = { throwable: Throwable, durationMs: Long ->
            eventsListener.onClearDirectoryFailure(
                device = this,
                remotePath = remotePath,
                throwable = throwable,
                durationMs = durationMs
            )
        },
        onSuccess = { _: Int, result: Notification.Exit, durationMs: Long ->
            eventsListener.onClearDirectorySuccess(
                device = this,
                remotePath = remotePath,
                output = result.output,
                durationMs = durationMs
            )
        }
    ).map { }

    override fun list(remotePath: Path): Result<List<String>> = retryAction.retry(
        retriesCount = DEFAULT_RETRY_COUNT,
        delaySeconds = DEFAULT_DELAY_SEC,
        action = {
            executeBlockingAdbRequest(
                request = ListDirectoryAdbShellRequest(remotePath)
            ).output.lines()
        },
        onError = { attempt: Int, throwable: Throwable, durationMs: Long ->
            eventsListener.onListError(
                device = this,
                attempt = attempt,
                remotePath = remotePath.toString(),
                throwable = throwable,
                durationMs = durationMs
            )
        },
        onFailure = { throwable: Throwable, durationMs: Long ->
            eventsListener.onListFailure(
                device = this,
                remotePath = remotePath.toString(),
                throwable = throwable,
                durationMs = durationMs
            )
        },
        onSuccess = { _: Int, _: List<String>, durationMs: Long ->
            eventsListener.onListSuccess(
                device = this,
                remotePath = remotePath.toString(),
                durationMs = durationMs
            )
        }
    )

    private fun pullInternal(
        from: Path,
        to: Path,
        validator: PullValidator
    ): Result<File> = retryAction.retry(
        retriesCount = DEFAULT_RETRY_COUNT,
        delaySeconds = DEFAULT_DELAY_SEC,
        action = {
            executeBlockingAdbRequest(
                request = PullAdbRequest(from, to),
                timeoutSeconds = adbPullTimeout.toSeconds(),
            )

            when (val pullResult = validator.isPulledCompletely(to)) {

                is PullValidator.Result.Ok -> to.toFile()

                is PullValidator.Result.Failure -> throw pullResult.problem.asRuntimeException()
            }
        },
        onError = { attempt: Int, throwable: Throwable, durationMs: Long ->
            eventsListener.onPullError(
                device = this,
                attempt = attempt,
                from = from,
                throwable = throwable,
                durationMs = durationMs
            )
        },
        onFailure = { throwable: Throwable, durationMs: Long ->
            eventsListener.onPullFailure(
                device = this,
                from = from,
                throwable = throwable,
                durationMs = durationMs
            )
        },
        onSuccess = { _: Int, _: Any, durationMs: Long ->
            eventsListener.onPullSuccess(
                device = this,
                from = from,
                to = to,
                durationMs = durationMs
            )
        }
    )

    override fun logcat(lines: Int?): Result<String> {
        return retryAction.retry(
            retriesCount = 3,
            delaySeconds = 1,
            action = {
                executeBlockingAdbRequest(
                    request = LogcatAdbRequest(lines),
                    timeoutSeconds = 10
                ).output
            },
            onSuccess = { _, _, durationMs ->
                eventsListener.onLogcatSuccess(this, durationMs)
            },
            onError = { _, throwable, durationMs ->
                eventsListener.onLogcatError(this, durationMs, throwable)
            },
            onFailure = { throwable, durationMs ->
                eventsListener.onLogcatFailure(this, durationMs, throwable)
            }
        )
    }

    private fun runTest(
        test: TestCase,
        testPackageName: String,
        testRunnerClass: String,
        instrumentationArguments: Map<String, String>,
        outputDir: File,
        timeoutMinutes: Long,
        enableDeviceDebug: Boolean
    ): Single<InstrumentationTestCaseRun> {
        val logsDir = File(File(outputDir, "logs"), coordinate.serial.value)
            .apply { mkdirs() }
        val started = timeProvider.nowInMillis()

        val blankValues = instrumentationArguments.filterValues { it.isBlank() }
        if (blankValues.isNotEmpty()) {
            return Single.error(
                IllegalArgumentException(
                    "Instrumentation args contains blank values: $blankValues" +
                        " it leads to adb args parsing problem. Filter it in configuration"
                )
            )
        }

        val instrumentationOutput = executeAdbRequest(
            request = RunTestsAdbShellRequest(
                testPackageName = testPackageName,
                testRunnerClass = testRunnerClass,
                instrumentationArguments = instrumentationArguments,
                enableDeviceDebug = enableDeviceDebug,
            ),
            redirectOutputTo = File(logsDir, "instrumentation-${test.name}.txt")
        )

        return instrumentationParser
            .parse(instrumentationOutput)
            .first() // example of multiple items is app process crashed after test completed
            .toSingle()
            .timeout(
                timeoutMinutes,
                TimeUnit.MINUTES,
                Single.just(
                    InstrumentationTestCaseRun.CompletedTestCaseRun(
                        name = test.name,
                        result = Failed.InfrastructureError.Timeout(
                            timeoutMin = timeoutMinutes,
                            error = RuntimeException("Failed on Timeout")
                        ),
                        timestampStartedMilliseconds = started,
                        timestampCompletedMilliseconds = started + TimeUnit.MINUTES.toMillis(timeoutMinutes)
                    )
                )
            )
    }

    private fun getAdbDevice(): Result<IDevice> = Result.tryCatch {
        AndroidDebugBridge.initIfNeeded(false)
        DdmPreferences.setTimeOut(Duration.ofSeconds(DDMLIB_SOCKET_TIME_OUT_SECONDS).toMillis().toInt())

        val bridge = AndroidDebugBridge.createBridge(adb.adbPath, false)
        waitForAdb(bridge)

        bridge.devices.find { it.serialNumber == coordinate.serial.value }
            ?: throw RuntimeException("Device $coordinate not found")
    }

    private fun waitForAdb(
        adb: AndroidDebugBridge,
        timeOut: Duration = Duration.ofMinutes(WAIT_FOR_ADB_TIME_OUT_MINUTES)
    ) {
        var timeOutMs = timeOut.toMillis()
        val sleepTimeMs = TimeUnit.SECONDS.toMillis(1)

        while (!adb.hasInitialDeviceList() && timeOutMs > 0) {
            try {
                Thread.sleep(sleepTimeMs)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }

            timeOutMs -= sleepTimeMs
        }

        if (timeOutMs <= 0 && !adb.hasInitialDeviceList()) {
            throw RuntimeException("Timeout getting device list.", null)
        }
    }

    private inline fun <reified T> loadProperty(
        key: String,
        crossinline cast: (result: String) -> T
    ): T {
        val commandResult = executeBlockingAdbRequest(request = GetPropAdbShellRequest(key))

        val output = commandResult.output.trim()

        return try {
            cast(output)
        } catch (e: Exception) {
            throw RuntimeException("Failed to cast property result with key: $key. Output: $output.")
        }
    }

    private fun executeBlockingAdbRequest(
        request: AdbRequest,
        timeoutSeconds: Long = DEFAULT_COMMAND_TIMEOUT_SECONDS
    ): Notification.Exit {
        return executeAdbRequest(request = request)
            .ofType(Notification.Exit::class.java)
            .timeout(
                timeoutSeconds,
                TimeUnit.SECONDS,
                Observable.error(
                    RuntimeException(
                        buildString {
                            append("Timeout: $timeoutSeconds seconds. ")
                            append("Failed to execute command: ${request.serialize(coordinate.serial.value)} ")
                            append("on device $coordinate")
                        }
                    )
                )
            )
            .toBlocking()
            .first()
    }

    private fun executeAdbRequest(
        request: AdbRequest,
        redirectOutputTo: File? = null,
    ): Observable<Notification> {
        return RxCommandLine(
            command = adb.adbPath,
            args = request.serialize(coordinate.serial.value)
        ).start(redirectOutputTo)
    }

    override fun toString(): String = "Device ${coordinate.serial}"
}

private const val DEFAULT_COMMAND_TIMEOUT_SECONDS = 5L
private const val DDMLIB_SOCKET_TIME_OUT_SECONDS = 20L
private const val WAIT_FOR_ADB_TIME_OUT_MINUTES = 1L
private const val DEFAULT_RETRY_COUNT = 5
private const val DEFAULT_DELAY_SEC = 3L
