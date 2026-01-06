package de.infix.testBalloon.framework.core

import de.infix.testBalloon.framework.shared.internal.Constants
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.milliseconds

class TestSuiteTests {
    private val iSep = Constants.INTERNAL_PATH_ELEMENT_SEPARATOR

    @Test
    fun sequentialExecution() = withTestFramework {
        val subSuiteCount = 3
        val testCount = 3
        val expectedTestElementPaths = ConcurrentList<String>()

        val suite by testSuite("topSuite") {
            for (suiteNumber in 1..subSuiteCount) {
                testSuite("subSuite$suiteNumber") {
                    for (testNumber in 1..testCount) {
                        expectedTestElementPaths.add(
                            "topSuite${iSep}subSuite$suiteNumber${iSep}test$testNumber"
                        )
                        test("test$testNumber") {
                            delay((4 - testNumber).milliseconds)
                        }
                    }
                }
            }
        }

        withTestReport(suite) {
            with(finishedTestEvents()) {
                assertTrue(isNotEmpty())
                assertAllSucceeded()
                assertElementPathsContainInOrder(expectedTestElementPaths.elements(), exhaustive = true)
            }
        }
    }

    @Test
    fun aroundAll() = withTestFramework {
        val trace = ConcurrentList<String>()

        val suite1 by testSuite(
            "suite1",
            testConfig = TestConfig.aroundAll { tests ->
                trace.add("$testElementPath aroundAll begin")
                tests()
                trace.add("$testElementPath aroundAll end")
            }
        ) {
            test("test1") {
                trace.add("$testElementPath")
            }

            testSuite("innerSuite") {
                test("test1") {
                    trace.add("$testElementPath")
                }
            }
        }

        withTestReport(suite1) {
            assertContentEquals(
                listOf(
                    "«suite1» aroundAll begin",
                    "«suite1${iSep}test1»",
                    "«suite1${iSep}innerSuite${iSep}test1»",
                    "«suite1» aroundAll end"
                ),
                trace.elements()
            )
        }
    }

    @Test
    fun aroundAllWithoutInnerInvocation() = withTestFramework {
        val suite1 by testSuite("suite1", testConfig = TestConfig.aroundAll {}) {
            test("test1") {}
        }

        withTestReport(suite1) {
            finishedTestEvents().any {
                it.throwable?.message?.contains("the element action has not been invoked") == true
            }
        }
    }

    @Test
    fun aroundAllWithoutInnerInvocationPermitted() = withTestFramework {
        val suite1 by testSuite(
            "suite1",
            testConfig = TestConfig.permits(TestConfig.Permit.WrapperWithoutInnerInvocation).aroundAll {}
        ) {
            test("test1") {}
        }

        withTestReport(suite1) {
            finishedTestEvents().all { it.succeeded }
        }
    }

    @Test
    fun suiteWithoutChildren() = withTestFramework {
        val suite1 by testSuite("suite1") {
        }

        assertFails {
            withTestReport(suite1) {}
        }.also {
            assertContains(it.cause?.message ?: "", "does not contain any child tests or test suites")
        }
    }

    @Test
    fun suiteWithoutChildrenPermitted() = withTestFramework {
        val suite1 by testSuite("suite1", testConfig = TestConfig.permits(TestConfig.Permit.SuiteWithoutChildren)) {
        }

        withTestReport(suite1) {
            finishedTestEvents().all { it.succeeded }
        }
    }

    @Test
    fun suiteWithEmptyName() = withTestFramework {
        val suite1 by testSuite("") {
        }

        assertFails {
            withTestReport(suite1) {}
        }.also {
            assertContains(it.message ?: "", "with an empty or blank name ''")
        }
    }

    @Test
    fun suiteWithBlankName() = withTestFramework {
        val suite1 by testSuite(" ") {
        }

        assertFails {
            withTestReport(suite1) {}
        }.also {
            assertContains(it.message ?: "", "with an empty or blank name ' '")
        }
    }

