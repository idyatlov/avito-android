package com.avito.android.network_contracts.codegen

import com.avito.android.Result
import com.avito.android.isFailure
import com.avito.android.network_contracts.codegen.config.CodegenConfig
import com.avito.android.network_contracts.codegen.executor.Codegen
import com.avito.android.network_contracts.shared.throwGradleError
import com.avito.logger.Logger
import com.avito.logger.LoggerFactory
import com.avito.utils.ProcessRunner
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import java.io.File

@CacheableTask
internal abstract class CodegenTask : DefaultTask() {

    @get:Input
    abstract val kind: Property<String>

    @get:Input
    abstract val codegenProjectName: Property<String>

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    @get:Optional
    abstract val apiClassName: Property<String>

    @get:Input
    abstract val moduleName: Property<String>

    @get:Input
    abstract val skipValidation: Property<Boolean>

    @get:Input
    abstract val crtEnvName: Property<String>

    @get:Input
    abstract val keyEnvName: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    abstract val tmpCrtFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    abstract val tmpKeyFile: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val codegenExecutableFiles: ConfigurableFileCollection

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val schemesDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    abstract val moduleDirectory: DirectoryProperty

    @get:Internal
    internal abstract val loggerFactory: Property<LoggerFactory>

    private val logger: Logger by lazy { loggerFactory.get().create("CodegenTask") }

    @TaskAction
    fun generate() {
        val arch = findOperatingSystemArchitecture(outputDirectory.get().asFile)
        check(arch !is Arch.Unknown) { "Unsupported OS system: ${arch.rawValue}" }

        val config = CodegenConfig(
            packageName = packageName.get(),
            apiClassName = apiClassName.orNull.orEmpty(),
            schemesDirectoryRelativePath = schemesDir.get().asFile.toRelativeString(moduleDirectory.get().asFile),
            buildDirectoryRelativePath = outputDirectory.get().asFile.toRelativeString(moduleDirectory.get().asFile),
            moduleName = moduleName.get(),
            kind = kind.get(),
            name = codegenProjectName.get(),
            moduleDir = moduleDirectory.get().asFile,
            skipValidation = skipValidation.get(),
            crtEnv = crtEnvName.get() to tmpCrtFile.orNull?.asFile?.toPath(),
            keyEnv = keyEnvName.get() to tmpKeyFile.orNull?.asFile?.toPath(),
        )
        val codegen = Codegen.create(arch, codegenExecutableFiles, logger, config)

        val result = codegen.execute(
            skipValidation = skipValidation.get()
        )

        if (result.isFailure()) {
            throw GradleException("Network contracts generation failed:\n${result.throwable.message}", result.throwable)
        }
    }

    private fun findOperatingSystemArchitecture(processWorkingDirectory: File): Arch {
        val current = OperatingSystem.current()
        return when {
            current.isMacOsX -> {
                val processRunner = ProcessRunner.create(processWorkingDirectory)

                when (val archResult = processRunner.getOsxArchProcessor()) {
                    is Result.Success -> archResult.value
                    is Result.Failure -> throwGradleError(archResult.throwable.message.toString())
                }
            }

            current.isLinux -> Arch.LinuxAmd64

            else -> Arch.Unknown(current.name)
        }
    }

    companion object {
        const val NAME = "codegen"
    }
}

private fun ProcessRunner.getOsxArchProcessor() =
    run("/usr/bin/uname -m")
        .map(String::trim)
        .map(Arch.Companion::getArch)
