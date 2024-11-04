package com.avito.android.module_type

import com.avito.android.module_type.internal.CheckModuleDependenciesTask
import com.avito.android.module_type.internal.ExtractModuleDescriptionTask
import com.avito.kotlin.dsl.isRoot
import com.avito.kotlin.dsl.typedNamedOrNull
import com.avito.module.configurations.ConfigurationType
import com.avito.module.dependencies.directDependenciesOnProjects
import org.gradle.api.Plugin
import org.gradle.api.Project

public class ModuleTypesPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // The plugin may already be applied by other plugins, such as [ModuleTypeValidationPlugin].
        if (project.plugins.hasPlugin(ModuleTypesPlugin::class.java)) {
            return
        }

        if (project.isRoot()) {
            configureRootProject(project)
        } else {
            configureNonRootProject(project)
        }
    }

    private fun configureRootProject(project: Project) {
        val extension = project.extensions.create(
            ModuleTypeRootExtension.name,
            ModuleTypeRootExtension::class.java,
        )
        project.tasks.register(
            CheckModuleDependenciesTask.name,
            CheckModuleDependenciesTask::class.java
        ) { task ->
            with(extension.dependencyRestrictionsExtension) {
                task.group = "verification"
                task.betweenFunctionalTypesRestrictions.set(betweenFunctionalTypesRestrictions)
                task.betweenDifferentAppsRestriction.set(betweenDifferentAppsRestriction)
                task.toWiringRestriction.set(toWiringRestriction)
                task.solutionMessage.set(solutionMessage)
            }
        }
    }

    private fun configureNonRootProject(project: Project) {
        val extension = project.extensions.create(
            "module",
            ModuleTypeExtension::class.java,
        )
        project.registerExtractModuleDescriptionTask(extension)
    }

    private fun Project.registerExtractModuleDescriptionTask(extension: ModuleTypeExtension) {
        val checksTask = project.rootProject.tasks.typedNamedOrNull<CheckModuleDependenciesTask>(
            CheckModuleDependenciesTask.name
        )
        requireNotNull(checksTask) {
            "Plugin must be applied to the root project also"
        }
        checksTask.configure {
            it.dependsOn("${project.path}:${ExtractModuleDescriptionTask.name}")
            // Workaround for project isolation to wire tasks
            // - We can't read task's dependencies in execution phase
            // - We can't wire them through output/input because we need a project to get task provider
            it.dependentProjects.set(
                mutableSetOf(project.path) + it.dependentProjects.get()
            )
        }

        project.tasks.register(
            ExtractModuleDescriptionTask.name,
            ExtractModuleDescriptionTask::class.java
        ) { task ->
            task.modulePath.set(project.path)
            task.moduleType.set(extension.type)
            task.outputFile.set(
                project.layout.buildDirectory.file(ExtractModuleDescriptionTask.outputPath)
            )
            val directDependencies = project.directDependenciesOnProjects(ConfigurationType.values().toSet())
                .mapValues { it.value.map { it.path }.toSet() }
            task.directDependencies.set(directDependencies)
        }
    }
}

internal const val pluginId = "com.avito.android.module-types"
