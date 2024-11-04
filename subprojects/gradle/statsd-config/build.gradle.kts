plugins {
    id("convention.kotlin-jvm")
    id("convention.publish-kotlin-library")
}

dependencies {
    api(project(":subprojects:common:statsd"))

    implementation(gradleApi())
    implementation(project(":subprojects:gradle:gradle-extensions"))
    implementation(project(":subprojects:logger:slf4j-gradle-logger"))
}
