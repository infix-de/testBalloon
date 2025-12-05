package de.infix.testBalloon.framework.core

import de.infix.testBalloon.framework.core.internal.TestSetupReport
import de.infix.testBalloon.framework.core.internal.runTestAwaitingCompletion
import de.infix.testBalloon.framework.shared.AbstractTest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlin.time.Duration

/**
 * A test containing a test [action] which raises assertion errors on failure.
 *
 * The test action runs in a [TestExecutionScope] and may suspend.
 */
public class Test internal constructor(
    parent: TestSuite,
    name: String,
    displayName: String = name,
    testConfig: TestConfig,
    private val action: suspend TestExecutionScope.() -> Unit
) : TestElement(parent, name = name, displayName = displayName, testConfig = testConfig),
    AbstractTest {

    override fun setUp(selection: Selection, report: TestSetupReport) {
        setUpReporting(report) {
            super.setUp(selection, report)

            isIncluded = selection.includes(this)
        }
    }

    override suspend fun execute(report: TestExecutionReport) {
        if (!isIncluded) return

        executeReporting(report) {
            if (testElementIsEnabled) {
                @Suppress("DEPRECATION")
                testConfig.executeWrapped(this) {
                    val testScopeContext = TestScopeContext.current()

                    if (testScopeContext != null) {
                        executeInTestScope(testScopeContext)
                    } else {
                        coroutineScope {
                            TestExecutionScope(this@Test, this).action()
                        }
                    }
                }
            }
        }
    }

    /**
     * Executes the test action in [kotlinx.coroutines.test.TestScope].
     */
    private suspend fun Test.executeInTestScope(testScopeContext: TestScopeContext) {
        var inheritableContext = currentCoroutineContext().minusKey(Job)
        if (inheritableContext[CoroutineDispatcher] !is TestDispatcher) {
            inheritableContext = inheritableContext.minusKey(CoroutineDispatcher)
        }
        TestScope(inheritableContext)
            .runTestAwaitingCompletion(timeout = testScopeContext.timeout) TestScope@{
                TestExecutionScope(
                    this@Test,
                    CoroutineScope(currentCoroutineContext()),
                    this@TestScope,
                    testScopeContext.timeout
                ).action()
            }
    }
}

/**
 * A coroutine scope in which a single [Test] action executes.
 */
public class TestExecutionScope internal constructor(
    internal val test: Test,
    scope: CoroutineScope,
    private val testScopeOrNull: TestScope? = null,
    testTimeout: Duration? = null
) : AbstractTest by test,
    CoroutineScope by scope {

    /** The [kotlinx.coroutines.test.TestScope], which must have been enabled by [TestConfig.testScope]. */
    public val testScope: TestScope
        get() = testScopeOrNull
            ?: throw IllegalStateException("$test is not executing in a TestScope.")

    /** The test timeout if set by [TestConfig.testScope], or null. */
    @Suppress("CanBePrimaryConstructorProperty")
    @TestBalloonExperimentalApi
    public val testTimeout: Duration? = testTimeout
}
