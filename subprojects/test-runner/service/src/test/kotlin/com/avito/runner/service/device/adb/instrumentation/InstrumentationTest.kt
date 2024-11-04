package com.avito.runner.service.device.adb.instrumentation

import com.avito.cli.Notification
import com.avito.runner.model.TestCaseRun
import com.avito.runner.service.worker.device.adb.instrumentation.InstrumentationEntry
import com.avito.runner.service.worker.device.adb.instrumentation.InstrumentationEntry.InstrumentationTestEntry
import com.avito.runner.service.worker.device.adb.instrumentation.InstrumentationTestCaseRunParser
import com.avito.runner.service.worker.model.InstrumentationTestCaseRun
import com.avito.test.model.TestName
import com.avito.truth.isInstanceOf
import com.avito.utils.ResourcesReader
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import rx.Observable
import rx.observers.TestSubscriber
import rx.schedulers.Schedulers
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.concurrent.TimeUnit

internal class InstrumentationTest {

    private val instrumentationParser = InstrumentationTestCaseRunParser.Impl()

    @Test
    fun `read instrumentation output - emits expected entries - with failed test`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-failed-test.txt")

        val subscriber = instrumentationParser
            .readInstrumentationOutput(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        val entries = subscriber.onNextEvents.eraseTime()

        assertThat(entries).containsExactlyElementsIn(
            listOf(
                InstrumentationTestEntry(
                    numTests = 4,
                    stream = "com.example.test.TestClass:",
                    id = "AndroidJUnitRunner",
                    test = "test1",
                    clazz = "com.example.test.TestClass",
                    current = 1,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Start,
                    timestampMilliseconds = 0
                ),
                InstrumentationTestEntry(
                    numTests = 4,
                    stream = """Error in test1(com.example.test.TestClass):
java.net.UnknownHostException: Test Exception
at com.example.test.TestClass.test1.1.invoke(TestClass.kt:245)
at com.example.test.TestClass.test1.1.invoke(TestClass.kt:44)
at com.example.test.TestClass.test1(TestClass.kt:238)
at java.lang.reflect.Method.invoke(Native Method)
at org.junit.runners.model.FrameworkMethod.1.runReflectiveCall(FrameworkMethod.java:50)
at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
at org.junit.rules.ExpectedException.ExpectedExceptionStatement.evaluate(ExpectedException.java:239)
at com.example.test.utils.LaunchAppRule.apply.1.evaluate(LaunchAppRule.kt:36)
at com.example.test.utils.RetryRule.runTest(RetryRule.kt:43)
at com.example.test.utils.RetryRule.access.runTest(RetryRule.kt:14)
at com.example.test.utils.RetryRule.apply.1.evaluate(RetryRule.kt:29)
at org.junit.rules.RunRules.evaluate(RunRules.java:20)
at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)
at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)
at org.junit.runners.ParentRunner.3.run(ParentRunner.java:290)
at org.junit.runners.ParentRunner.1.schedule(ParentRunner.java:71)
at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
at org.junit.runners.ParentRunner.access.000(ParentRunner.java:58)
at org.junit.runners.ParentRunner.2.evaluate(ParentRunner.java:268)
at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
at org.junit.runners.Suite.runChild(Suite.java:128)
at org.junit.runners.Suite.runChild(Suite.java:27)
at org.junit.runners.ParentRunner.3.run(ParentRunner.java:290)
at org.junit.runners.ParentRunner.1.schedule(ParentRunner.java:71)
at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
at org.junit.runners.ParentRunner.access.000(ParentRunner.java:58)
at org.junit.runners.ParentRunner.2.evaluate(ParentRunner.java:268)
at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
at org.junit.runner.JUnitCore.run(JUnitCore.java:115)
at android.support.test.internal.runner.TestExecutor.execute(TestExecutor.java:59)
at android.support.test.runner.JunoAndroidRunner.onStart(JunoAndroidRunner.kt:107)
at android.app.Instrumentation.InstrumentationThread.run(Instrumentation.java:1932)""",
                    id = "AndroidJUnitRunner",
                    test = "test1",
                    clazz = "com.example.test.TestClass",
                    current = 1,
                    stack = """java.net.UnknownHostException: Test Exception
at com.example.test.TestClass.test1.1.invoke(TestClass.kt:245)
at com.example.test.TestClass.test1.1.invoke(TestClass.kt:44)
at com.example.test.TestClass.test1(TestClass.kt:238)
at java.lang.reflect.Method.invoke(Native Method)
at org.junit.runners.model.FrameworkMethod.1.runReflectiveCall(FrameworkMethod.java:50)
at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
at org.junit.rules.ExpectedException.ExpectedExceptionStatement.evaluate(ExpectedException.java:239)
at com.example.test.utils.LaunchAppRule.apply.1.evaluate(LaunchAppRule.kt:36)
at com.example.test.utils.RetryRule.runTest(RetryRule.kt:43)
at com.example.test.utils.RetryRule.access.runTest(RetryRule.kt:14)
at com.example.test.utils.RetryRule.apply.1.evaluate(RetryRule.kt:29)
at org.junit.rules.RunRules.evaluate(RunRules.java:20)
at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)
at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)
at org.junit.runners.ParentRunner.3.run(ParentRunner.java:290)
at org.junit.runners.ParentRunner.1.schedule(ParentRunner.java:71)
at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
at org.junit.runners.ParentRunner.access.000(ParentRunner.java:58)
at org.junit.runners.ParentRunner.2.evaluate(ParentRunner.java:268)
at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
at org.junit.runners.Suite.runChild(Suite.java:128)
at org.junit.runners.Suite.runChild(Suite.java:27)
at org.junit.runners.ParentRunner.3.run(ParentRunner.java:290)
at org.junit.runners.ParentRunner.1.schedule(ParentRunner.java:71)
at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
at org.junit.runners.ParentRunner.access.000(ParentRunner.java:58)
at org.junit.runners.ParentRunner.2.evaluate(ParentRunner.java:268)
at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
at org.junit.runner.JUnitCore.run(JUnitCore.java:115)
at android.support.test.internal.runner.TestExecutor.execute(TestExecutor.java:59)
at android.support.test.runner.JunoAndroidRunner.onStart(JunoAndroidRunner.kt:107)
at android.app.Instrumentation.InstrumentationThread.run(Instrumentation.java:1932)""",
                    statusCode = InstrumentationTestEntry.StatusCode.Failure,
                    timestampMilliseconds = 0
                ),
                InstrumentationTestEntry(
                    numTests = 4,
                    stream = "",
                    id = "AndroidJUnitRunner",
                    test = "test2",
                    clazz = "com.example.test.TestClass",
                    current = 2,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Start,
                    timestampMilliseconds = 0
                ),
                InstrumentationTestEntry(
                    numTests = 4,
                    stream = ".",
                    id = "AndroidJUnitRunner",
                    test = "test2",
                    clazz = "com.example.test.TestClass",
                    current = 2,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Ok,
                    timestampMilliseconds = 0
                ),
                InstrumentationTestEntry(
                    numTests = 4,
                    stream = "com.example.test.TestClass:",
                    id = "AndroidJUnitRunner",
                    test = "test3",
                    clazz = "com.example.test.TestClass",
                    current = 3,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Start,
                    timestampMilliseconds = 0
                ),
                InstrumentationTestEntry(
                    numTests = 4,
                    stream = ".",
                    id = "AndroidJUnitRunner",
                    test = "test3",
                    clazz = "com.example.test.TestClass",
                    current = 3,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Ok,
                    timestampMilliseconds = 0
                ),
                InstrumentationTestEntry(
                    numTests = 4,
                    stream = "",
                    id = "AndroidJUnitRunner",
                    test = "test4",
                    clazz = "com.example.test.TestClass",
                    current = 4,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Start,
                    timestampMilliseconds = 0
                ),
                InstrumentationTestEntry(
                    numTests = 4,
                    stream = ".",
                    id = "AndroidJUnitRunner",
                    test = "test4",
                    clazz = "com.example.test.TestClass",
                    current = 4,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Ok,
                    timestampMilliseconds = 0
                ),
                InstrumentationEntry.InstrumentationResultEntry(
                    statusCode = InstrumentationEntry.InstrumentationResultEntry.StatusCode.Ok,
                    timestampMilliseconds = 0
                )
            )
        )
    }

