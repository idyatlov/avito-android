plugins {
    id("convention.kotlin-jvm")
    id("convention.publish-kotlin-library")
}

dependencies {
    implementation(gradleApi())

    testImplementation(project(":subprojects:gradle:test-project"))
}
