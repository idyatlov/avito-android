package com.avito.android.stats

import com.avito.logger.LoggerFactory
import com.avito.logger.create
import com.timgroup.statsd.NoOpStatsDClient
import com.timgroup.statsd.NonBlockingStatsDClient
import com.timgroup.statsd.StatsDClient
import com.timgroup.statsd.StatsDClientErrorHandler

internal class StatsDSenderImpl(
    private val config: StatsDConfig,
    loggerFactory: LoggerFactory
) : StatsDSender {

    private val logger = loggerFactory.create<StatsDSender>()

    private val errorHandler = StatsDClientErrorHandler {
        logger.warn("statsd error", it)
    }

    private val client: StatsDClient by lazy {
        when (config) {
            is StatsDConfig.Disabled -> NoOpStatsDClient()
            is StatsDConfig.Enabled -> try {
                NonBlockingStatsDClient(
                    config.namespace.toString(),
                    config.host,
                    config.port,
                    errorHandler
                )
            } catch (e: Exception) {
                logger.warn("Can't create statsDClient on main host", e)
                if (config.host == config.fallbackHost) {
                    NoOpStatsDClient()
                } else {
                    try {
                        NonBlockingStatsDClient(
                            config.namespace.toString(),
                            config.fallbackHost,
                            config.port,
                            errorHandler
                        )
                    } catch (err: Exception) {
                        errorHandler.handle(err)
                        NoOpStatsDClient()
                    }
                }
            }
        }
    }

    override fun send(metric: StatsMetric) {
        val aspect = metric.name.asAspect()
        when (metric) {
            is TimeMetric -> client.time(aspect, metric.timeInMs)
            is CountMetric -> client.count(aspect, metric.delta)
            is GaugeLongMetric -> client.gauge(aspect, metric.gauge)
            is GaugeDoubleMetric -> client.gauge(aspect, metric.gauge)
            is GaugeLongDeltaMetric -> client.recordGaugeDelta(aspect, metric.delta)
            is GaugeDoubleDeltaMetric -> client.recordGaugeDelta(aspect, metric.delta)
            is SetEventMetric -> client.recordSetEvent(aspect, metric.eventName)
        }
        if (config is StatsDConfig.Enabled) {
            logger.verbose("${metric.type}:${config.namespace}.$aspect:${metric.value}")
        } else {
            logger.verbose("Skip statsd event: ${metric.type}:<namespace>.$aspect:${metric.value}")
        }
    }
}
