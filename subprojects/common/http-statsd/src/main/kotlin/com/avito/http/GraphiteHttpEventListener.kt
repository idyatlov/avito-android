package com.avito.http

import com.avito.android.graphite.GraphiteMetric
import com.avito.android.graphite.GraphiteSender
import com.avito.graphite.series.SeriesName
import com.avito.logger.LoggerFactory
import com.avito.time.TimeProvider

public class GraphiteHttpEventListener(
    private val graphite: GraphiteSender,
    timeProvider: TimeProvider,
    requestMetadataProvider: RequestMetadataProvider,
    metricsPrefix: SeriesName = SeriesName.create("network"),
    loggerFactory: LoggerFactory,
) : MetricHttpEventListener(
    timeProvider,
    requestMetadataProvider,
    metricsPrefix,
    loggerFactory,
) {

    override fun send(seriesName: SeriesName, executionTimeMs: Long) {
        graphite.send(GraphiteMetric(seriesName, executionTimeMs.toString()))
    }
}
