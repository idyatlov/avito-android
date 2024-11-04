package com.avito.android.stats

import com.avito.graphite.series.SeriesName

/**
 * [Statsd metrics](https://github.com/statsd/statsd/blob/master/docs/metric_types.md#statsd-metric-types)
 */
public sealed class StatsMetric {

    public abstract val name: SeriesName
    public abstract val value: Any
    public abstract val type: String

    public abstract fun copy(name: SeriesName, value: Any): StatsMetric

    public companion object {
        public fun time(name: SeriesName, ms: Long): TimeMetric = TimeMetric(name, ms)
        public fun count(name: SeriesName, value: Long = 1): CountMetric = CountMetric(name, value)
        public fun gaugeLong(name: SeriesName, value: Long): GaugeLongMetric = GaugeLongMetric(name, value)
        public fun gaugeDouble(name: SeriesName, value: Double): GaugeDoubleMetric = GaugeDoubleMetric(name, value)
    }
}

/**
 * [Timing](https://github.com/statsd/statsd/blob/master/docs/metric_types.md#timing)
 */
public data class TimeMetric(
    override val name: SeriesName,
    // todo use java.time.api
    public val timeInMs: Long
) : StatsMetric() {
    override val value: Long = timeInMs
    override val type: String = "time"

    override fun copy(name: SeriesName, value: Any): TimeMetric =
        TimeMetric(name, value as Long)
}

/**
 * [Counting](https://github.com/statsd/statsd/blob/master/docs/metric_types.md#counting)
 */
public data class CountMetric(
    override val name: SeriesName,
    public val delta: Long = 1
) : StatsMetric() {
    override val value: Long = delta
    override val type: String = "count"

    override fun copy(name: SeriesName, value: Any): CountMetric =
        CountMetric(name, value as Long)
}

/**
 * [Gauges](https://github.com/statsd/statsd/blob/master/docs/metric_types.md#gauges)
 */
public data class GaugeLongMetric(
    override val name: SeriesName,
    public val gauge: Long
) : StatsMetric() {
    override val value: Long = gauge
    override val type: String = "gauge"

    override fun copy(name: SeriesName, value: Any): GaugeLongMetric =
        GaugeLongMetric(name, value as Long)
}

/**
 * [Gauges](https://github.com/statsd/statsd/blob/master/docs/metric_types.md#gauges)
 */
public data class GaugeDoubleMetric(
    override val name: SeriesName,
    public val gauge: Double
) : StatsMetric() {
    override val value: Double = gauge
    override val type: String = "gauge"

    override fun copy(name: SeriesName, value: Any): GaugeDoubleMetric =
        GaugeDoubleMetric(name, value as Double)
}

/**
 * [Gauges](https://github.com/statsd/statsd/blob/master/docs/metric_types.md#gauges)
 */
public data class GaugeLongDeltaMetric(
    override val name: SeriesName,
    public val delta: Long
) : StatsMetric() {
    override val value: Long = delta
    override val type: String = "gauge_delta"

    override fun copy(name: SeriesName, value: Any): GaugeLongDeltaMetric =
        GaugeLongDeltaMetric(name, value as Long)
}

/**
 * [Gauges](https://github.com/statsd/statsd/blob/master/docs/metric_types.md#gauges)
 */
public data class GaugeDoubleDeltaMetric(
    override val name: SeriesName,
    public val delta: Double
) : StatsMetric() {
    override val value: Double = delta
    override val type: String = "gauge_delta"

    override fun copy(name: SeriesName, value: Any): GaugeDoubleDeltaMetric =
        GaugeDoubleDeltaMetric(name, value as Double)
}

/**
 * [Sets](https://github.com/statsd/statsd/blob/master/docs/metric_types.md#sets)
 */
public data class SetEventMetric(
    override val name: SeriesName,
    public val eventName: String
) : StatsMetric() {
    override val value: String = eventName
    override val type: String = "set"

    override fun copy(name: SeriesName, value: Any): SetEventMetric =
        SetEventMetric(name, value as String)
}