    @Test
    fun `read instrumentation output - completes stream - with failed test`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-failed-test.txt")

        val subscriber = instrumentationParser
            .readInstrumentationOutput(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        subscriber.assertCompleted()
    }

    @Test
    fun `read instrumentation output - does not emit error - with failed test`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-failed-test.txt")

        val subscriber = instrumentationParser
            .readInstrumentationOutput(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        subscriber.assertNoErrors()
    }

    @Test
    fun `read instrumentation output as tests - emits expected tests - with failed test`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-failed-test.txt")

        val subscriber = instrumentationParser
            .parse(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        val tests: List<InstrumentationTestCaseRun> = subscriber.onNextEvents.eraseDuration()

        assertThat(tests).containsExactlyElementsIn(
            listOf<InstrumentationTestCaseRun>(
                InstrumentationTestCaseRun.CompletedTestCaseRun(
                    name = TestName("com.example.test.TestClass", "test1"),
                    result = TestCaseRun.Result.Failed.InRun(
                        errorMessage = """java.net.UnknownHostException: Test Exception
at com.example.test.TestClass.test1.1.invoke(TestClass.kt:245)
at com.example.test.TestClass.test1.1.invoke(TestClass.kt:44)
at com.example.test.TestClass.test1(TestClass.kt:238)
at java.lang.reflect.Method.invoke(Native Method)
at org.junit.runners.model.FrameworkMethod.1.runReflectiveCall(FrameworkMethod.java:50)
at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
at org.junit.rules.ExpectedException.ExpectedExceptionStatement.evaluate(ExpectedException.java:239)
at com.example.test.utils.LaunchAppRule.apply.1.evaluate(LaunchAppRule.kt:36)
at com.example.test.utils.RetryRule.runTest(RetryRule.kt:43)
at com.example.test.utils.RetryRule.access.runTest(RetryRule.kt:14)
at com.example.test.utils.RetryRule.apply.1.evaluate(RetryRule.kt:29)
at org.junit.rules.RunRules.evaluate(RunRules.java:20)
at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)
at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)
at org.junit.runners.ParentRunner.3.run(ParentRunner.java:290)
at org.junit.runners.ParentRunner.1.schedule(ParentRunner.java:71)
at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
at org.junit.runners.ParentRunner.access.000(ParentRunner.java:58)
at org.junit.runners.ParentRunner.2.evaluate(ParentRunner.java:268)
at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
at org.junit.runners.Suite.runChild(Suite.java:128)
at org.junit.runners.Suite.runChild(Suite.java:27)
at org.junit.runners.ParentRunner.3.run(ParentRunner.java:290)
at org.junit.runners.ParentRunner.1.schedule(ParentRunner.java:71)
at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
at org.junit.runners.ParentRunner.access.000(ParentRunner.java:58)
at org.junit.runners.ParentRunner.2.evaluate(ParentRunner.java:268)
at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
at org.junit.runner.JUnitCore.run(JUnitCore.java:115)
at android.support.test.internal.runner.TestExecutor.execute(TestExecutor.java:59)
at android.support.test.runner.JunoAndroidRunner.onStart(JunoAndroidRunner.kt:107)
at android.app.Instrumentation.InstrumentationThread.run(Instrumentation.java:1932)"""
                    ),
                    timestampStartedMilliseconds = 0,
                    timestampCompletedMilliseconds = 0
                ),
                InstrumentationTestCaseRun.CompletedTestCaseRun(
                    name = TestName("com.example.test.TestClass", "test2"),
                    result = TestCaseRun.Result.Passed.Regular,
                    timestampStartedMilliseconds = 0,
                    timestampCompletedMilliseconds = 0
                ),
                InstrumentationTestCaseRun.CompletedTestCaseRun(
                    name = TestName("com.example.test.TestClass", "test3"),
                    result = TestCaseRun.Result.Passed.Regular,
                    timestampStartedMilliseconds = 0,
                    timestampCompletedMilliseconds = 0
                ),
                InstrumentationTestCaseRun.CompletedTestCaseRun(
                    name = TestName("com.example.test.TestClass", "test4"),
                    result = TestCaseRun.Result.Passed.Regular,
                    timestampStartedMilliseconds = 0,
                    timestampCompletedMilliseconds = 0
                )
            )
        )
    }

