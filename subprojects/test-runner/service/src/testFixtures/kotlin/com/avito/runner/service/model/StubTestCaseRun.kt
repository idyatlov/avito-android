package com.avito.runner.service.model

import com.avito.runner.model.TestCaseRun
import com.avito.test.model.TestCase

public fun TestCaseRun.Companion.createStubInstance(
    testCase: TestCase = TestCase.createStubInstance(),
    result: TestCaseRun.Result = TestCaseRun.Result.Passed.Regular,
    timestampStartedMilliseconds: Long = 1000,
    timestampCompletedMilliseconds: Long = 2000
): TestCaseRun = TestCaseRun(
    test = testCase,
    result = result,
    timestampStartedMilliseconds = timestampStartedMilliseconds,
    timestampCompletedMilliseconds = timestampCompletedMilliseconds
)