    @Test
    fun suiteWithEmptyDisplayName() = withTestFramework {
        val suite1 by testSuite("suite1", displayName = "") {
        }

        assertFails {
            withTestReport(suite1) {}
        }.also {
            assertContains(it.message ?: "", "with an empty or blank displayName ''")
        }
    }

    @Test
    fun suiteWithBlankDisplayName() = withTestFramework {
        val suite1 by testSuite("suite1", displayName = " ") {
        }

        assertFails {
            withTestReport(suite1) {}
        }.also {
            assertContains(it.message ?: "", "with an empty or blank displayName ' '")
        }
    }

    @Test
    fun testWithEmptyName() = withTestFramework {
        val suite1 by testSuite("suite1") {
            test("") {
            }
        }

        assertFails {
            withTestReport(suite1) {}
        }.also {
            assertContains(it.cause?.message ?: "", "with an empty or blank name ''")
        }
    }

    @Test
    fun testWithBlankName() = withTestFramework {
        val suite1 by testSuite("suite1") {
            test(" ") {
            }
        }

        assertFails {
            withTestReport(suite1) {}
        }.also {
            assertContains(it.cause?.message ?: "", "with an empty or blank name ' '")
        }
    }

    @Test
    fun testWithEmptyDisplayName() = withTestFramework {
        val suite1 by testSuite("suite1") {
            test("test1", displayName = "") {
            }
        }

        assertFails {
            withTestReport(suite1) {}
        }.also {
            assertContains(it.cause?.message ?: "", "with an empty or blank displayName ''")
        }
    }

    @Test
    fun testWithBlankDisplayName() = withTestFramework {
        val suite1 by testSuite("suite1") {
            test("test1", displayName = " ") {
            }
        }

        assertFails {
            withTestReport(suite1) {}
        }.also {
            assertContains(it.cause?.message ?: "", "with an empty or blank displayName ' '")
        }
    }

    @Test
    fun aroundEach() = withTestFramework {
        val trace = ConcurrentList<String>()

        val suite1 by testSuite(
            "suite1",
            testConfig = TestConfig
                .aroundEach { elementAction ->
                    trace.add("$testElementPath aroundEach1.1 begin")
                    elementAction()
                    trace.add("$testElementPath aroundEach1.1 end")
                }.aroundEachTest { elementAction ->
                    trace.add("$testElementPath aroundEachTest begin")
                    elementAction()
                    trace.add("$testElementPath aroundEachTest end")
                }.aroundEach { elementAction ->
                    trace.add("$testElementPath aroundEach1.2 begin")
                    elementAction()
                    trace.add("$testElementPath aroundEach1.2 end")
                }
        ) {
            test("test1") {
                trace.add("$testElementPath")
            }

            testSuite(
                "innerSuite",
                testConfig = TestConfig.aroundEach { elementAction ->
                    trace.add("$testElementPath aroundEach2 begin")
                    elementAction()
                    trace.add("$testElementPath aroundEach2 end")
                }
            ) {
                test("test1") {
                    trace.add("$testElementPath")
                }
            }
        }

        withTestReport(suite1) {
            assertContentEquals(
                listOf(
                    "«suite1» aroundEach1.1 begin",
                    "«suite1» aroundEach1.2 begin",
                    "«suite1${iSep}test1» aroundEach1.1 begin",
                    "«suite1${iSep}test1» aroundEachTest begin",
                    "«suite1${iSep}test1» aroundEach1.2 begin",
                    "«suite1${iSep}test1»",
                    "«suite1${iSep}test1» aroundEach1.2 end",
                    "«suite1${iSep}test1» aroundEachTest end",
                    "«suite1${iSep}test1» aroundEach1.1 end",
                    "«suite1${iSep}innerSuite» aroundEach1.1 begin",
                    "«suite1${iSep}innerSuite» aroundEach1.2 begin",
                    "«suite1${iSep}innerSuite» aroundEach2 begin",
                    "«suite1${iSep}innerSuite${iSep}test1» aroundEach1.1 begin",
                    "«suite1${iSep}innerSuite${iSep}test1» aroundEachTest begin",
                    "«suite1${iSep}innerSuite${iSep}test1» aroundEach1.2 begin",
                    "«suite1${iSep}innerSuite${iSep}test1» aroundEach2 begin",
                    "«suite1${iSep}innerSuite${iSep}test1»",
                    "«suite1${iSep}innerSuite${iSep}test1» aroundEach2 end",
                    "«suite1${iSep}innerSuite${iSep}test1» aroundEach1.2 end",
                    "«suite1${iSep}innerSuite${iSep}test1» aroundEachTest end",
                    "«suite1${iSep}innerSuite${iSep}test1» aroundEach1.1 end",
                    "«suite1${iSep}innerSuite» aroundEach2 end",
                    "«suite1${iSep}innerSuite» aroundEach1.2 end",
                    "«suite1${iSep}innerSuite» aroundEach1.1 end",
                    "«suite1» aroundEach1.2 end",
                    "«suite1» aroundEach1.1 end"
                ),
                trace.elements()
            )
        }
    }

