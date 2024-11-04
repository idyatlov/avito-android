package com.avito.runner.scheduler.suite.filter

import com.avito.report.model.TestStaticData
import com.avito.runner.scheduler.suite.config.InstrumentationFilterData
import com.avito.runner.scheduler.suite.filter.TestsFilter.Result.Excluded
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

public interface FilterInfoWriter {
    public fun writeFilterConfig(config: InstrumentationFilterData)
    public fun writeAppliedFilter(filter: TestsFilter)
    public fun writeFilterExcludes(excludes: List<Pair<TestStaticData, Excluded>>)

    public class Impl(
        outputDir: File,
    ) : FilterInfoWriter {

        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

        private val filterDir: File = File(outputDir, "filter").apply { mkdir() }
        private val filterConfig: File = File(filterDir, "filter-config.json")
        private val filterApplied = File(filterDir, "filters-applied.json")
        private val filterExcludesFile = File(filterDir, "filters-excludes.json")

        override fun writeFilterConfig(config: InstrumentationFilterData) {
            filterConfig.writeText(gson.toJson(config))
        }

        override fun writeAppliedFilter(filter: TestsFilter) {
            filterApplied.writeText(gson.toJson(filter))
        }

        override fun writeFilterExcludes(excludes: List<Pair<TestStaticData, Excluded>>) {
            filterExcludesFile.writeText(
                gson.toJson(
                    excludes.groupBy(
                        keySelector = { (_, excludeReason) ->
                            excludeReason.byFilter
                        },
                        valueTransform = { (test, _) ->
                            mapOf(
                                "testName" to test.name,
                                "device" to test.device.name
                            )
                        }
                    )
                )
            )
        }
    }
}
