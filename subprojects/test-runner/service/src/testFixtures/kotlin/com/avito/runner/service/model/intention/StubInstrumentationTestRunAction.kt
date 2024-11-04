package com.avito.runner.service.model.intention

import com.avito.runner.service.model.createStubInstance
import com.avito.test.model.TestCase

internal fun InstrumentationTestRunAction.Companion.createStubInstance(
    testCase: TestCase = TestCase.createStubInstance(),
    testPackage: String = "",
    targetPackage: String = "",
    testArtifactsDirectoryPackage: String = targetPackage,
    testRunner: String = "",
    instrumentationParams: Map<String, String> = emptyMap(),
    executionNumber: Int = 0,
    timeoutMinutes: Long = 0,
    enableDeviceDebug: Boolean = false
): InstrumentationTestRunAction = InstrumentationTestRunAction(
    test = testCase,
    testPackage = testPackage,
    targetPackage = targetPackage,
    testArtifactsDirectoryPackage = testArtifactsDirectoryPackage,
    testRunner = testRunner,
    instrumentationParams = instrumentationParams,
    executionNumber = executionNumber,
    timeoutMinutes = timeoutMinutes,
    enableDeviceDebug = enableDeviceDebug
)