    @Test
    fun `read instrumentation output on unexpected exit before start - emits failed on instrumentation parsing`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-unexpected-exit-before-start.txt")

        val subscriber = instrumentationParser
            .parse(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        val tests: List<InstrumentationTestCaseRun> = subscriber.onNextEvents

        assertThat(tests[0])
            .isInstanceOf<InstrumentationTestCaseRun.FailedOnInstrumentationParsing>()
    }

    @Test
    fun `read instrumentation output on unexpected exit after start - emits failed on instrumentation parsing`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-unexpected-exit-after-start.txt")

        val subscriber = instrumentationParser
            .parse(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        val tests: List<InstrumentationTestCaseRun> = subscriber.onNextEvents

        assertThat(tests[0])
            .isInstanceOf<InstrumentationTestCaseRun.FailedOnInstrumentationParsing>()
    }

    @Test
    fun `read instrumentation output - emits only result entry - with 0 tests`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-0-tests.txt")

        val subscriber = instrumentationParser
            .readInstrumentationOutput(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        val entries = subscriber.onNextEvents.eraseTime()
        assertThat(entries).containsExactlyElementsIn(
            listOf<InstrumentationEntry>(
                InstrumentationEntry.InstrumentationResultEntry(
                    statusCode = InstrumentationEntry.InstrumentationResultEntry.StatusCode.Ok,
                    timestampMilliseconds = 0
                )
            )
        )
    }

