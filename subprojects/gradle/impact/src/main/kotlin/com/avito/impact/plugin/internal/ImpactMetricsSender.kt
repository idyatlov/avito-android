package com.avito.impact.plugin.internal

import com.avito.android.isAndroidApp
import com.avito.android.sentry.EnvironmentInfo
import com.avito.android.stats.GaugeLongMetric
import com.avito.android.stats.StatsDSender
import com.avito.graphite.series.SeriesName
import com.avito.impact.ModifiedProject
import com.avito.impact.ModifiedProjectsFinder
import com.avito.math.percentOf
import com.avito.module.configurations.ConfigurationType
import com.avito.utils.gradle.Environment
import org.gradle.api.Project

internal class ImpactMetricsSender(
    private val projectsFinder: ModifiedProjectsFinder,
    environmentInfo: EnvironmentInfo,
    private val metricTracker: StatsDSender
) {

    init {
        require(environmentInfo.environment is Environment.CI) {
            "ImpactMetricsSender should run only in CI environment"
        }
    }

    private val ConfigurationType.metricName: String
        get() {
            return when (this) {
                ConfigurationType.AndroidTests -> "androidtests"
                ConfigurationType.Main -> "implementation"
                ConfigurationType.Lint -> "lint"
                ConfigurationType.UnitTests -> "unittests"
                ConfigurationType.Detekt -> "detekt"
                ConfigurationType.CodeGenerators -> "codegenerators"
            }
        }

    fun sendMetrics() {
        val allProjects = projectsFinder.allProjects()

        ConfigurationType.values().forEach { type ->
            val modified = projectsFinder.modifiedProjects(type)

            sendModulesMetrics(type, allProjects, modified)
            sendAppsMetrics(type, allProjects, modified)
        }
    }

    private fun sendModulesMetrics(
        configurationType: ConfigurationType,
        projects: Set<Project>,
        modified: Set<ModifiedProject>
    ) {
        val metric = GaugeLongMetric(
            name = SeriesName.create(
                "build", "impact", "modules", configurationType.metricName.lowercase(), "modified"
            ),
            gauge = modified.size.percentOf(projects.size).roundToLong()
        )
        metricTracker.send(metric)
    }

    private fun sendAppsMetrics(
        configurationType: ConfigurationType,
        projects: Set<Project>,
        modified: Set<ModifiedProject>
    ) {
        val apps = projects.count { it.isAndroidApp() }
        val modifiedApps = modified.count { it.project.isAndroidApp() }

        val metric = GaugeLongMetric(
            name = SeriesName.create("build", "impact", "apps", configurationType.metricName.lowercase(), "modified"),
            gauge = modifiedApps.percentOf(apps).roundToLong()
        )
        metricTracker.send(metric)
    }
}
