package com.avito.instrumentation.configuration

import com.avito.instrumentation.configuration.target.TargetConfiguration
import com.avito.runner.scheduler.suite.filter.ImpactAnalysisMode
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import java.time.Duration
import javax.inject.Inject

public abstract class InstrumentationConfiguration @Inject constructor(
    public val name: String,
    objects: ObjectFactory
) {

    internal abstract val targetsContainer: NamedDomainObjectContainer<TargetConfiguration>

    public abstract val jobSlug: Property<String>

    public var instrumentationParams: Map<String, String> = emptyMap()

    public var reportSkippedTests: Boolean = false

    @Deprecated("Delete after 2024.27")
    public var runOnlyChangedTests: Boolean = false

    public val impactAnalysisMode: Property<ImpactAnalysisMode> =
        objects.property<ImpactAnalysisMode>().convention(ImpactAnalysisMode.ALL)

    @Deprecated("Setup instrumentation.environment")
    public var kubernetesNamespace: String = "default"

    @Deprecated("Use testRunnerExecutionTimeout and instrumentationTaskTimeout properties instead")
    public var timeoutInSeconds: Long = 120L // TODO: remove after MBS-11465

    public var testRunnerExecutionTimeout: Duration = Duration.ofMinutes(100)

    public var instrumentationTaskTimeout: Duration = Duration.ofMinutes(120)

    public var singleTestRunTimeout: Duration = Duration.ofMinutes(5)

    public var enableDeviceDebug: Boolean = false

    public var filter: String = "default"

    /**
     * failures of tests with @Flaky annotation will not impact run verdict
     *
     * https://avito-tech.github.io/avito-android/test/FlakyAnnotation/
     */
    public val suppressFlaky: Property<Boolean> = objects.property<Boolean>().convention(false)

    /**
     * any test failures will not impact run verdict
     */
    public val suppressFailure: Property<Boolean> = objects.property<Boolean>().convention(false)

    public fun targets(action: Action<NamedDomainObjectContainer<TargetConfiguration>>) {
        action.execute(targetsContainer)
    }
}