    @Test
    fun `read instrumentation output - completes stream - with 0 tests`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-0-tests.txt")

        val subscriber = instrumentationParser
            .readInstrumentationOutput(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        subscriber.assertCompleted()
    }

    @Test
    fun `read instrumentation output - does not emit error - with 0 tests`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-0-tests.txt")

        val subscriber = instrumentationParser
            .readInstrumentationOutput(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        subscriber.assertNoErrors()
    }

    @Test
    fun `read instrumentation output as tests - does not emit error - with 0 tests`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-0-tests.txt")

        val subscriber = instrumentationParser
            .parse(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        subscriber.assertNoErrors()
    }

    @Test
    fun `read instrumentation output as tests - completes stream - with 0 tests`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-0-tests.txt")

        val subscriber = instrumentationParser
            .parse(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        subscriber.assertCompleted()
    }

    @Test
    fun `read instrumentation output as tests - does not emit any test - with 0 tests`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-0-tests.txt")

        val subscriber = instrumentationParser
            .parse(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        subscriber.assertNoValues()
    }

    @Test
    fun `read instrumentation output - emits expected entries - unordered output`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-unordered-output.txt")

        val subscriber = instrumentationParser
            .readInstrumentationOutput(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        val entries = subscriber.onNextEvents.eraseTime()
        assertThat(entries).containsExactlyElementsIn(
            listOf(
                InstrumentationTestEntry(
                    numTests = 3,
                    stream = "com.example.test.TestClass:",
                    id = "AndroidJUnitRunner",
                    test = "test1",
                    clazz = "com.example.test.TestClass",
                    current = 1,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Start,
                    timestampMilliseconds = 0
                ),
                InstrumentationTestEntry(
                    numTests = 3,
                    stream = ".",
                    id = "AndroidJUnitRunner",
                    test = "test1",
                    clazz = "com.example.test.TestClass",
                    current = 1,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Ok,
                    timestampMilliseconds = 0
                ),
                InstrumentationTestEntry(
                    numTests = 3,
                    stream = "com.example.test.TestClass:",
                    id = "AndroidJUnitRunner",
                    test = "test2",
                    clazz = "com.example.test.TestClass",
                    current = 2,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Start,
                    timestampMilliseconds = 0
                ),
                InstrumentationTestEntry(
                    numTests = 3,
                    stream = "",
                    id = "AndroidJUnitRunner",
                    test = "test3",
                    clazz = "com.example.test.TestClass",
                    current = 3,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Start,
                    timestampMilliseconds = 0
                ),
                InstrumentationTestEntry(
                    numTests = 3,
                    stream = ".",
                    id = "AndroidJUnitRunner",
                    test = "test2",
                    clazz = "com.example.test.TestClass",
                    current = 2,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Ok,
                    timestampMilliseconds = 0
                ),
                InstrumentationTestEntry(
                    numTests = 3,
                    stream = ".",
                    id = "AndroidJUnitRunner",
                    test = "test3",
                    clazz = "com.example.test.TestClass",
                    current = 3,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Ok,
                    timestampMilliseconds = 0
                ),
                InstrumentationEntry.InstrumentationResultEntry(
                    statusCode = InstrumentationEntry.InstrumentationResultEntry.StatusCode.Ok,
                    timestampMilliseconds = 0
                )
            )
        )
    }

