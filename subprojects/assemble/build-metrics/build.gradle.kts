plugins {
    id("convention.kotlin-jvm")
    id("convention.publish-gradle-plugin")
    id("convention.gradle-testing")
}

dependencies {
    implementation(projects.subprojects.common.math)
    implementation(projects.subprojects.common.result)
    implementation(projects.subprojects.common.techBudgetCommon)
    implementation(projects.subprojects.logger.gradleLogger)
    implementation(projects.subprojects.gradle.buildEnvironment)
    implementation(projects.subprojects.assemble.gradleProfile)
    implementation(projects.subprojects.assemble.criticalPath.api)
    implementation(projects.subprojects.gradle.android)
    implementation(projects.subprojects.gradle.graphiteConfig)
    implementation(projects.subprojects.gradle.gradleExtensions)
    implementation(projects.subprojects.gradle.process)
    implementation(projects.subprojects.gradle.statsdConfig)
    implementation(projects.subprojects.gradle.teamcity)
    implementation(projects.subprojects.gradle.workerExtensions)
    implementation(libs.kotlinGradle)
    implementation(libs.moshi)

    testImplementation(libs.mockitoKotlin)
    testImplementation(libs.mockitoJUnitJupiter)
    testImplementation(testFixtures(projects.subprojects.gradle.process))
    testImplementation(testFixtures(projects.subprojects.gradle.buildEnvironment))

    gradleTestImplementation(projects.subprojects.common.junitUtils)
    gradleTestImplementation(projects.subprojects.common.testOkhttp)
    gradleTestImplementation(projects.subprojects.gradle.testProject)
    gradleTestImplementation(projects.subprojects.gradle.git)
}

gradlePlugin {
    plugins {
        create("buildMetrics") {
            id = "com.avito.android.build-metrics"
            implementationClass = "com.avito.android.plugin.build_metrics.BuildMetricsPlugin"
            displayName = "Build metrics"
        }
    }
}
