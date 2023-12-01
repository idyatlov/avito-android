package com.avito.android.baseline_profile

import com.avito.android.baseline_profile.configuration.SaveProfileToVersionControlExtension
import com.avito.android.baseline_profile.internal.BaselineProfileFileLocationExtensions.baselineProfileTargetLocation
import com.avito.android.baseline_profile.internal.BaselineProfileFileLocationExtensions.findProfileOrThrow
import com.avito.android.baseline_profile.internal.GitClient
import com.avito.logger.GradleLoggerPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@CacheableTask
internal abstract class ApplyProfileToSourceCodeTask : DefaultTask() {
    @get:Input
    abstract val testOutputsDirectory: DirectoryProperty

    @get:Input
    abstract val applicationModuleName: Property<String>

    @get:Input
    abstract val extension: Property<SaveProfileToVersionControlExtension>

    private val loggerFactory by lazy {
        GradleLoggerPlugin.provideLoggerFactory(project.rootProject).get()
    }

    private val gitClient by lazy {
        GitClient(
            rootProjectDir = project.rootProject.projectDir,
            loggerFactory = loggerFactory,
            extension = extension.get(),
        )
    }

    override fun getDescription() =
        """
            |Copy baseline profile from ci artifacts directory to application source directory.
            |Then, optionally, commit and push it to VCS.
        """.trimMargin()

    @TaskAction
    fun copy() {
        val targetProfileLocation = requireApplicationProject()
            .baselineProfileTargetLocation()
            .asFile

        val profile = testOutputsDirectory.get().findProfileOrThrow()
        profile.copyTo(targetProfileLocation, overwrite = true)

        if (extension.get().enable.getOrElse(false)) {
            gitClient.commitAndPushProfile(targetProfileLocation.toPath())
        }
    }

    private fun requireApplicationProject(): Project {
        val projectByName = project.rootProject.findProject(applicationModuleName.get())
        return requireNotNull(projectByName) {
            "Could not resolve application project by provided module name - ${applicationModuleName.get()}"
        }
    }

    companion object {
        const val taskName: String = "copyProfileToSourcesAndSaveToVcs"
    }
}
