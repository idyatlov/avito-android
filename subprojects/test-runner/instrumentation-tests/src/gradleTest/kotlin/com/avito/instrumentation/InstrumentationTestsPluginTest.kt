package com.avito.instrumentation

import com.avito.test.gradle.TestProjectGenerator
import com.avito.test.gradle.ciRun
import com.avito.test.gradle.gradlew
import com.avito.test.gradle.module.AndroidAppModule
import com.avito.test.gradle.module.AndroidLibModule
import com.avito.test.gradle.module.Module
import com.avito.test.gradle.plugin.plugins
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class InstrumentationTestsPluginTest {

    @Test
    fun `configuration - ok - empty instrumentation block`(@TempDir projectDir: File) {
        createProject(
            projectDir = projectDir,
            module = AndroidAppModule(
                "app",
                plugins = plugins {
                    id(instrumentationPluginId)
                },
                buildGradleExtra = """
                    instrumentation {
                    }
                """.trimIndent(),
                useKts = true,
            )
        )

        gradlew(projectDir, "help", dryRun = true).assertThat().buildSuccessful()
    }

    @Test
    fun `tasks resolution - ok - empty instrumentation block`(@TempDir projectDir: File) {
        createProject(
            projectDir = projectDir,
            module = AndroidAppModule(
                "app",
                plugins = plugins {
                    id(instrumentationPluginId)
                },
                buildGradleExtra = """
                    instrumentation {
                    }
                """.trimIndent(),
                useKts = true,
            )
        )

        gradlew(projectDir, "tasks", dryRun = false).assertThat().buildSuccessful()
    }

    /**
     * IDE will turn red resolving script with plugin applied, it uses tasks or some equivalent in process
     *
     * todo Parameter: teamcityBuildId is required (must be digit)
     */
    @Disabled
    @Test
    fun `tasks resolution - ok - with configurations set and no args`(@TempDir projectDir: File) {
        createProject(
            projectDir = projectDir,
            module = AndroidAppModule(
                "app",
                plugins = plugins {
                    id(instrumentationPluginId)
                },
                buildGradleExtra = instrumentationConfiguration(),
                useKts = true,
            )
        )

        gradlew(projectDir, "tasks", dryRun = false).assertThat().buildSuccessful()
    }

    @Test
    fun `run instrumentation by name - ok - in application project`(@TempDir projectDir: File) {
        val moduleName = "app"

        createProject(
            projectDir = projectDir,
            module = AndroidAppModule(
                moduleName,
                plugins = plugins {
                    id(instrumentationPluginId)
                },
                buildGradleExtra = instrumentationConfiguration(),
                useKts = true,
            )
        )

        runGradle(projectDir, ":$moduleName:instrumentationTwoTest", "-PrunOnlyFailedTests=false").assertThat()
            .run {
                tasksShouldBeTriggered(":$moduleName:instrumentationTwoTest")
            }
    }

    @Test
    fun `run instrumentation by name - ok - in application project with flavors`(@TempDir projectDir: File) {
        val moduleName = "app"

        createProject(
            projectDir = projectDir,
            module = AndroidAppModule(
                moduleName,
                plugins = plugins {
                    id(instrumentationPluginId)
                },
                buildGradleExtra = """
                    |${instrumentationConfiguration()}
                    |    
                    |android {
                    |   flavorDimensions("version")
                    |   productFlavors {
                    |       register("demo") { 
                    |           setDimension("version")
                    |       }
                    |       register("full") {
                    |           setDimension("version")
                    |       }
                    |   }
                    |}
                    |""".trimMargin(),
                useKts = true,
            )
        )

        runGradle(
            projectDir,
            ":$moduleName:instrumentationDemoTwoTest",
            ":$moduleName:instrumentationFullTwoTest",
            "-PrunOnlyFailedTests=false"
        ).assertThat()
            .run {
                tasksShouldBeTriggered(
                    ":$moduleName:instrumentationDemoTwoTest",
                    ":$moduleName:instrumentationFullTwoTest"
                ).inOrder()
            }
    }

    @Test
    fun `run instrumentation by name - ok - in application project with multidimensional flavors`(
        @TempDir projectDir: File
    ) {
        val moduleName = "app"

        createProject(
            projectDir = projectDir,
            module = AndroidAppModule(
                moduleName,
                plugins = plugins {
                    id(instrumentationPluginId)
                },
                buildGradleExtra = """
                    |${instrumentationConfiguration()}
                    |    
                    |android {
                    |   flavorDimensions("version", "monetization")
                    |    productFlavors {
                    |       register("demo") { 
                    |           setDimension("version")
                    |       }
                    |       register("full") {
                    |           setDimension("version")
                    |       }
                    |       register("free") {
                    |           setDimension("monetization")
                    |       }
                    |       register("paid") {
                    |           setDimension("monetization")
                    |       }
                    |    }
                    |}
                    |""".trimMargin(),
                useKts = true,
            )
        )

        runGradle(
            projectDir,
            ":$moduleName:instrumentationDemoFreeTwoTest",
            ":$moduleName:instrumentationFullPaidTwoTest",
            "-PrunOnlyFailedTests=false"
        ).assertThat()
            .run {
                tasksShouldBeTriggered(
                    ":$moduleName:instrumentationDemoFreeTwoTest",
                    ":$moduleName:instrumentationFullPaidTwoTest"
                ).inOrder()
            }
    }

    @Test
    fun `run instrumentation by name - ok - in library project`(@TempDir projectDir: File) {
        val moduleName = "lib"

        createProject(
            projectDir = projectDir,
            module = AndroidLibModule(
                moduleName,
                plugins = plugins {
                    id(instrumentationPluginId)
                },
                buildGradleExtra = instrumentationConfiguration(),
                useKts = true,
            )
        )

        runGradle(projectDir, ":$moduleName:instrumentationTwoTest", "-PrunOnlyFailedTests=false").assertThat()
            .run {
                tasksShouldBeTriggered(":$moduleName:instrumentationTwoTest")
            }
    }

    private fun createProject(projectDir: File, module: Module) {
        TestProjectGenerator(
            plugins = plugins {
                id("com.avito.android.gradle-logger")
            },
            modules = listOf(module)
        ).generateIn(projectDir)
    }

    private fun runGradle(projectDir: File, vararg args: String) =
        ciRun(
            projectDir, *args,
            "-PdeviceName=LOCAL",
            "-PteamcityBuildId=0",
            "-Papp.versionName=1",
            "-Papp.versionCode=1",
            "-Pavito.bitbucket.url=http://bitbucket",
            "-Pavito.bitbucket.projectKey=AA",
            "-Pavito.bitbucket.repositorySlug=android",
            "-Pavito.stats.enabled=false",
            "-Pavito.stats.host=http://stats",
            "-Pavito.stats.fallbackHost=http://stats",
            "-Pavito.stats.port=80",
            "-Pavito.stats.namespace=android",
            dryRun = true
        )
}