    @Test
    fun `read instrumentation output - completes stream - unordered output`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-unordered-output.txt")

        val subscriber = instrumentationParser
            .readInstrumentationOutput(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        subscriber.assertCompleted()
    }

    @Test
    fun `read instrumentation output - does not emit error - unordered output`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-unordered-output.txt")

        val subscriber = instrumentationParser
            .readInstrumentationOutput(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        subscriber.assertNoErrors()
    }

    @Test
    fun `read instrumentation output as tests - emits expected tests - unordered output`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-unordered-output.txt")

        val subscriber = instrumentationParser
            .parse(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        val tests = subscriber.onNextEvents.eraseDuration()
        assertThat(tests).containsExactlyElementsIn(
            listOf<InstrumentationTestCaseRun>(
                InstrumentationTestCaseRun.CompletedTestCaseRun(
                    name = TestName("com.example.test.TestClass", "test1"),
                    result = TestCaseRun.Result.Passed.Regular,
                    timestampStartedMilliseconds = 0,
                    timestampCompletedMilliseconds = 0
                ),
                InstrumentationTestCaseRun.CompletedTestCaseRun(
                    name = TestName("com.example.test.TestClass", "test2"),
                    result = TestCaseRun.Result.Passed.Regular,
                    timestampStartedMilliseconds = 0,
                    timestampCompletedMilliseconds = 0
                ),
                InstrumentationTestCaseRun.CompletedTestCaseRun(
                    name = TestName("com.example.test.TestClass", "test3"),
                    result = TestCaseRun.Result.Passed.Regular,
                    timestampStartedMilliseconds = 0,
                    timestampCompletedMilliseconds = 0
                )
            )
        )
    }

    @Test
    fun `read instrumentation output as tests - completes stream - unordered output`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-unordered-output.txt")

        val subscriber = instrumentationParser
            .parse(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        subscriber.assertCompleted()
    }

    @Test
    fun `read instrumentation output as tests - does not emit error - unordered output`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-unordered-output.txt")

        val subscriber = instrumentationParser
            .parse(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        subscriber.assertNoErrors()
    }

    @Test
    fun `read instrumentation output - emits expected entries - ignored test`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-ignored-test.txt")

        val subscriber = instrumentationParser
            .readInstrumentationOutput(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        val entries = subscriber.onNextEvents.eraseTime()
        assertThat(entries).containsExactlyElementsIn(
            listOf(
                InstrumentationTestEntry(
                    numTests = 2,
                    stream = "com.example.test.TestClass:",
                    id = "AndroidJUnitRunner",
                    test = "test1",
                    clazz = "com.example.test.TestClass",
                    current = 1,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Start,
                    timestampMilliseconds = 0
                ),
                InstrumentationTestEntry(
                    numTests = 2,
                    stream = ".",
                    id = "AndroidJUnitRunner",
                    test = "test1",
                    clazz = "com.example.test.TestClass",
                    current = 1,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Ok,
                    timestampMilliseconds = 0
                ),
                InstrumentationTestEntry(
                    numTests = 2,
                    stream = "",
                    id = "AndroidJUnitRunner",
                    test = "test2",
                    clazz = "com.example.test.TestClass",
                    current = 2,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Start,
                    timestampMilliseconds = 0
                ),
                InstrumentationTestEntry(
                    numTests = 2,
                    stream = "",
                    id = "AndroidJUnitRunner",
                    test = "test2",
                    clazz = "com.example.test.TestClass",
                    current = 2,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Ignored,
                    timestampMilliseconds = 0
                ),
                InstrumentationEntry.InstrumentationResultEntry(
                    statusCode = InstrumentationEntry.InstrumentationResultEntry.StatusCode.Ok,
                    timestampMilliseconds = 0
                )
            )
        )
    }

