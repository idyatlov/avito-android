package com.avito.runner.scheduler.suite.filter

internal val includeAll = object : TestsFilter {
    override val name: String = "StubIncludeAll"

    override fun filter(test: TestsFilter.Test): TestsFilter.Result {
        return TestsFilter.Result.Included
    }
}

public fun excludedFilter(reason: TestsFilter.Result.Excluded): TestsFilter {
    return object : TestsFilter {
        override val name: String = "StubExclude"

        override fun filter(test: TestsFilter.Test): TestsFilter.Result {
            return reason
        }
    }
}

public class StubFilterFactory(
    private val filter: TestsFilter = includeAll
) : FilterFactory {

    override fun createFilter(): TestsFilter {
        return filter
    }
}
