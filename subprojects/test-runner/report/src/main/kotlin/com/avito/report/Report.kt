package com.avito.report

import com.avito.android.Result
import com.avito.report.model.AndroidTest
import com.avito.report.model.TestAttempt
import com.avito.report.model.TestStaticData
import com.avito.report.model.TestStatus
import com.avito.test.model.TestCase

public interface Report {

    public val reportLinksGenerator: ReportLinksGenerator

    public val testSuiteNameProvider: TestSuiteNameProvider

    public fun addTest(testAttempt: TestAttempt)

    public fun addTest(tests: Collection<AndroidTest>)

    /**
     * Skipped tests available right after initial filtering, so it's added even before test runner started
     */
    public fun addSkippedTests(skippedTests: List<Pair<TestStaticData, String>>)

    /**
     * Optionally report about tests, lost during run
     */
    public fun reportLostTests(notReportedTests: Collection<AndroidTest.Lost>)

    /**
     * single result for each test, where attempts aggregated by
     * [com.avito.report.inmemory.TestAttemptsAggregateStrategy]
     */
    public fun getTestResults(): Collection<AndroidTest>

    public fun getPreviousRunsResults(): Result<Map<TestCase, TestStatus>>

    public fun getRunResultsById(id: String): Result<Map<TestCase, TestStatus>>

    public companion object
}
