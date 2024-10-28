package com.avito.http

import com.avito.android.Result
import com.avito.graphite.series.SeriesName
import com.avito.logger.LoggerFactory
import com.avito.logger.create
import com.avito.time.TimeProvider
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException

public abstract class MetricHttpEventListener(
    private val timeProvider: TimeProvider,
    private val requestMetadataProvider: RequestMetadataProvider,
    private val metricPrefix: SeriesName,
    loggerFactory: LoggerFactory,
) : EventListener() {

    private val logger = loggerFactory.create<MetricHttpEventListener>()

    private val successResponseCode = 200..299

    private var callStarted = 0L

    private var responseCode: Int? = null

    override fun callStart(call: Call) {
        callStarted = timeProvider.nowInMillis()
    }

    override fun responseHeadersEnd(call: Call, response: Response) {
        if (response.isSuccessful) {
            responseCode = response.code
        } else {
            sendCode(
                call = call,
                code = response.code.toString(),
                latencyMs = response.receivedResponseAtMillis - response.sentRequestAtMillis
            )
        }
    }

    override fun callEnd(call: Call) {
        if (responseCode in successResponseCode) {
            sendCode(
                call = call,
                code = responseCode.toString(),
                latencyMs = timeProvider.nowInMillis() - callStarted
            )
        }
    }

    override fun callFailed(call: Call, ioe: IOException) {

        val latencyMs = timeProvider.nowInMillis() - callStarted

        val code = if (ioe is SocketTimeoutException) {
            "timeout"
        } else {
            "unknown"
        }
        send(serviceMetric(call.request()), code, latencyMs)
    }

    protected abstract fun send(seriesName: SeriesName, executionTimeMs: Long)

    private fun sendCode(call: Call, code: String, latencyMs: Long) {
        send(serviceMetric(call.request()), code, latencyMs)
    }

    private fun send(seriesName: Result<SeriesName>, code: String, executionTimeMs: Long) {
        seriesName
            .onSuccess { series ->
                send(series.append(code), executionTimeMs)
            }
    }

    private fun serviceMetric(request: Request): Result<SeriesName> {
        return requestMetadataProvider.provide(request)
            .map {
                metricPrefix.append(it.serviceName, it.methodName)
            }.onFailure { error ->
                val urlWithoutParams = request.url.newBuilder()
                    .query(null)
                    .build()

                logger.warn(
                    msg = """
                        |Failed to send metrics. RequestMetadata not available for: ${request.method} $urlWithoutParams
                        |Possible solutions:
                        | - If you use OkHttp then add okhttp.Request.tag() with type RequestMetadata
                        | - If you use Retrofit add @Tag annotation with type RequestMetadata
                        | - If original request creation not available for modification use custom RequestMetadataProvider
                    """.trimMargin(),
                    error = error
                )
            }
    }
}
