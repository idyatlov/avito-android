package com.avito.test.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.configurationcache.extensions.serviceOf
import org.gradle.initialization.GradlePropertiesController
import org.gradle.testfixtures.ProjectBuilder

public fun rootProject(): Project =
    ProjectBuilder.builder()
        .withName("root")
        .build()

public fun androidApp(name: String, parent: Project): Project {
    val project = buildProject(name, parent)
    project.plugins.apply("com.android.application")
    project.repositories.run {
        add(mavenCentral())
        add(google())
    }
    return project
}

public fun androidLib(name: String, parent: Project): Project {
    val project = buildProject(name, parent)
    project.plugins.apply("com.android.library")
    project.repositories.run {
        add(mavenCentral())
        add(google())
    }
    return project
}

public fun javaLib(name: String, parent: Project): Project {
    val project = buildProject(name, parent)
    project.plugins.apply(JavaLibraryPlugin::class.java)
    return project
}

public fun Project.apiDependency(dependency: Project) {
    dependency("api", dependency)
}

public fun Project.implementationDependency(dependency: Project) {
    dependency("implementation", dependency)
}

private fun Project.dependency(configuration: String, dependency: Project) {
    with(dependencies) {
        add(configuration, project(mapOf("path" to ":${dependency.path}")))
    }
}

private fun buildProject(name: String, parent: Project): Project {
    val project = ProjectBuilder.builder()
        .withName(name)
        .withParent(parent)
        .build()

    // workaround for https://github.com/gradle/gradle/issues/16774
    parent.serviceOf<GradlePropertiesController>().loadGradlePropertiesFrom(parent.rootDir, true)

    return project
}
