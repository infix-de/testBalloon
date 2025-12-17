package de.infix.testBalloon.framework.core

import de.infix.testBalloon.framework.core.TestFixture.TestEnvelopeContext
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
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A test containing a test [action] which raises assertion errors on failure.
 *
 * The test action runs in a [Test.ExecutionScope] and may suspend.
 */
public class Test internal constructor(
    parent: TestSuite,
    name: String,
    displayName: String = name,
    testConfig: TestConfig,
    private val action: suspend ExecutionScope.() -> Unit
) : TestElement(parent, name = name, displayName = displayName, testConfig = testConfig),
    AbstractTest {

    /**
     * The scope in which a single [Test] action executes.
     */
    public class ExecutionScope internal constructor(
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
                    val blockingEnvelope =
                        TestEnvelopeContext.current()?.envelopeValue() as? TestFixture.BlockingEnvelope
                    val testScopeContext = TestScopeContext.current()

                    if (blockingEnvelope != null) {
                        val inheritableContext = inheritableContext()
                        blockingEnvelope.execute(test = this@Test) {
                            runTest(inheritableContext, timeout = testScopeContext?.timeout ?: 60.seconds) {
                                ExecutionScope(this@Test, this).action()
                            }
                        }
                    } else {
                        if (testScopeContext != null) {
                            executeInTestScope(testScopeContext)
                        } else {
                            coroutineScope {
                                ExecutionScope(this@Test, this).action()
                            }
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
        TestScope(inheritableContext())
            .runTestAwaitingCompletion(timeout = testScopeContext.timeout) TestScope@{
                ExecutionScope(
                    this@Test,
                    CoroutineScope(currentCoroutineContext()),
                    this@TestScope,
                    testScopeContext.timeout
                ).action()
            }
    }

    private suspend fun inheritableContext(): CoroutineContext = currentCoroutineContext().minusKey(Job).let {
        if (it[CoroutineDispatcher] is TestDispatcher) {
            it
        } else {
            it.minusKey(CoroutineDispatcher)
        }
    }
}