    @Test
    fun failFast() = withTestFramework {
        val suite1 by testSuite("suite1", testConfig = TestConfig.failFast(3)) {
            for (testId in 1..15) {
                test("test$testId") {
                    if (testId.mod(2) == 0) {
                        fail("expect failure")
                    }
                }
            }
        }

        // Note that since this test does not interact with the Kotlin/JS test infrastructure, it tests
        // premature completion in a JVM-like fashion (stopping to run tests after "fail fast" detection)
        // on all platforms.
        withTestReport(suite1, expectFrameworkFailure = true) { frameworkFailure ->
            assertEquals(4, (frameworkFailure as? FailFastException)?.failureCount)
            with(finishedEvents()) {
                val failedTests = filter { it.failed && it.element is de.infix.testBalloon.framework.core.Test }
                assertEquals(5, failedTests.size) // 4 test failures plus one FailFastException
            }
        }
    }

    @Test
    fun suiteLevelFixture() = withTestFramework {
        val trace = ConcurrentList<String>()

        val invocationSuite by testSuite("invocationSuite") {
            val outerFixture =
                testFixture { trace.also { it.add("$testElementPath fixture creating") } } closeWith
                    { trace.add("$testElementPath fixture closing") }

            test("test1") {
                // The fixture must not be created here, as there is no invocation.
                trace.add("$testElementPath")
            }

            testSuite("innerSuite") {
                test("test1") {
                    outerFixture().add("$testElementPath")
                }
            }
        }

        val parameterSuite by testSuite("parameterSuite") {
            testFixture {
                trace.also { it.add("$testElementPath fixture creating") }
            } closeWith {
                trace.add("$testElementPath fixture closing")
            } asParameterForAll {
                test("test1") { _ ->
                    // The fixture must be created here, although its value is not used.
                    trace.add("$testElementPath")
                }

                testSuite("innerSuite") {
                    test("test1") { fixture ->
                        fixture.add("$testElementPath")
                    }
                }
            }
        }

        val contextSuite by testSuite("contextSuite") {
            testFixture {
                trace.also { it.add("$testElementPath fixture creating") }
            } closeWith {
                trace.add("$testElementPath fixture closing")
            } asContextForAll {
                test("test1") { executionScope ->
                    // The fixture must be created here, although its value is not used.
                    trace.add("${executionScope.testElementPath}")
                }

                testSuite("innerSuite") {
                    test("test1") { executionScope ->
                        trace.add("${executionScope.testElementPath}")
                    }
                }
            }
        }

        withTestReport(invocationSuite, parameterSuite, contextSuite) {
            assertContentEquals(
                listOf(
                    "«invocationSuite${iSep}test1»",
                    "«invocationSuite» fixture creating",
                    "«invocationSuite${iSep}innerSuite${iSep}test1»",
                    "«invocationSuite» fixture closing",
                    "«parameterSuite» fixture creating",
                    "«parameterSuite${iSep}test1»",
                    "«parameterSuite${iSep}innerSuite${iSep}test1»",
                    "«parameterSuite» fixture closing",
                    "«contextSuite» fixture creating",
                    "«contextSuite${iSep}test1»",
                    "«contextSuite${iSep}innerSuite${iSep}test1»",
                    "«contextSuite» fixture closing"
                ),
                trace.elements()
            )
        }
    }

