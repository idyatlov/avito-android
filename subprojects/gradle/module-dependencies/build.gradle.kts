plugins {
    id("convention.kotlin-jvm")
    id("convention.publish-kotlin-library")
}

dependencies {
    compileOnly(gradleApi())

    implementation(project(":subprojects:gradle:gradle-extensions"))
}