    @Test
    fun `read instrumentation output - completes stream - ignored test`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-ignored-test.txt")

        val subscriber = instrumentationParser
            .readInstrumentationOutput(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        subscriber.assertCompleted()
    }

    @Test
    fun `read instrumentation output - does not emit error - ignored test`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-ignored-test.txt")

        val subscriber = instrumentationParser
            .readInstrumentationOutput(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        subscriber.assertNoErrors()
    }

    @Test
    fun `read instrumentation output as tests - emits expected tests - ignored test`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-ignored-test.txt")

        val subscriber = instrumentationParser
            .parse(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        val entries = subscriber.onNextEvents.eraseDuration()
        assertThat(entries).containsExactlyElementsIn(
            listOf<InstrumentationTestCaseRun>(
                InstrumentationTestCaseRun.CompletedTestCaseRun(
                    name = TestName("com.example.test.TestClass", "test1"),
                    result = TestCaseRun.Result.Passed.Regular,
                    timestampStartedMilliseconds = 0,
                    timestampCompletedMilliseconds = 0
                ),
                InstrumentationTestCaseRun.CompletedTestCaseRun(
                    name = TestName("com.example.test.TestClass", "test2"),
                    result = TestCaseRun.Result.Ignored,
                    timestampStartedMilliseconds = 0,
                    timestampCompletedMilliseconds = 0
                )
            )
        )
    }

    @Test
    fun `read instrumentation output - emits error result entry - process crashed while test run`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-crashed-process.txt")

        val subscriber = instrumentationParser
            .readInstrumentationOutput(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        val entries = subscriber.onNextEvents.eraseTime()
        assertThat(entries).containsExactlyElementsIn(
            listOf(
                InstrumentationTestEntry(
                    numTests = 1,
                    stream = "ru.test.testapplication.ExampleInstrumentedTest:",
                    id = "AndroidJUnitRunner",
                    test = "failedTestCrash",
                    clazz = "ru.test.testapplication.ExampleInstrumentedTest",
                    current = 1,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Start,
                    timestampMilliseconds = 0
                ),
                InstrumentationEntry.InstrumentationResultEntry(
                    shortMessage = "Process crashed.",
                    statusCode = InstrumentationEntry.InstrumentationResultEntry.StatusCode.Error,
                    timestampMilliseconds = 0
                )
            )
        )
    }

    @Test
    fun `read instrumentation output as tests - emits failed test - process crashed while test run`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-crashed-process.txt")

        val subscriber = instrumentationParser
            .parse(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        val entries = subscriber.onNextEvents.eraseDuration()
        assertThat(entries).containsExactlyElementsIn(
            listOf<InstrumentationTestCaseRun>(
                InstrumentationTestCaseRun.CompletedTestCaseRun(
                    name = TestName("ru.test.testapplication.ExampleInstrumentedTest", "failedTestCrash"),
                    result = TestCaseRun.Result.Failed.InRun("Process crashed."),
                    timestampStartedMilliseconds = 0,
                    timestampCompletedMilliseconds = 0
                )
            )
        )
    }