    @Test
    fun testLevelFixture() = withTestFramework {
        val trace = ConcurrentList<String>()

        val parameterSuite by testSuite("parameterSuite") {
            testFixture {
                trace.also { it.add("$testElementPath fixture creating") }
            } closeWith {
                trace.add("$testElementPath fixture closing")
            } asParameterForEach {
                test("test1") { _ ->
                    // The fixture must be created here, although its value is not used.
                    trace.add("$testElementPath")
                }

                testSuite("innerSuite") {
                    test("test1") { fixture ->
                        fixture.add("$testElementPath")
                    }
                }
            }
        }

        val contextSuite by testSuite("contextSuite") {
            testFixture {
                trace.also { it.add("$testElementPath fixture creating") }
            } closeWith {
                trace.add("$testElementPath fixture closing")
            } asContextForEach {
                test("test1") { executionScope ->
                    // The fixture must be created here, although its value is not used.
                    trace.add("${executionScope.testElementPath}")
                }

                testSuite("innerSuite") {
                    test("test1") { executionScope ->
                        trace.add("${executionScope.testElementPath}")
                    }
                }
            }
        }

        withTestReport(parameterSuite, contextSuite) {
            assertContentEquals(
                listOf(
                    "«parameterSuite» fixture creating",
                    "«parameterSuite${iSep}test1»",
                    "«parameterSuite» fixture closing",
                    "«parameterSuite» fixture creating",
                    "«parameterSuite${iSep}innerSuite${iSep}test1»",
                    "«parameterSuite» fixture closing",
                    "«contextSuite» fixture creating",
                    "«contextSuite${iSep}test1»",
                    "«contextSuite» fixture closing",
                    "«contextSuite» fixture creating",
                    "«contextSuite${iSep}innerSuite${iSep}test1»",
                    "«contextSuite» fixture closing"
                ),
                trace.elements()
            )
        }
    }

    @Test
    fun testLevelFixtureNested() = withTestFramework {
        val trace = ConcurrentList<String>()

        val parameterSuite by testSuite("parameterSuite") {
            testFixture {
                1.also { trace.add("$testElementPath fixture '$it' creating") }
            } closeWith {
                trace.add("$testElementPath fixture '$this' closing")
            } asParameterForEach {
                test("test1") { fixture ->
                    trace.add("$testElementPath fixture '$fixture'")
                }

                testFixture {
                    2.also { trace.add("$testElementPath fixture '$it' creating") }
                } closeWith {
                    trace.add("$testElementPath fixture '$this' closing")
                } asParameterForEach {
                    test("test2") { fixture ->
                        trace.add("$testElementPath fixture '$fixture'")
                    }
                }

                test("test3") { fixture ->
                    trace.add("$testElementPath fixture '$fixture'")
                }
            }
        }

        val contextSuite by testSuite("contextSuite") {
            testFixture {
                1.also { trace.add("$testElementPath fixture '$it' creating") }
            } closeWith {
                trace.add("$testElementPath fixture '$this' closing")
            } asContextForEach {
                test("test1") { executionScope ->
                    // The fixture must be created here, although its value is not used.
                    trace.add("${executionScope.testElementPath} fixture '$this'")
                }

                testFixture {
                    2.also { trace.add("$testElementPath fixture '$it' creating") }
                } closeWith {
                    trace.add("$testElementPath fixture '$this' closing")
                } asContextForEach {
                    test("test2") { executionScope ->
                        trace.add("${executionScope.testElementPath} fixture '$this'")
                    }
                }

                test("test3") { executionScope ->
                    trace.add("${executionScope.testElementPath} fixture '$this'")
                }
            }
        }

        withTestReport(parameterSuite, contextSuite) {
            assertContentEquals(
                listOf(
                    "«parameterSuite» fixture '1' creating",
                    "«parameterSuite${iSep}test1» fixture '1'",
                    "«parameterSuite» fixture '1' closing",
                    "«parameterSuite» fixture '2' creating",
                    "«parameterSuite${iSep}test2» fixture '2'",
                    "«parameterSuite» fixture '2' closing",
                    "«parameterSuite» fixture '1' creating",
                    "«parameterSuite${iSep}test3» fixture '1'",
                    "«parameterSuite» fixture '1' closing",
                    "«contextSuite» fixture '1' creating",
                    "«contextSuite${iSep}test1» fixture '1'",
                    "«contextSuite» fixture '1' closing",
                    "«contextSuite» fixture '2' creating",
                    "«contextSuite${iSep}test2» fixture '2'",
                    "«contextSuite» fixture '2' closing",
                    "«contextSuite» fixture '1' creating",
                    "«contextSuite${iSep}test3» fixture '1'",
                    "«contextSuite» fixture '1' closing"
                ),
                trace.elements()
            )
        }
    }

