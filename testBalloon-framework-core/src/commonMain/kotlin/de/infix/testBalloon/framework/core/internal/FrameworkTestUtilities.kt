package de.infix.testBalloon.framework.core.internal

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestElement
import de.infix.testBalloon.framework.core.TestExecutionReport
import de.infix.testBalloon.framework.core.TestPlatform
import de.infix.testBalloon.framework.core.TestSession
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.internal.integration.ThrowingTestSetupReport
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.AbstractTestSession
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalTestingApi
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.minutes

/**
 * Utilities for testing scenarios requiring a framework initialization, intended for use with kotlin.test only.
 *
 * These functions are provided for testing TestBalloon add-ons, which would otherwise not have access to
 * the framework's setup. As the framework tolerates only one TestSession at any time, these functions cannot
 * be used inside TestBalloon tests. They must be used with kotlin.test or any other non-TestBalloon framework.
 */
@TestBalloonInternalTestingApi
public object FrameworkTestUtilities {
    /**
     * Resets the framework's global state.
     *
     * This must be used before any JUnit*-based tests if they are used with TestBalloon tests in the same module.
     */
    public fun resetTestFramework() {
        TestFramework.resetState()
    }

    /** Sets up a test suite with [content] and asserts its successful execution. */
    public fun assertSuccessfulSuite(
        testSession: AbstractTestSession? = null,
        testConfig: TestConfig = TestConfig.Companion,
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
    public fun withTestFramework(testSession: AbstractTestSession? = null, action: suspend () -> Unit): TestResult {
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
                // WORKAROUND: `join()` and `await()` will hang on Wasm/WASI if a `Test` is running on the
                //     test dispatcher.
                logInfo { "WORKAROUND: Skip waiting for primary coroutine on ${testPlatform.displayName}." }
            } else {
                // Why `await()` if all we get is a `Unit`?
                // `join()` would ignore a possible exception in its job, while `await()` will re-throw it.
                deferredJob.await()
            }
        }
    }

    /**
     * Configures the test session with top-level [suites], executes it, lets [action] examine the resulting report.
     */
    public suspend fun withTestReport(
        vararg suites: TestSuite,
        expectFrameworkFailure: Boolean = false,
        invokeSetup: Boolean = true,
        action: suspend InMemoryTestExecutionReport.(frameworkFailure: Throwable?) -> Unit
    ) {
        withTestReport(
            suites = suites,
            selection = TestElement.AllInSelection,
            expectFrameworkFailure = expectFrameworkFailure,
            invokeSetup = invokeSetup,
            action = action
        )
    }

    /**
     * Configures the test session with top-level [suites], executes it, lets [action] examine the resulting report.
     */
    internal suspend fun withTestReport(
        vararg suites: TestSuite,
        selection: TestElement.Selection,
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
    @TestBalloonInternalTestingApi
    public class InMemoryTestExecutionReport : TestExecutionReport() {
        private val allEvents = ConcurrentList<TestElement.Event>()

        public fun allEvents(): List<TestElement.Event> = allEvents.elements()

        public fun finishedEvents(): List<TestElement.Event.Finished> = allEvents().mapNotNull {
            it as? TestElement.Event.Finished
        }

        public fun allTestEvents(): List<TestElement.Event> = allEvents().filter { it.element is Test }

        public fun finishedTestEvents(): List<TestElement.Event.Finished> = allTestEvents().mapNotNull {
            it as? TestElement.Event.Finished
        }

        override suspend fun add(event: TestElement.Event) {
            // println("REPORT: $event")
            check(allEvents.add(event))
        }

        public fun List<TestElement.Event.Finished>.assertAllSucceeded() {
            if (!all { it.succeeded }) {
                throw AssertionError(
                    mapNotNull {
                        if (!it.succeeded) "Test '${it.element.testElementName}' failed with ${it.throwable}" else null
                    }.joinToString(separator = ",\n\t")
                )
            }
        }
    }

    @TestBalloonInternalTestingApi
    public class ConcurrentList<Element> : SynchronizedObject() {
        private val elements = mutableListOf<Element>()

        public fun add(element: Element): Boolean = synchronized(this) { elements.add(element) }
        public fun elements(): List<Element> = synchronized(this) { elements.toList() }
    }
}