    @Test
    fun `read instrumentation output - emits error result entry - process crashed before test run`() {
        val outputWithFailedTest =
            ResourcesReader.readFile("instrumentation-output-crashed-process-before-test-ran.txt")

        val subscriber = instrumentationParser
            .readInstrumentationOutput(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        val entries = subscriber.onNextEvents.eraseTime()
        assertThat(entries).containsExactlyElementsIn(
            listOf<InstrumentationEntry>(
                InstrumentationEntry.InstrumentationResultEntry(
                    shortMessage = "Process crashed.",
                    statusCode = InstrumentationEntry.InstrumentationResultEntry.StatusCode.Error,
                    timestampMilliseconds = 0
                )
            )
        )
    }

    @Test
    fun `read instrumentation output as tests - emits failed test - process crashed before test run`() {
        val outputWithFailedTest =
            ResourcesReader.readFile("instrumentation-output-crashed-process-before-test-ran.txt")

        val subscriber = instrumentationParser
            .parse(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        val entries = subscriber.onNextEvents.eraseDuration()
        assertThat(entries).containsExactlyElementsIn(
            listOf<InstrumentationTestCaseRun>(
                InstrumentationTestCaseRun.FailedOnStartTestCaseRun(
                    message = "Process crashed."
                )
            )
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `read instrumentation output - emits error result entry - process crashed before test run after one completed test`() {
        val outputWithFailedTest =
            ResourcesReader.readFile("instrumentation-output-crashed-process-before-test-ran-after-completed-test.txt")

        val subscriber = instrumentationParser
            .readInstrumentationOutput(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        val entries = subscriber.onNextEvents.eraseTime()
        assertThat(entries).containsExactlyElementsIn(
            listOf(
                InstrumentationTestEntry(
                    numTests = 2,
                    stream = "",
                    id = "AndroidJUnitRunner",
                    test = "test1",
                    clazz = "com.example.test.TestClass",
                    current = 1,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Start,
                    timestampMilliseconds = 0
                ),
                InstrumentationTestEntry(
                    numTests = 2,
                    stream = ".",
                    id = "AndroidJUnitRunner",
                    test = "test1",
                    clazz = "com.example.test.TestClass",
                    current = 1,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Ok,
                    timestampMilliseconds = 0
                ),
                InstrumentationEntry.InstrumentationResultEntry(
                    shortMessage = "Process crashed.",
                    statusCode = InstrumentationEntry.InstrumentationResultEntry.StatusCode.Error,
                    timestampMilliseconds = 0
                )
            )
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `read instrumentation output as tests - emits failed test - process crashed before test run after one completed test`() {
        val outputWithFailedTest =
            ResourcesReader.readFile("instrumentation-output-crashed-process-before-test-ran-after-completed-test.txt")

        val subscriber = instrumentationParser
            .parse(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        val entries = subscriber.onNextEvents.eraseDuration()
        assertThat(entries).containsExactlyElementsIn(
            listOf(
                InstrumentationTestCaseRun.CompletedTestCaseRun(
                    name = TestName("com.example.test.TestClass", "test1"),
                    result = TestCaseRun.Result.Passed.Regular,
                    timestampStartedMilliseconds = 0,
                    timestampCompletedMilliseconds = 0
                ),
                InstrumentationTestCaseRun.FailedOnStartTestCaseRun(
                    message = "Process crashed."
                )
            )
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `read instrumentation output - emits error result entry with fields from run entry - this fields not found in failed entry`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-test-failed-without-test-field.txt")

        val subscriber = instrumentationParser
            .readInstrumentationOutput(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        val entries = subscriber.onNextEvents.eraseTime()
        assertThat(entries).containsExactlyElementsIn(
            listOf(
                InstrumentationTestEntry(
                    numTests = 1,
                    stream = "com.avito.android.feature.advert.publish.verticals." +
                        "AutoUsedSubCategoryItem_IsPublishedWithGivenParams_WhenAddedByUserWithoutListingFees:",
                    id = "AndroidJUnitRunner",
                    test = "dataSet5",
                    clazz = "com.avito.android.feature.advert.publish.verticals." +
                        "AutoUsedSubCategoryItem_IsPublishedWithGivenParams_WhenAddedByUserWithoutListingFees",
                    current = 1,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Start,
                    timestampMilliseconds = 0
                ),
                InstrumentationTestEntry(
                    numTests = 1,
                    stream = "Error in dataSet5(com.avito.android.feature.advert.publish.verticals." +
                        "AutoUsedSubCategoryItem_IsPublishedWithGivenParams_WhenAddedByUserWithoutListingFees):\n" +
                        "androidx.test.espresso.NoMatchingViewException: " +
                        "No views in hierarchy found matching: (is descendant of a: " +
                        "(with id: com.avito.android.dev:id/input_view and has child: " +
                        "with string from resource id: <2131755562>[phone] value: Телефон) " +
                        "and with id: 2131296759 and используйте контейнер InputView, в качестве матчера)",
                    id = "AndroidJUnitRunner",
                    test = "dataSet5",
                    clazz = "com.avito.android.feature.advert.publish.verticals." +
                        "AutoUsedSubCategoryItem_IsPublishedWithGivenParams_WhenAddedByUserWithoutListingFees",
                    current = 1,
                    stack = "",
                    statusCode = InstrumentationTestEntry.StatusCode.Failure,
                    timestampMilliseconds = 0
                ),
                InstrumentationEntry.InstrumentationResultEntry(
                    shortMessage = "",
                    longMessage = "",
                    timestampMilliseconds = 0,
                    statusCode = InstrumentationEntry.InstrumentationResultEntry.StatusCode.Ok
                )
            )
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `read instrumentation output as tests - emits failed test with fields from run entry - this fields not found in failed entry`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-test-failed-without-test-field.txt")

        val subscriber = instrumentationParser
            .parse(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        val entries = subscriber.onNextEvents.eraseDuration()
        assertThat(entries).containsExactlyElementsIn(
            listOf<InstrumentationTestCaseRun>(
                InstrumentationTestCaseRun.CompletedTestCaseRun(
                    name = TestName(
                        "com.avito.android.feature.advert.publish.verticals." +
                            "AutoUsedSubCategoryItem_IsPublishedWithGivenParams_WhenAddedByUserWithoutListingFees",
                        "dataSet5"
                    ),
                    result = TestCaseRun.Result.Failed.InRun(
                        errorMessage = ""
                    ),
                    timestampStartedMilliseconds = 0,
                    timestampCompletedMilliseconds = 0
                )
            )
        )
    }

    @Test
    fun `read invalid instrumentation output - emits error`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-invalid-command.txt")

        val subscriber = instrumentationParser
            .parse(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        val entries = subscriber.onNextEvents.eraseDuration()
        assertThat(entries)
            .hasSize(1)

        assertThat(entries[0])
            .isInstanceOf<InstrumentationTestCaseRun.FailedOnInstrumentationParsing>()

        assertThat(subscriber.onErrorEvents)
            .isEmpty()
    }

    @Test
    fun `read instrumentation output - completes stream - with additional outputs`() {
        val outputWithFailedTest = ResourcesReader.readFile("instrumentation-output-additional-outputs.txt")
        val outputFilePathFromSampleOutput = "/storage/emulated/0/Android/media/com.avito.android.macrobenchmark/" +
            "BaselineProfileGenerator_startup-baseline-prof.txt"

        val subscriber = instrumentationParser
            .parse(
                getInstrumentationOutput(outputWithFailedTest)
            )
            .subscribeAndWait()

        val entries = subscriber.onNextEvents.eraseDuration()
        val run = InstrumentationTestCaseRun.CompletedTestCaseRun(
            name = TestName(
                "com.avito.android.macrobenchmark.baselineprofile.BaselineProfileGenerator",
                "startup"
            ),
            result = TestCaseRun.Result.Passed.WithMacrobenchmarkOutputs(
                outputFiles = listOf(outputFilePathFromSampleOutput).map { File(it).toPath() }
            ),
            timestampStartedMilliseconds = 0,
            timestampCompletedMilliseconds = 0
        )
        assertThat(entries).containsExactlyElementsIn(
            listOf<InstrumentationTestCaseRun>(
                run, run, run, run, run, run
            )
        )
    }

    private fun <T> Observable<T>.subscribeAndWait() = TestSubscriber<T>()
        .apply {
            subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(this)
        }
        .apply {
            awaitTerminalEvent(30, TimeUnit.SECONDS)
        }

    private fun List<InstrumentationEntry>.eraseTime() = map {
        when (it) {
            is InstrumentationTestEntry -> it.copy(timestampMilliseconds = 0)
            is InstrumentationEntry.InstrumentationResultEntry -> it.copy(timestampMilliseconds = 0)
            is InstrumentationEntry.InstrumentationMacrobenchmarkOutputEntry -> it
        }
    }

    private fun List<InstrumentationTestCaseRun>.eraseDuration() = map {
        when (it) {
            is InstrumentationTestCaseRun.CompletedTestCaseRun -> it.copy(
                timestampStartedMilliseconds = 0,
                timestampCompletedMilliseconds = 0
            )
            else -> it
        }
    }

    private fun getInstrumentationOutput(output: File): Observable<Notification> {
        return Observable.unsafeCreate {
            val reader = BufferedReader(
                FileReader(output)
            )

            var line: String? = reader.readLine()
            while (line != null) {
                it.onNext(
                    Notification.Output(line = line)
                )
                line = reader.readLine()
            }

            it.onNext(
                Notification.Exit(output = output.readText())
            )

            it.onCompleted()
        }
    }
}