    @Test
    fun testLevelToSuiteLevelFixture() = withTestFramework {
        val suite1 by testSuite("suite1") {
            val fixture1 = testFixture {
            } asParameterForEach {
                test("test1") { _ ->
                }
            }

            test("test2") {
                assertEquals(fixture1(), Unit)
            }
        }
        withTestReport(suite1) { frameworkFailure ->
            assertNull(frameworkFailure)
            with(finishedTestEvents()) {
                assertEquals(2, size)
                assertTrue(this[0].succeeded)
                assertTrue(this[1].failed)
                assertContains(this[1].throwable?.message!!, "reuse a test-level fixture as a suite-level fixture")
            }
        }
    }

    @Test
    fun suiteLevelToTestLevelFixture() = withTestFramework {
        val suite1 by testSuite("suite1") {
            testFixture {
            } asParameterForAll {
                test("test1") { _ ->
                }
            } asParameterForEach {
                test("test2") { _ ->
                }
            }
        }
        withTestReport(suite1) { frameworkFailure ->
            assertNull(frameworkFailure)
            with(finishedTestEvents()) {
                assertEquals(2, size)
                assertTrue(this[0].succeeded)
                assertTrue(this[1].failed)
                assertContains(this[1].throwable?.message!!, "reuse a suite-level fixture as a test-level fixture")
            }
        }
    }

    @Test
    fun suiteLevelFixtureWithAroundAll() = withTestFramework {
        val trace = ConcurrentList<String>()

        val suite1 by testSuite(
            "suite1",
            testConfig = TestConfig.aroundAll { tests ->
                trace.add("$testElementPath aroundAll begin")
                tests()
                trace.add("$testElementPath aroundAll end")
            }
        ) {
            val outerFixture =
                testFixture { trace.also { it.add("$testElementPath fixture creating") } } closeWith
                    { trace.add("$testElementPath fixture closing") }

            test("test1") {
                outerFixture().add("$testElementPath")
            }

            testSuite("innerSuite") {
                test("test1") {
                    outerFixture().add("$testElementPath")
                }
            }
        }

        withTestReport(suite1) {
            assertContentEquals(
                listOf(
                    "«suite1» aroundAll begin",
                    "«suite1» fixture creating",
                    "«suite1${iSep}test1»",
                    "«suite1${iSep}innerSuite${iSep}test1»",
                    "«suite1» fixture closing",
                    "«suite1» aroundAll end"
                ),
                trace.elements()
            )
        }
    }

