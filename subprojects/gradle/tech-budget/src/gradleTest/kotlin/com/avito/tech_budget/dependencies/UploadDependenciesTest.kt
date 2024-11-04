package com.avito.tech_budget.dependencies

import com.avito.android.utils.FAKE_OWNERSHIP_EXTENSION
import com.avito.android.utils.LIBS_OWNERS_TOML_CONTENT
import com.avito.android.utils.LIBS_VERSIONS_TOML_CONTENT
import com.avito.tech_budget.utils.dumpInfoExtension
import com.avito.tech_budget.utils.failureResponse
import com.avito.tech_budget.utils.successResponse
import com.avito.test.gradle.TestProjectGenerator
import com.avito.test.gradle.file
import com.avito.test.gradle.gradlew
import com.avito.test.gradle.module.AndroidAppModule
import com.avito.test.gradle.plugin.plugins
import com.avito.test.http.Mock
import com.avito.test.http.MockDispatcher
import com.avito.test.http.MockWebServerFactory
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class UploadDependenciesTest {

    private val mockDispatcher = MockDispatcher(unmockedResponse = successResponse())
    private val mockWebServer = MockWebServerFactory.create()
        .apply {
            dispatcher = mockDispatcher
        }

    @AfterEach
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `upload dependencies - dependencies`(@TempDir projectDir: File) {
        generateProject(projectDir)

        val request = mockDispatcher.captureRequest { path.contains("dumpModules") }

        uploadOwners(projectDir)
            .assertThat()
            .buildSuccessful()

        request.checks.singleRequestCaptured().jsonEquals(EXPECTED_DEPS_CODE_OWNERS)
    }

    @Test
    fun `upload owners - code ownership not applied - build failure`(@TempDir projectDir: File) {
        generateProject(projectDir, includeCodeOwnership = false)

        uploadOwners(projectDir, expectFailure = true)
            .assertThat()
            .buildFailed()
            .outputContains(
                "You must apply plugin `com.avito.android.code-ownership` to the root project to run this task"
            )
    }

    @Test
    fun `upload owners - version files not provided - build failure`(@TempDir projectDir: File) {
        generateProject(projectDir, includeVersionFiles = false)

        uploadOwners(projectDir, expectFailure = true)
            .assertThat()
            .buildFailed()
    }

    @Test
    fun `upload owners - upload error - build failure`(@TempDir projectDir: File) {
        mockDispatcher.registerMock(
            Mock(
                requestMatcher = { path.contains("dumpModules") },
                response = failureResponse()
            )
        )
        generateProject(projectDir)

        uploadOwners(projectDir, expectFailure = true)
            .assertThat()
            .buildFailed()
            .outputContains("Upload dependencies request failed")
    }

    private fun uploadOwners(
        projectDir: File,
        expectFailure: Boolean = false,
    ) =
        gradlew(
            projectDir,
            "uploadDependencies",
            "-Pcom.avito.android.tech-budget.enable=true",
            expectFailure = expectFailure
        )

    private fun generateProject(
        projectDir: File,
        includeCodeOwnership: Boolean = true,
        includeVersionFiles: Boolean = true
    ) = TestProjectGenerator(
        plugins = plugins {
            id("com.avito.android.gradle-logger")
            if (includeCodeOwnership) id("com.avito.android.code-ownership")
            id("com.avito.android.tech-budget")
            id("com.avito.android.tls-configuration")
        },
        useKts = true,
        buildGradleExtra = """
                ${if (includeCodeOwnership) FAKE_OWNERSHIP_EXTENSION else ""}
                techBudget {
                    ${dumpInfoExtension(mockWebServer.url("/").toString())}
                }
            """.trimIndent(),
        modules = listOf(
            AndroidAppModule(
                name = "app",
                imports = listOf("import com.avito.android.model.Owner"),
                plugins = plugins { id("com.avito.android.code-ownership") },
                useKts = true,
                buildGradleExtra = """
                    ownership {
                        owners(object: Owner { override fun toString() = "Speed" })
                    }
                """.trimIndent(),
            )
        )
    ).generateIn(projectDir).also {
        if (includeVersionFiles) {
            projectDir.file("gradle/libs.versions.toml", LIBS_VERSIONS_TOML_CONTENT)
            projectDir.file("gradle/libs.owners.toml", LIBS_OWNERS_TOML_CONTENT)
        }
    }

    private companion object {
        @Language("json")
        private val EXPECTED_DEPS_CODE_OWNERS = """
           {
               "dumpInfo":{
                  "commitHash":"123",
                  "commitDate":"2022-10-31",
                  "project":"avito"
               },
               "modules":[
                  {
                     "moduleName":":app",
                     "owners":[
                        "SpeedID"
                     ],
                     "type":"internal"
                  },
                  {
                     "moduleName":"io.gitlab.arturbosch.detekt",
                     "owners":[
                        "SpeedID"
                     ],
                     "type":"external"
                  },
                  {
                     "moduleName":"com.google.code.gson:gson",
                     "owners":[
                        "SpeedID"
                     ],
                     "type":"external"
                  },
                  {
                     "moduleName":"androidx.core:core",
                     "owners":[
                        "MessengerID"
                     ],
                     "type":"external"
                  },
                  {
                     "moduleName":"androidx.constraintlayout:constraintlayout",
                     "owners":[
                        "MessengerID"
                     ],
                     "type":"external"
                  }
               ]
            } 
        """.trimIndent()
    }
}
