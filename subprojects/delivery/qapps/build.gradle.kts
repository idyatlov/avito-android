plugins {
    id("convention.kotlin-jvm")
    id("convention.publish-gradle-plugin")
    id("convention.gradle-testing")
}

dependencies {
    implementation(project(":subprojects:delivery:sign-service")) {
        because("Creates qappsUploadSigned<Variant> tasks which is directly depends on corresponding signer task")
    }

    implementation(project(":subprojects:common:okhttp"))
    implementation(project(":subprojects:common:result"))
    api(project(":subprojects:gradle:android"))
    implementation(project(":subprojects:gradle:build-failer"))
    implementation(project(":subprojects:gradle:gradle-extensions"))
    implementation(project(":subprojects:gradle:statsd-config"))
    implementation(project(":subprojects:logger:slf4j-gradle-logger"))
    implementation(libs.retrofit)
    implementation(libs.retrofitConverterGson)
    implementation(libs.okhttpLogging)

    testImplementation(project(":subprojects:common:truth-extensions"))
    testImplementation(project(":subprojects:common:test-okhttp"))
    testImplementation(project(":subprojects:logger:logger"))

    gradleTestImplementation(project(":subprojects:gradle:test-project"))
    gradleTestImplementation(project(":subprojects:common:test-okhttp"))
}

gradlePlugin {
    plugins {
        create("qapps") {
            id = "com.avito.android.qapps"
            implementationClass = "com.avito.plugin.QAppsPlugin"
            displayName = "QApps"
        }
    }
}
