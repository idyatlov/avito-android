package com.avito.runner.scheduler.report

import com.avito.runner.model.TestCaseRun
import com.avito.runner.scheduler.report.model.SummaryReport
import com.avito.runner.scheduler.report.model.TestCaseRequestMatchingReport
import com.avito.runner.scheduler.runner.model.TestRunRequest
import com.avito.runner.scheduler.runner.model.TestRunResult
import com.avito.runner.scheduler.runner.model.createStubInstance
import com.avito.runner.service.model.DeviceTestCaseRun
import com.avito.runner.service.model.createStubInstance
import com.avito.test.model.TestCase
import com.avito.truth.isInstanceOf
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
internal class ReportCreatorTest {

    @Test
    fun `create summary - creates test report for each test case`() {
        val summary = createSummary(
            runs = listOf(
                TestRunRequest.Companion.createStubInstance(
                    testCase = TestCase.createStubInstance(methodName = "test1")
                ) to createTestSuiteRuns(),
                TestRunRequest.Companion.createStubInstance(
                    testCase = TestCase.createStubInstance(methodName = "test2")
                ) to createTestSuiteRuns(),
                TestRunRequest.Companion.createStubInstance(
                    testCase = TestCase.createStubInstance(methodName = "test3")
                ) to createTestSuiteRuns()
            )
        )

        assertThat(summary.reports).hasSize(3)
    }

    @Test
    fun `runs count - is empty`() {
        val summary = createSummary(
            runs = listOf(
                TestRunRequest.Companion.createStubInstance() to createTestSuiteRuns(),
                TestRunRequest.Companion.createStubInstance() to createTestSuiteRuns(),
                TestRunRequest.Companion.createStubInstance() to createTestSuiteRuns()
            )
        )

        assertThat(summary.successRunsCount).isEqualTo(0)
        assertThat(summary.failedRunsCount).isEqualTo(0)
        assertThat(summary.ignoredRunsCount).isEqualTo(0)
    }

    @Test
    fun `create summary - marks test as matched - all tests passed`() {
        val summary = createSummary(
            runs = listOf(
                TestRunRequest.Companion.createStubInstance() to createTestSuiteRuns(
                    TestCaseRun.Result.Passed.Regular,
                    TestCaseRun.Result.Passed.Regular,
                    TestCaseRun.Result.Passed.Regular
                )
            )
        )

        assertThat(
            summary.reports.first().result
        ).isInstanceOf<TestCaseRequestMatchingReport.Result.Matched>()
    }

    @Test
    fun `create summary - marks test as mismatched - all tests failed`() {
        val summary = createSummary(
            runs = listOf(
                TestRunRequest.Companion.createStubInstance() to createTestSuiteRuns(
                    TestCaseRun.Result.Failed.InRun(""),
                    TestCaseRun.Result.Failed.InRun(""),
                    TestCaseRun.Result.Failed.InRun("")
                )
            )
        )

        assertThat(
            summary.reports.first().result
        ).isInstanceOf<TestCaseRequestMatchingReport.Result.Mismatched>()
    }

    @Test
    fun `failed count - is not 0`() {
        val summary = createSummary(
            runs = listOf(
                TestRunRequest.Companion.createStubInstance() to createTestSuiteRuns(
                    TestCaseRun.Result.Failed.InRun(""),
                    TestCaseRun.Result.Failed.InRun(""),
                    TestCaseRun.Result.Failed.InRun("")
                )
            )
        )

        assertThat(summary.successRunsCount).isEqualTo(0)
        assertThat(summary.failedRunsCount).isEqualTo(3)
        assertThat(summary.ignoredRunsCount).isEqualTo(0)
    }

    @Test
    fun `create summary - marks test as mismatched - minimum success test count not reached`() {
        val runs = listOf(
            TestRunRequest.Companion.createStubInstance(
                scheduling = TestRunRequest.Scheduling(
                    retryCount = 2,
                    minimumSuccessCount = 2,
                    minimumFailedCount = 0
                )
            ) to createTestSuiteRuns(
                TestCaseRun.Result.Passed.Regular,
                TestCaseRun.Result.Failed.InRun(""),
                TestCaseRun.Result.Failed.InRun("")
            )
        )

        val summary = createSummary(runs = runs)

        assertThat(
            summary.reports.first().result
        ).isInstanceOf<TestCaseRequestMatchingReport.Result.Mismatched>()
    }

    @Test
    fun `failed and passed count - is not 0`() {
        val runs = listOf(
            TestRunRequest.Companion.createStubInstance(
                scheduling = TestRunRequest.Scheduling(
                    retryCount = 2,
                    minimumSuccessCount = 2,
                    minimumFailedCount = 0
                )
            ) to createTestSuiteRuns(
                TestCaseRun.Result.Passed.Regular,
                TestCaseRun.Result.Failed.InRun(""),
                TestCaseRun.Result.Failed.InRun("")
            )
        )

        val summary = createSummary(runs = runs)

        assertThat(summary.successRunsCount).isEqualTo(1)
        assertThat(summary.failedRunsCount).isEqualTo(2)
        assertThat(summary.ignoredRunsCount).isEqualTo(0)
    }

    @Test
    fun `create summary - marks test as matched - minimum success test count reached`() {
        val runs = listOf(
            TestRunRequest.Companion.createStubInstance(
                scheduling = TestRunRequest.Scheduling(
                    retryCount = 2,
                    minimumSuccessCount = 2,
                    minimumFailedCount = 0
                )
            ) to createTestSuiteRuns(
                TestCaseRun.Result.Passed.Regular,
                TestCaseRun.Result.Passed.Regular,
                TestCaseRun.Result.Failed.InRun("")
            )
        )

        val summary = createSummary(runs = runs)

        assertThat(
            summary.reports.first().result
        ).isInstanceOf<TestCaseRequestMatchingReport.Result.Matched>()
    }

    @Test
    fun `create summary - marks test as ignored - run contains ignored test`() {
        val runs = listOf(
            TestRunRequest.Companion.createStubInstance(
                scheduling = TestRunRequest.Scheduling(
                    retryCount = 2,
                    minimumSuccessCount = 2,
                    minimumFailedCount = 0
                )
            ) to createTestSuiteRuns(
                TestCaseRun.Result.Ignored
            )
        )

        val summary = createSummary(runs = runs)

        assertThat(
            summary.reports.first().result
        ).isInstanceOf<TestCaseRequestMatchingReport.Result.Ignored>()
    }

    private fun createTestSuiteRuns(
        vararg results: TestCaseRun.Result
    ): List<DeviceTestCaseRun> = results.map {
        DeviceTestCaseRun.createStubInstance(
            testCaseRun = TestCaseRun.createStubInstance(
                result = it
            )
        )
    }

    private fun createSummary(
        runs: List<Pair<TestRunRequest, List<DeviceTestCaseRun>>> = listOf(),
        startTimeMilliseconds: Long = 0
    ): SummaryReport {
        val reportCreator = SummaryReportMakerImpl()
        return reportCreator.make(
            results = runs.map { TestRunResult(it.first, it.second) },
            startTimeMilliseconds = startTimeMilliseconds
        )
    }
}
