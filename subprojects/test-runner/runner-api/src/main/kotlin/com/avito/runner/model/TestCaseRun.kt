package com.avito.runner.model

import com.avito.test.model.TestCase
import java.nio.file.Path

public data class TestCaseRun(
    val test: TestCase,
    val result: Result,
    val timestampStartedMilliseconds: Long,
    val timestampCompletedMilliseconds: Long
) {
    val durationMilliseconds: Long
        get() = timestampCompletedMilliseconds - timestampStartedMilliseconds

    public sealed class Result {
        public sealed class Passed : Result() {
            public object Regular : Passed()

            public data class WithMacrobenchmarkOutputs(
                public val outputFiles: List<Path> = emptyList()
            ) : Passed()
        }

        public object Ignored : Result()
        public sealed class Failed : Result() {
            public data class InRun(val errorMessage: String) : Failed()

            public sealed class InfrastructureError : Failed() {

                public abstract val error: Throwable

                public class Unexpected(override val error: Throwable) : InfrastructureError()

                public class FailedOnStart(override val error: Throwable) : InfrastructureError()

                public class FailedOnParsing(override val error: Throwable) : InfrastructureError()

                public class FailOnPullingArtifacts(override val error: Throwable) : InfrastructureError()

                public class Timeout(
                    public val timeoutMin: Long,
                    override val error: Throwable
                ) : InfrastructureError()
            }
        }
    }

    public companion object
}
