package de.infix.testBalloon.framework.core

import de.infix.testBalloon.framework.core.internal.runTestAwaitingCompletion
import de.infix.testBalloon.framework.shared.AbstractTest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope

/**
 * A test containing a test [action] which raises assertion errors on failure. The [action] may suspend.
 */
public class Test internal constructor(
    parent: TestSuite,
    name: String,
    displayName: String = name,
    testConfig: TestConfig,
    private val action: suspend TestExecutionScope.() -> Unit
) : TestElement(parent, name = name, displayName = displayName, testConfig = testConfig),
    AbstractTest {

    override fun parameterize(selection: Selection, report: TestConfigurationReport) {
        configureReporting(report) {
            super.parameterize(selection, report)

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
                            TestExecutionScope(this@Test, this, null).action()
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
                    this@TestScope
                ).action()
            }
    }
}

public class TestExecutionScope internal constructor(
    internal val test: Test,
    scope: CoroutineScope,
    private val testScopeOrNull: TestScope?
) : AbstractTest by test,
    CoroutineScope by scope {

    public val testScope: TestScope
        get() = testScopeOrNull
            ?: throw IllegalStateException("$test is not executing in a TestScope.")
}