    @Test
    fun suiteLevelFixtureWithDisabledElements() = withTestFramework {
        val trace = ConcurrentList<String>()

        val suite1 by testSuite("suite1") {
            val suite1Fixture =
                testFixture { trace.also { it.add("$testElementPath fixture creating") } } closeWith
                    { trace.add("$testElementPath fixture closing") }

            test("test1", testConfig = TestConfig.disable()) {
                suite1Fixture().add("$testElementPath")
            }
        }

        val suite2 by testSuite("suite2") {
            val suite2Fixture =
                testFixture { trace.also { it.add("$testElementPath fixture creating") } } closeWith
                    { trace.add("$testElementPath fixture closing") }

            test("test1", testConfig = TestConfig.disable()) {
                suite2Fixture().add("$testElementPath")
            }

            test("test2") {
                suite2Fixture().add("$testElementPath")
            }
        }

        val suite3 by testSuite("suite3") {
            val suite3Fixture =
                testFixture { trace.also { it.add("$testElementPath fixture creating") } } closeWith
                    { trace.add("$testElementPath fixture closing") }

            test("test1", testConfig = TestConfig.disable()) {
                suite3Fixture().add("$testElementPath")
            }

            testSuite("innerSuite") {
                test("test1", testConfig = TestConfig.disable()) {
                    suite3Fixture().add("$testElementPath")
                }
                test("test2") {
                    suite3Fixture().add("$testElementPath")
                }
            }
        }

        withTestReport(suite1, suite2, suite3) {
            assertContentEquals(
                listOf(
                    "«suite2» fixture creating",
                    "«suite2${iSep}test2»",
                    "«suite2» fixture closing",
                    "«suite3» fixture creating",
                    "«suite3${iSep}innerSuite${iSep}test2»",
                    "«suite3» fixture closing"
                ),
                trace.elements()
            )
        }
    }

    @Test
    fun suiteLevelFixtureWithFailedTest() = withTestFramework {
        val trace = ConcurrentList<String>()

        val suite1 by testSuite("suite1") {
            val fixture1 =
                testFixture { trace.also { it.add("$testElementPath fixture creating") } } closeWith
                    { trace.add("$testElementPath fixture closing") }

            test("test1") {
                fixture1().add("$testElementPath")
            }

            test("test2") {
                fixture1().add("$testElementPath")
                fail("intentionally")
            }
        }

        withTestReport(suite1) {
            with(finishedTestEvents()) {
                assertEquals(2, size)
                assertTrue(this[0].succeeded)
                assertTrue(this[1].failed)
            }
            assertContentEquals(
                listOf(
                    "«suite1» fixture creating",
                    "«suite1${iSep}test1»",
                    "«suite1${iSep}test2»",
                    "«suite1» fixture closing"
                ),
                trace.elements()
            )
        }
    }

    @Test
    fun suiteLevelFixtureActionFailure() = withTestFramework {
        var failCount = 0
        var closeCount = 0

        val suite1 by testSuite("suite1") {
            val fixture1 =
                testFixture { fail("fixture failing intentionally (${++failCount})") } closeWith { closeCount++ }

            test("test1") {
                fixture1()
            }

            test("test2") {
                fixture1()
            }
        }

        withTestReport(suite1) {
            with(finishedTestEvents()) {
                assertEquals(2, size)
                forEach { event ->
                    assertTrue(event.failed)
                    event.throwable?.assertMessageStartsWith("fixture failing intentionally")
                }
                assertEquals(2, failCount)
                assertEquals(0, closeCount)
            }
        }
    }

