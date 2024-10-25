plugins {
    id("convention.kotlin-jvm-android")
    id("convention.publish-kotlin-library")
}

dependencies {
    api(project(":subprojects:common:series"))
    api(project(":subprojects:logger:logger"))
    api(libs.androidAnnotations)
}