internal fun instrumentationConfiguration(): String = """
    |import com.avito.instrumentation.reservation.request.Device
    |import com.avito.instrumentation.configuration.KubernetesViaCredentials
    |import com.avito.kotlin.dsl.getOptionalStringProperty
    |import com.avito.instrumentation.configuration.report.ReportConfig
    |
    |instrumentation {
    |    outputDir.set(project.file("outputs"))
    |    report.set(ReportConfig.NoOp)
    |    environments {
    |       register<KubernetesViaCredentials>("test") {
    |           url.set("http://stub")
    |           namespace.set("android-emulator")
    |           token.set(getOptionalStringProperty("kubernetesToken"))
    |       }
    |    }
    |    
    |    instrumentationParams = mapOf(
    |        "jobSlug" to "FunctionalTests"
    |    )
    |    
    |    configurations {
    |
    |        register("functional") {
    |            instrumentationParams = mapOf(
    |                "deviceName" to "api22"
    |            )
    |
    |            targets {
    |                register("api22") {
    |                    deviceName = "api22"
    |
    |                    scheduling {
    |                        quota {
    |                            minimumSuccessCount = 1
    |                        }
    |
    |                        staticDevicesReservation {
    |                            device = Device.LocalEmulator.device(27)
    |                            count = 1
    |                        }
    |                    }
    |                }
    |            }
    |        }
    |
    |        register("two") {
    |            instrumentationParams = mapOf(
    |                "deviceName" to "api22"
    |            )
    |
    |            targets {
    |                register("api22") {
    |                    deviceName = "api22"
    |
    |                    scheduling {
    |                        quota {
    |                            minimumSuccessCount = 1
    |                        }
    |
    |                        staticDevicesReservation {
    |                            device = Device.LocalEmulator.device(27)
    |                            count = 1
    |                        }
    |                    }
    |                }
    |            }
    |        }
    |    }
    |}
    |
    |android {
    |    defaultConfig {
    |        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    |        testInstrumentationRunnerArguments(mapOf("planSlug" to "AvitoAndroid"))
    |    }
    |}
    |""".trimMargin()
