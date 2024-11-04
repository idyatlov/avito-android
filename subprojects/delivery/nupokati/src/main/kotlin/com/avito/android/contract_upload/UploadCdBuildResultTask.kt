package com.avito.android.contract_upload

import com.avito.android.artifactory_backup.ArtifactsAdapter
import com.avito.android.http.ArtifactoryClient
import com.avito.android.http.createArtifactoryHttpClient
import com.avito.android.model.input.CdBuildConfig
import com.avito.android.model.output.CdBuildResult
import com.avito.android.model.output.toCdCoordinates
import com.avito.git.gitStateProvider
import com.avito.reportviewer.ReportViewerLinksGeneratorImpl
import com.avito.reportviewer.ReportViewerQuery
import com.avito.reportviewer.model.ReportCoordinates
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

public abstract class UploadCdBuildResultTask : DefaultTask() {

    @get:Input
    public abstract val artifactoryUser: Property<String>

    @get:Input
    public abstract val artifactoryPassword: Property<String>

    @get:Input
    public abstract val reportViewerUrl: Property<String>

    @get:Input
    public abstract val reportCoordinates: Property<ReportCoordinates>

    @get:Input
    public abstract val teamcityBuildUrl: Property<String>

    @get:Input
    internal abstract val cdBuildConfig: Property<CdBuildConfig>

    @get:Input
    public abstract val appVersionCode: Property<Int>

    @get:InputFile
    public abstract val buildOutputFileProperty: RegularFileProperty

    @TaskAction
    public fun sendCdBuildResult() {
        val gitState = project.gitStateProvider()

        val reportLinksGenerator = ReportViewerLinksGeneratorImpl(
            reportViewerUrl = reportViewerUrl.get(),
            reportCoordinates = reportCoordinates.get(),
            reportViewerQuery = ReportViewerQuery.createForJvm()
        )

        val buildOutputFile = buildOutputFileProperty.get().asFile

        val cdBuildConfig = cdBuildConfig.get()

        val artifactsAdapter = ArtifactsAdapter(cdBuildConfig.schemaVersion)

        val artifacts = artifactsAdapter.fromJson(buildOutputFile.readText())

        createUploadAction().send(
            testResults = CdBuildResult.TestResultsLink(
                reportUrl = reportLinksGenerator.generateReportLink(filterOnlyFailures = false),
                reportCoordinates = reportCoordinates.get().toCdCoordinates()
            ),
            artifacts = artifacts,
            cdBuildConfig = cdBuildConfig,
            versionCode = appVersionCode.get(),
            teamcityUrl = teamcityBuildUrl.get(),
            gitState = gitState.get(),
        )
    }

    private fun createUploadAction(): UploadCdBuildResultTaskAction = UploadCdBuildResultTaskAction(
        client = ArtifactoryClient(
            createArtifactoryHttpClient(
                user = artifactoryUser.get(),
                password = artifactoryPassword.get(),
            )
        )
    )
}
