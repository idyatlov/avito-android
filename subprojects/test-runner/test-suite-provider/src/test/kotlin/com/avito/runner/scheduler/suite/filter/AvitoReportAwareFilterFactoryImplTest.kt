package com.avito.runner.scheduler.suite.filter

import com.avito.android.Result
import com.avito.runner.scheduler.suite.config.InstrumentationFilterData
import com.avito.runner.scheduler.suite.config.RunStatus
import com.avito.runner.scheduler.suite.config.createStub
import com.avito.test.model.DeviceName
import com.avito.test.model.TestCase
import com.avito.test.model.TestName
import com.google.common.truth.Truth
import org.junit.jupiter.api.Test

/**
 * Tests that are depends on Avito Report
 * Should be extracted from FilterFactoryImpl
 */
internal class AvitoReportAwareFilterFactoryImplTest {

    @Test
    fun `when filterData report is present and has excludes then filters contain Report exclude filter`() {
        val runResultsProvider = StubRunResultsProvider()
        val reportId = "report#1"

        runResultsProvider.reportIdToRunResults = Result.Success(
            mapOf(
                reportId to mapOf(
                    TestCase(TestName("Test", "test1"), DeviceName("25")) to RunStatus.Success,
                    TestCase(TestName("Test", "test2"), DeviceName("25")) to RunStatus.Lost,
                )
            )
        )

        val factory = StubFilterFactoryFactory.create(
            runResultsProvider = runResultsProvider,
            filter = InstrumentationFilterData.createStub(
                report = InstrumentationFilterData.FromRunHistory.ReportFilter(
                    reportId = reportId,
                    statuses = Filter.Value(
                        included = emptySet(),
                        excluded = setOf(RunStatus.Success)
                    )
                )
            )
        )

        val filter = factory.createFilter() as CompositionFilter

        Truth.assertThat(filter.filters)
            .containsAtLeastElementsIn(
                listOf(
                    ExcludeByTestSignaturesFilter(
                        source = TestsFilter.Signatures.Source.Report,
                        signatures = setOf(
                            TestsFilter.Signatures.TestSignature(
                                name = "Test.test1",
                                deviceName = "25"
                            )
                        )
                    )
                )
            )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when filterData excludePrevious statuses and Report return list then filters contain ExcludeTestSignaturesFilters#Previous with included statuses`() {
        val runResultsProvider = StubRunResultsProvider()

        runResultsProvider.previousRunResults = Result.Success(
            mapOf(
                TestCase(TestName("Test", "test1"), DeviceName("25")) to RunStatus.Success,
                TestCase(TestName("Test", "test2"), DeviceName("25")) to RunStatus.Lost,
            )
        )

        val factory = StubFilterFactoryFactory.create(
            runResultsProvider = runResultsProvider,
            filter = InstrumentationFilterData.createStub(
                previousStatuses = Filter.Value(
                    included = emptySet(),
                    excluded = setOf(RunStatus.Success)
                )
            )
        )

        val filter = factory.createFilter() as CompositionFilter

        val that = Truth.assertThat(filter.filters)
        that.containsAtLeastElementsIn(
            listOf(
                ExcludeByTestSignaturesFilter(
                    source = TestsFilter.Signatures.Source.PreviousRun,
                    signatures = setOf(
                        TestsFilter.Signatures.TestSignature(
                            name = "Test.test1",
                            deviceName = "25"
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `when filterData report is present and has includes then filters contain Report include filter`() {
        val runResultsProvider = StubRunResultsProvider()
        val reportId = "report#1"

        runResultsProvider.reportIdToRunResults = Result.Success(
            mapOf(
                reportId to mapOf(
                    TestCase(TestName("Test", "test1"), DeviceName("25")) to RunStatus.Success,
                    TestCase(TestName("Test", "test2"), DeviceName("25")) to RunStatus.Lost,
                )
            )
        )

        val factory = StubFilterFactoryFactory.create(
            runResultsProvider = runResultsProvider,
            filter = InstrumentationFilterData.createStub(
                report = InstrumentationFilterData.FromRunHistory.ReportFilter(
                    reportId = reportId,
                    statuses = Filter.Value(
                        included = setOf(RunStatus.Success),
                        excluded = emptySet()
                    )
                )
            )
        )

        val filter = factory.createFilter() as CompositionFilter

        Truth.assertThat(filter.filters)
            .containsAtLeastElementsIn(
                listOf(
                    IncludeByTestSignaturesFilter(
                        source = TestsFilter.Signatures.Source.Report,
                        signatures = setOf(
                            TestsFilter.Signatures.TestSignature(
                                name = "Test.test1",
                                deviceName = "25"
                            )
                        )
                    )
                )
            )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when filterData includePrevious statuses and Report return list then filters contain IncludeTestSignaturesFilters#Previous with included statuses`() {
        val runResultsProvider = StubRunResultsProvider()

        runResultsProvider.previousRunResults = Result.Success(
            mapOf(
                TestCase(TestName("Test", "test1"), DeviceName("25")) to RunStatus.Success,
                TestCase(TestName("Test", "test2"), DeviceName("25")) to RunStatus.Lost
            )
        )

        val factory = StubFilterFactoryFactory.create(
            runResultsProvider = runResultsProvider,
            filter = InstrumentationFilterData.createStub(
                previousStatuses = Filter.Value(
                    included = setOf(RunStatus.Success),
                    excluded = emptySet()
                )
            )
        )

        val filter = factory.createFilter() as CompositionFilter

        val that = Truth.assertThat(filter.filters)
        that.containsAtLeastElementsIn(
            listOf(
                IncludeByTestSignaturesFilter(
                    source = TestsFilter.Signatures.Source.PreviousRun,
                    signatures = setOf(
                        TestsFilter.Signatures.TestSignature(
                            name = "Test.test1",
                            deviceName = "25"
                        )
                    )
                )
            )
        )
    }
}
