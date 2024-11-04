package com.avito.android.test.report

import android.view.View
import androidx.test.espresso.AppNotIdleException
import androidx.test.espresso.FailureHandler
import androidx.test.espresso.NoMatchingRootException
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.PerformException
import junit.framework.AssertionFailedError
import org.hamcrest.Matcher

public class ReportFriendlyFailureHandler : FailureHandler {

    private val normalizers = listOf(
        RegexNormalizer(
            Regex("View Hierarchy:.+", RegexOption.DOT_MATCHES_ALL)
        ),
        RegexNormalizer(
            Regex("Problem views are marked with.+", RegexOption.DOT_MATCHES_ALL)
        ),
        RegexToPatternMessageNormalizer(
            Regex("Expected: with text: is \"(.+)\"\\s+Got: \".+res-name=(.*?), .+text=(.*?),.+\""),
            "Во view: \"{2}\"\nожидался текст: \"{1}\"\nполучили: \"{3}\""
        ),
        RegexToPatternMessageNormalizer(
            Regex("No views in hierarchy found matching: (.+)"),
            "Не найдена view в иерархии: \"{1}\""
        ),
        RegexToPatternMessageNormalizer(
            Regex("View is not present in the hierarchy.+"),
            "Не найдена view в иерархии"
        ),
        RegexToPatternMessageNormalizer(
            Regex("with decor view of type .+ did not match any of the following roots:"),
            "Не найдена view в иерархии указанного window"
        ),
        RegexToPatternMessageNormalizer(
            Regex("Found 0 items matching holder with view.+"),
            "Не найдена view в recycler "
        ),
        RegexToPatternMessageNormalizer(
            Regex("Parameter specified as non-null is null.+parameter (.+)"),
            "Параметр {1} == null"
        ),
        RegexToPatternMessageNormalizer(
            Regex(".+Perform action single click on.+: \\((.+)\\).+"),
            "Не удалось кликнуть по элементу в Recycler: {1}"
        )
    )

    override fun handle(error: Throwable, viewMatcher: Matcher<View>?) {
        throw when {
            error.isCausedBy<AppNotIdleException> { it.message.orEmpty().contains("ASYNC_TASKS_HAVE_IDLED") } -> {
                val asyncTaskThreads = Thread.getAllStackTraces()
                    .filterKeys { it.name.contains("AsyncTask") }
                val threadDump = asyncTaskThreads.entries.joinToString(separator = "\n") { it.dumpState() }
                val exception = createExceptionWithPrivateStringConstructor<AppNotIdleException>(
                    "AsyncTask is still running in background. Thread dump:\n$threadDump"
                )
                exception.initCause(error)
                exception
            }
            error.isCausedBy<AppNotIdleException> { it.message.orEmpty().contains("Looped for ") } -> {
                val exception = createExceptionWithPrivateStringConstructor<AppNotIdleException>(
                    "Main thread is busy. " +
                        "The probable cause is in animations."
                )
                exception.initCause(error)
                exception
            }
            error is PerformException ->
                // RecyclerView descendant checks implemented via ViewActions (gross)
                if (error.actionDescription.startsWith("Check descendant view")) {
                    AssertionFailedError("Не прошла проверка: ${error.actionDescription}").apply {
                        initCause(error.cause)
                    }
                } else {
                    PerformException.Builder()
                        .from(error)
                        .withViewDescription(minimizeViewDescription(error.viewDescription))
                        .build()
                }
            error is AssertionError -> AssertionFailedError(error.normalizedMessage()).apply { initCause(error.cause) }
            error is NoMatchingRootException -> error.toNormalizedException()
            error is NoMatchingViewException -> error.toNormalizedException()
            else -> error
        }
    }

    private inline fun <reified T : Exception> T.toNormalizedException(): T {
        val exception = createExceptionWithPrivateStringConstructor<T>(this.normalizedMessage())
        exception.stackTrace = this.stackTrace
        return exception
    }

    private fun Throwable.normalizedMessage(): String {
        var failureMessage: String = this.message ?: return "No message"

        normalizers.forEach { failureMessage = it.normalize(failureMessage) }

        return failureMessage
    }

    private inline fun <reified T : Throwable> Throwable.isCausedBy(matcher: (error: T) -> Boolean): Boolean {
        var error: Throwable? = this
        while (error != null) {
            if (T::class.isInstance(error) && matcher(error as T)) return true
            error = error.cause
        }
        return false
    }

    private fun minimizeViewDescription(viewDescription: String): String {
        // not a redundant escape, will crash on some androids without it
        @Suppress("RegExpRedundantEscape")
        return RegexToPatternMessageNormalizer(
            Regex("([a-zA-Z].*?)\\{.+res-name=(.*?),.+\\}"),
            "{1}(R.id.{2})"
        ).normalize(viewDescription)
    }

    /**
     * Removes all changing data from failure messages to group it afterwards
     */
    private interface Normalizer {
        // TODO: accept original exception to preserve information about Exception class
        fun normalize(failureMessage: String): String
    }

    /**
     * @param pattern string with {1} {2} ... etc placeholders, will be replaced by [regex] matches one by one
     */
    private class RegexToPatternMessageNormalizer(
        private val regex: Regex,
        private val pattern: String
    ) : Normalizer {

        override fun normalize(failureMessage: String): String {
            regex.find(failureMessage)?.apply {
                var result = pattern

                groupValues.forEachIndexed { index, match ->
                    result = result.replace("{$index}", match.replace("\n", " "))
                }
                return result
            }

            return failureMessage
        }
    }

    /**
     * Replaces [regex] with [replacement] in failure message
     *
     * example
     * was   : LinearLayout$LayoutParams@1aeef385
     * become: LinearLayout$LayoutParams
     */
    private class RegexNormalizer(
        private val regex: Regex,
        private val replacement: String = ""
    ) : Normalizer {

        override fun normalize(failureMessage: String): String = failureMessage.replace(regex, replacement)
    }
}

private fun Map.Entry<Thread, Array<StackTraceElement>>.dumpState(): String {
    val thread = this.key
    val stacktrace = this.value
    return "Thread: ${thread.name}  state: ${thread.state}" + stacktrace.joinToString(separator = "\n") { "    $it" }
}

internal inline fun <reified T : Exception> createExceptionWithPrivateStringConstructor(message: String): T {
    val constructor = T::class.java.getDeclaredConstructor(String::class.java)
    constructor.isAccessible = true
    return constructor.newInstance(message)
}