    @Test
    fun suiteLevelFixtureWithSetupFailures() = withTestFramework {
        val trace = ConcurrentList<String>()

        val suite1 by testSuite("suite1") {
            val fixtures = listOf(
                testFixture { trace.add("$testElementPath fixture1 creating") } closeWith
                    { trace.add("$testElementPath fixture1 closing") },
                testFixture { trace.add("$testElementPath fixture2 creating") } closeWith {
                    trace.add("$testElementPath fixture2 failing intentionally on close")
                    fail("$testElementPath fixture2 failing intentionally on close")
                },
                testFixture { trace.add("$testElementPath fixture3 creating") } closeWith {
                    trace.add("$testElementPath fixture3 failing intentionally on close")
                    fail("$testElementPath fixture3 failing intentionally on close")
                }
            )

            suspend fun Test.ExecutionScope.traceWithFixtureAccess() {
                trace.add("$testElementPath begin")
                fixtures.forEach { it() }
                trace.add("$testElementPath end")
            }

            test("test1") {
                traceWithFixtureAccess()
            }

            testSuite(
                "inner",
                testConfig = TestConfig.aroundAll {
                    trace.add("aroundAll $testElementPath failing intentionally")
                    fail("aroundAll $testElementPath failing intentionally")
                }
            ) {
                test("test2") {
                    traceWithFixtureAccess()
                }
            }

            test("test2") {
                traceWithFixtureAccess()
            }
        }

        withTestReport(suite1) {
            assertContentEquals(
                listOf(
                    "«suite1${iSep}test1» begin",
                    "«suite1» fixture1 creating",
                    "«suite1» fixture2 creating",
                    "«suite1» fixture3 creating",
                    "«suite1${iSep}test1» end",
                    "aroundAll «suite1${iSep}inner» failing intentionally",
                    "«suite1${iSep}test2» begin",
                    "«suite1${iSep}test2» end",
                    "«suite1» fixture3 failing intentionally on close",
                    "«suite1» fixture2 failing intentionally on close",
                    "«suite1» fixture1 closing"
                ),
                trace.elements()
            )

            with(finishedEvents()) {
                val failures = mapNotNull { it.throwable }
                assertContentEquals(
                    listOf(
                        "aroundAll «suite1${iSep}inner» failing intentionally",
                        "«suite1» fixture3 failing intentionally on close"
                    ),
                    failures.map {
                        it.message
                    }
                )

                val fixtureClosingFailureStackTrace = failures.last().stackTraceToString()
                if (!fixtureClosingFailureStackTrace.contains(
                        Regex("""Suppressed: \S+: «suite1» fixture2 failing intentionally on close""")
                    )
                ) {
                    fail(
                        "fixture closing failure missing suppressed exception:\n" +
                            fixtureClosingFailureStackTrace.prependIndent("\t")
                    )
                }
            }
        }
    }

    @Test
    fun suiteLevelFixtureConcurrency() = assertSuccessfulSuite {
        val instanceCount = atomic(0)

        testSuite("suite1", testConfig = TestConfig.invocation(TestConfig.Invocation.Concurrent)) {
            val fixture1 = testFixture { instanceCount.incrementAndGet() }

            repeat(100) { index ->
                test("test$index") {
                    assertEquals(fixture1(), 1)
                }
            }
        }
    }

    @Test
    fun disabled() = assertSuccessfulSuite {
        test("test1", testConfig = TestConfig.disable()) {
            fail("test $testElementPath should be disabled")
        }

        test("test2") {
        }

        testSuite("middleSuite", testConfig = TestConfig.disable()) {
            test("test1") {
                fail("test $testElementPath should be disabled")
            }

            testSuite("innerSuite1") {
                test("test1") {
                    fail("test $testElementPath should be disabled")
                }
            }
        }
    }

    @Test
    fun uniqueElementPaths() = withTestFramework {
        val suite1 by testSuite("suite1") {
            test("test1") {
            }

            test("test1") { // same element path!
            }
        }

        withTestReport(suite1) {
            with(finishedTestEvents()) {
                assertEquals(
                    size,
                    map { it.element.testElementPath }.toSet().size,
                    "All test elements must have a unique element path"
                )
            }
        }
    }

    @Test
    fun displayNameTopLevel() = withTestFramework {
        val suite1 by testSuite("suite1", displayName = "top-level suite #1") {
            test("test1") {
            }
        }

        withTestReport(suite1) {
            verifyDisplayNames(listOf("suite1${iSep}test1", "suite1/top-level suite #1"))
        }
    }

    @Test
    fun displayNameInnerSuite() = withTestFramework {
        val suite1 by testSuite("suite1") {
            test("test1") {
            }

            testSuite("innerSuite1", displayName = "inner suite #1") {
                test("test1") {
                }
            }
        }

        withTestReport(suite1) {
            verifyDisplayNames(
                listOf(
                    "suite1${iSep}test1",
                    "suite1${iSep}innerSuite1${iSep}test1",
                    "suite1${iSep}innerSuite1/inner suite #1",
                    "suite1"
                )
            )
        }
    }

