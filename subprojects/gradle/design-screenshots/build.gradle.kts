plugins {
    id("convention.kotlin-jvm")
    id("convention.publish-gradle-plugin")
    id("convention.gradle-testing")
}

dependencies {
    implementation(project(":subprojects:gradle:gradle-extensions"))
    implementation(project(":subprojects:gradle:android"))
    implementation(project(":subprojects:test-runner:service"))
    implementation(project(":subprojects:logger:slf4j-gradle-logger"))

    implementation(project(":subprojects:common:result"))

    gradleTestImplementation(project(":subprojects:gradle:test-project"))
}

gradlePlugin {
    plugins {
        create("designScreenshots") {
            id = "com.avito.android.design-screenshots"
            implementationClass = "com.avito.plugin.ScreenshotsPlugin"
            displayName = "Screenshot testing"
        }
    }
}
