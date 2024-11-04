plugins {
    id("convention.kotlin-jvm")
    id("convention.publish-kotlin-library")
}

dependencies {
    implementation(libs.kotlinCompilerEmbeddable)

    testImplementation(project(":subprojects:gradle:test-project")) {
        because("File extensions") // todo probably move to :common:files
    }
}