    private fun InMemoryTestExecutionReport.verifyDisplayNames(expectation: List<String>) {
        assertContentEquals(
            expectation,
            finishedEvents().map {
                with(it.element) {
                    if (testElementDisplayName != testElementName) {
                        "${testElementPath.internalId}/$testElementDisplayName"
                    } else {
                        testElementPath.internalId
                    }
                }
            }.takeWhile { it.startsWith("suite1") } // ignore compartment and session
        )
    }

    @Test
    fun additionalReports() = withTestFramework {
        val eventLog = mutableListOf<String>()

        class AdditionalExecutionReport(val name: String) : TestExecutionReport() {
            override suspend fun add(event: TestElement.Event) {
                eventLog.add("$name: $event${if (!event.element.testElementIsEnabled) " [*]" else ""}")
            }
        }

        class IntentionalFailure : Error("intentional failure") {
            override fun toString(): String = "IntentionalFailure()"
        }

        val additionalReportA = AdditionalExecutionReport("A")
        val additionalReportB = AdditionalExecutionReport("B")

        val suite1 by testSuite("suite1", testConfig = TestConfig.executionReport(additionalReportA)) {
            test("test1", testConfig = TestConfig.disable()) {
            }

            test("test2") {
            }

            testSuite("middleSuite", testConfig = TestConfig.executionReport(additionalReportB)) {
                test("test1") {
                    throw IntentionalFailure()
                }

                testSuite("innerSuite1", testConfig = TestConfig.disable()) {
                    test("test1") {
                    }
                }
            }
        }

        withTestReport(suite1) {
            // [*] means: disabled test element, will be reported without actual execution
            assertContentEquals(
                listOf(
                    "A: TestSuite(«suite1»): Starting",
                    "A: Test(«suite1${iSep}test1»): Starting [*]",
                    "A: Test(«suite1${iSep}test1»): Finished – throwable=null [*]",
                    "A: Test(«suite1${iSep}test2»): Starting",
                    "A: Test(«suite1${iSep}test2»): Finished – throwable=null",
                    "A: TestSuite(«suite1${iSep}middleSuite»): Starting",
                    "B: TestSuite(«suite1${iSep}middleSuite»): Starting",
                    "A: Test(«suite1${iSep}middleSuite${iSep}test1»): Starting",
                    "B: Test(«suite1${iSep}middleSuite${iSep}test1»): Starting",
                    "B: Test(«suite1${iSep}middleSuite${iSep}test1»): Finished – throwable=IntentionalFailure()",
                    "A: Test(«suite1${iSep}middleSuite${iSep}test1»): Finished – throwable=IntentionalFailure()",
                    "A: TestSuite(«suite1${iSep}middleSuite${iSep}innerSuite1»): Starting [*]",
                    "B: TestSuite(«suite1${iSep}middleSuite${iSep}innerSuite1»): Starting [*]",
                    "A: Test(«suite1${iSep}middleSuite${iSep}innerSuite1${iSep}test1»): Starting [*]",
                    "B: Test(«suite1${iSep}middleSuite${iSep}innerSuite1${iSep}test1»): Starting [*]",
                    "B: Test(«suite1${iSep}middleSuite${iSep}innerSuite1${iSep}test1»): Finished – throwable=null [*]",
                    "A: Test(«suite1${iSep}middleSuite${iSep}innerSuite1${iSep}test1»): Finished – throwable=null [*]",
                    "B: TestSuite(«suite1${iSep}middleSuite${iSep}innerSuite1»): Finished – throwable=null [*]",
                    "A: TestSuite(«suite1${iSep}middleSuite${iSep}innerSuite1»): Finished – throwable=null [*]",
                    "B: TestSuite(«suite1${iSep}middleSuite»): Finished – throwable=null",
                    "A: TestSuite(«suite1${iSep}middleSuite»): Finished – throwable=null",
                    "A: TestSuite(«suite1»): Finished – throwable=null"
                ),
                eventLog
            )
        }
    }
}
