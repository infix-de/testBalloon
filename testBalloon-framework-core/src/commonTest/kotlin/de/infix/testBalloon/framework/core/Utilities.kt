package de.infix.testBalloon.framework.core

import de.infix.testBalloon.framework.core.internal.TestFramework
import de.infix.testBalloon.framework.core.internal.initializeTestFramework
import de.infix.testBalloon.framework.core.internal.integration.ThrowingTestSetupReport
import de.infix.testBalloon.framework.core.internal.logInfo
import de.infix.testBalloon.framework.shared.AbstractTestSession
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.Duration.Companion.minutes

/** Sets up a test suite with [content] and asserts its successful execution. */
internal fun assertSuccessfulSuite(
    testSession: AbstractTestSession? = null,
    testConfig: TestConfig = TestConfig,
    content: TestSuite.() -> Unit
): TestResult = withTestFramework(testSession) {
    val suite by testSuite("suite", testConfig = testConfig) {
        content()
    }

    withTestReport(suite) {
        finishedTestEvents().assertAllSucceeded()
    }
}

/** Performs [action] in an initialized framework session. */
@OptIn(DelicateCoroutinesApi::class)
internal fun withTestFramework(testSession: AbstractTestSession? = null, action: suspend () -> Unit): TestResult {
    val deferredJob = GlobalScope.async(Dispatchers.Default) {
        initializeTestFramework(testSession)
        try {
            action()
            if (testPlatform.type == TestPlatform.Type.WasmWasi) {
                logInfo { "Primary coroutine on ${testPlatform.displayName} completed." }
            }
        } finally {
            // Reset global state for another round of test framework initialization.
            TestFramework.resetState()
        }
    }

    return TestScope().runTest(timeout = 2.minutes) {
        if (testPlatform.type == TestPlatform.Type.WasmWasi) {
            // WORKAROUND: `join()` and `await()` will hang on Wasm/WASI if a `Test` is running on the test dispatcher.
            logInfo { "WORKAROUND: Skip waiting for primary coroutine on ${testPlatform.displayName}." }
        } else {
            // Why `await()` if all we get is a `Unit`?
            // `join()` would ignore a possible exception in its job, while `await()` will re-throw it.
            deferredJob.await()
        }
    }
}

/**
 * Configures the framework session with top-level [suites], executes it, lets [action] examine the resulting report.
 */
internal suspend fun withTestReport(
    vararg suites: TestSuite,
    selection: TestElement.Selection = TestElement.AllInSelection,
    expectFrameworkFailure: Boolean = false,
    invokeSetup: Boolean = true,
    action: suspend InMemoryTestExecutionReport.(frameworkFailure: Throwable?) -> Unit
) {
    require(suites.isNotEmpty()) { "At least one suite must be provided" }

    if (invokeSetup) {
        TestSession.global.setUp(selection, ThrowingTestSetupReport())
    }

    val report = InMemoryTestExecutionReport()
    var frameworkFailure: Throwable? = null
    try {
        TestSession.global.execute(report)
    } catch (throwable: Throwable) {
        frameworkFailure = throwable
        if (!expectFrameworkFailure) {
            throw frameworkFailure
        }
    }
    report.action(frameworkFailure)
}

/**
 * An in-memory test report, collecting [TestElement.Event]s for later examination.
 */
internal class InMemoryTestExecutionReport : TestExecutionReport() {
    private val allEvents = ConcurrentList<TestElement.Event>()

    fun allEvents() = allEvents.elements()
    fun finishedEvents() = allEvents().mapNotNull { it as? TestElement.Event.Finished }
    fun allTestEvents() = allEvents().filter { it.element is Test }
    fun finishedTestEvents() = allTestEvents().mapNotNull { it as? TestElement.Event.Finished }

    override suspend fun add(event: TestElement.Event) {
        // println("REPORT: $event")
        allEvents.add(event)
    }

    fun List<TestElement.Event.Finished>.assertAllSucceeded() {
        if (!all { it.succeeded }) {
            fail(
                mapNotNull {
                    if (!it.succeeded) "Test '${it.element.testElementName}' failed with ${it.throwable}" else null
                }.joinToString(separator = ",\n\t")
            )
        }
    }

    fun List<TestElement.Event>.assertElementPathsContainInOrder(
        expectedPaths: List<String>,
        exhaustive: Boolean = false
    ) {
        map { it.element.testElementPath.internalId }.assertContainsInOrder(expectedPaths, exhaustive)
    }
}

internal fun List<String>.assertContainsInOrder(expectedElements: List<String>, exhaustive: Boolean = false) {
    if (exhaustive && expectedElements.size != size) {
        fail("Expected ${expectedElements.size} elements but got $size")
    }
    val firstExpectedElement = expectedElements.first()
    val actualElementsSlice = drop(indexOfFirst { it == firstExpectedElement }).take(expectedElements.size)
    assertContentEquals(expectedElements, actualElementsSlice)
}

internal fun Throwable.assertMessageStartsWith(phrase: String) =
    assertEquals(message?.startsWith(phrase), true, "Exception message did not start with '$phrase', but is '$message'")

/** References a [TestSuite], resolving the lazy initialization for top-level suites. */
internal fun TestSuite.reference() {
    require(toString().isNotEmpty())
}

internal class ConcurrentSet<Element> : SynchronizedObject() {
    private val elements = mutableSetOf<Element>()

    fun clear() = synchronized(this) { elements.clear() }
    fun add(element: Element) = synchronized(this) { elements.add(element) }
    fun elements() = synchronized(this) { elements.toSet() }
}

internal class ConcurrentList<Element> : SynchronizedObject() {
    private val elements = mutableListOf<Element>()

    fun add(element: Element) = synchronized(this) { elements.add(element) }
    fun elements() = synchronized(this) { elements.toList() }
}
