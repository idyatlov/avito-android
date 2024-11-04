package com.avito.android.tech_budget.internal.lint_issues.upload

import com.avito.android.OwnerSerializerProvider
import com.avito.android.tech_budget.DumpInfoConfiguration
import com.avito.android.tech_budget.internal.dump.DumpInfo
import com.avito.android.tech_budget.internal.lint_issues.upload.model.LintIssuesRequestBody
import com.avito.android.tech_budget.internal.service.RetrofitBuilderService
import com.avito.android.tech_budget.internal.utils.executeWithHttpFailure
import com.avito.logger.GradleLoggerPlugin
import com.avito.logger.LoggerFactory
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import retrofit2.create

internal abstract class UploadLintIssuesTask : DefaultTask() {
    @get:OutputFiles
    abstract val outputXmlFiles: Property<FileCollection>

    @get:Nested
    abstract val dumpInfoConfiguration: Property<DumpInfoConfiguration>

    @get:Internal
    abstract val ownerSerializer: Property<OwnerSerializerProvider>

    @get:Internal
    abstract val retrofitBuilderService: Property<RetrofitBuilderService>

    private val loggerFactory: Provider<LoggerFactory> = GradleLoggerPlugin.provideLoggerFactory(this)

    @TaskAction
    fun uploadWarnings() {
        val dumpConfiguration = dumpInfoConfiguration.get()
        val api: UploadLintIssuesApi = retrofitBuilderService.get()
            .build(loggerFactory.get())
            .create()

        val lintIssueParser = LintXmlReportParser(project.rootDir.absolutePath)
        val lintIssues = lintIssueParser.parseXmlReport(outputXmlFiles.get().files)

        val logger = loggerFactory.get().create("LintIssues")
        if (lintIssues.isNotEmpty()) {
            logger.info("Uploading ${lintIssues.size} lint issues")
            api.dumpLintIssues(
                LintIssuesRequestBody(
                    dumpInfo = DumpInfo.fromExtension(dumpConfiguration),
                    lintIssues = lintIssues
                )
            ).executeWithHttpFailure(errorMessage = "Upload lint issues request failed")
        } else {
            logger.info("Nothing to upload. No lint issues found")
        }
    }

    companion object {
        const val NAME = "uploadLintIssues"
    }
}
