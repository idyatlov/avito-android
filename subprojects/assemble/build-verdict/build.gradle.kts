plugins {
    id("convention.kotlin-jvm")
    id("convention.publish-gradle-plugin")
    id("convention.gradle-testing")
}

dependencies {
    api(project(":subprojects:assemble:build-verdict-tasks-api"))

    implementation(gradleApi())
    implementation(project(":subprojects:common:throwable-utils"))
    implementation(project(":subprojects:gradle:gradle-extensions"))
    implementation(libs.gson)
    implementation(libs.kotlinHtml)

    gradleTestImplementation(project(":subprojects:gradle:test-project"))
}

gradlePlugin {
    plugins {
        create("buildVerdict") {
            id = "com.avito.android.build-verdict"
            implementationClass = "com.avito.android.build_verdict.BuildVerdictPlugin"
            displayName = "Create file with a build verdict"
        }
    }
}
