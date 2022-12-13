package com.avito.android.tech_budget

import com.avito.android.tech_budget.ab_tests.CollectABTestsConfiguration
import com.avito.android.tech_budget.deeplinks.CollectDeeplinksConfiguration
import com.avito.android.tech_budget.feature_toggles.CollectFeatureTogglesConfiguration
import org.gradle.api.Action
import org.gradle.api.tasks.Nested

public abstract class TechBudgetExtension {

    @get:Nested
    internal abstract val warnings: CollectWarningsConfiguration

    @get:Nested
    internal abstract val dumpInfo: DumpInfoConfiguration

    @get:Nested
    internal abstract val deepLinks: CollectDeeplinksConfiguration

    @get:Nested
    internal abstract val abTests: CollectABTestsConfiguration

    @get:Nested
    internal abstract val featureToggles: CollectFeatureTogglesConfiguration

    public fun collectWarnings(action: Action<CollectWarningsConfiguration>) {
        action.execute(warnings)
    }

    public fun collectDeepLinks(action: Action<CollectDeeplinksConfiguration>) {
        action.execute(deepLinks)
    }

    public fun collectABTests(action: Action<CollectABTestsConfiguration>) {
        action.execute(abTests)
    }

    public fun collectFeatureToggles(action: Action<CollectFeatureTogglesConfiguration>) {
        action.execute(featureToggles)
    }

    public fun dumpInfo(action: Action<DumpInfoConfiguration>) {
        action.execute(dumpInfo)
    }
}
