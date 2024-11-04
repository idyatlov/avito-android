package com.avito.test.summary

import com.avito.android.stats.statsdConfig
import com.avito.logger.GradleLoggerPlugin
import com.avito.test.summary.json.TestSummaryDestinationDeserializer
import com.avito.test.summary.model.TestSummaryDestination
import com.avito.test.summary.sender.AlertinoTestSummarySender
import com.avito.test.summary.sender.TestSummarySender
import com.google.gson.GsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

public abstract class TestSummaryTask : DefaultTask() {

    @get:Nested
    public abstract val alertinoExtension: Property<AlertinoExtension>

    @get:Nested
    public abstract val reportViewerExtension: Property<ReportViewerExtension>

    @get:Input
    public abstract val buildUrl: Property<String>

    @TaskAction
    public fun doWork() {
        val loggerFactory = GradleLoggerPlugin.provideLoggerFactory(this).get()
        val logger = loggerFactory.create("TestSummaryTask")
        logger.info("Executing testSummary task")

        val alertino = alertinoExtension.get()

        val reportViewer = reportViewerExtension.get()

        val di = TestSummaryDI(
            project.statsdConfig,
            loggerFactory
        )

        val reportsApi = di.provideReportsApi(reportViewer.reportsHost.get())
        val gson = GsonBuilder()
            .registerTypeAdapter(
                TestSummaryDestination::class.java,
                TestSummaryDestinationDeserializer()
            ).create()
        val testSummarySender: TestSummarySender = AlertinoTestSummarySender(
            alertinoBaseUrl = alertino.alertinoEndpoint.get(),
            alertinoTemplate = alertino.alertinoTemplate.get(),
            alertinoTemplatePlaceholder = alertino.alertinoTemplatePlaceholder.get(),
            reportViewerUrl = reportViewer.url.get(),
            reportsApi = reportsApi,
            buildUrl = buildUrl.get(),
            reportCoordinates = reportViewer.reportCoordinates.get(),
            globalSummaryChannel = alertino.summaryChannel.get(),
            testSummaryDestination = alertino.testSummaryDestination.get().asFile,
            gson = gson,
            loggerFactory = loggerFactory
        )

        logger.info("Sending result")
        testSummarySender.send()
    }
}
