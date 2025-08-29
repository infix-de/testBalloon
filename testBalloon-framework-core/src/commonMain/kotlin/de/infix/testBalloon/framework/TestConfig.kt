package de.infix.testBalloon.framework

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A test element's configuration, modifying the execution of tests and suites.
 *
 * A test element can be configured via any number of chained [TestConfig]s.
 *
 * When [TestConfig]s are combined into a chain,
 * - a later conflicting [TestConfig] takes precedence over an earlier one,
 * - non-conflicting coroutine context elements accumulate,
 * - a series of [aroundAll], [aroundEach] wrappers, or [traversal]s nest from the outside to the inside in order
 *   of appearance.
 *
 * Each [TestConfig] operates at the [TestElement] level where it is configured. However, child elements inherit
 * some configuration effects as described in the respective [TestConfig] function.
 *
 * Use an existing configuration or the empty [TestConfig] object as the starting point, then invoke the [TestConfig]
 * functions to return chained configurations.
 *
 * Example:
 * ```
 * testConfig = TestConfig
 *     .coroutineContext(UnconfinedTestDispatcher())
 *     .invocation(TestInvocation.CONCURRENT)
 * ```
 */
public open class TestConfig internal constructor(
    private val parameterizingAction: ParameterizingAction?,
    private val executionWrappingAction: TestElementExecutionWrappingAction?,
    private val executionReportSetupAction: ExecutionReportSetupAction?
) {
    /** Returns a [TestConfig] which combines `this` configuration with a parameterizing action. */
    internal fun parameterizing(nextParameterizingAction: ParameterizingAction): TestConfig = TestConfig(
        parameterizingAction = if (parameterizingAction != null) {
            {
                parameterizingAction()
                nextParameterizingAction()
            }
        } else {
            nextParameterizingAction
        },
        executionWrappingAction = executionWrappingAction,
        executionReportSetupAction = executionReportSetupAction
    )

    /** Returns a [TestConfig] which combines `this` configuration with an execution-wrapping action. */
    internal fun executionWrapping(innerExecutionWrappingAction: TestElementExecutionWrappingAction): TestConfig =
        TestConfig(
            parameterizingAction = parameterizingAction,
            executionWrappingAction = if (executionWrappingAction != null) {
                { elementAction ->
                    executionWrappingAction {
                        innerExecutionWrappingAction(elementAction)
                    }
                }
            } else {
                innerExecutionWrappingAction
            },
            executionReportSetupAction = executionReportSetupAction
        )

    /** Returns a [TestConfig] which combines `this` configuration with a report setup action. */
    internal fun reportSetup(nextExecutionReportSetupAction: ExecutionReportSetupAction): TestConfig = TestConfig(
        parameterizingAction = parameterizingAction,
        executionWrappingAction = executionWrappingAction,
        executionReportSetupAction = if (executionReportSetupAction != null) {
            { elementAction ->
                executionReportSetupAction(elementAction)
                nextExecutionReportSetupAction(elementAction)
            }
        } else {
            nextExecutionReportSetupAction
        }
    )

    /** Returns a [TestConfig] which chains `this` configuration with [otherConfiguration]. */
    public fun chainedWith(otherConfiguration: TestConfig): TestConfig {
        var result = this

        otherConfiguration.parameterizingAction?.let { result = result.parameterizing(it) }
        otherConfiguration.executionWrappingAction?.let { result = result.executionWrapping(it) }
        otherConfiguration.executionReportSetupAction?.let { result = result.reportSetup(it) }

        return result
    }

    /** Parameterizes [testElement] according to `this` configuration. */
    internal open fun parameterize(testElement: TestElement) {
        if (parameterizingAction != null) testElement.parameterizingAction()
    }

    /** Wraps the execution according to `this` configuration, then executes [elementAction] on [testElement]. */
    internal suspend fun <SpecificTestElement : TestElement> executeWrapped(
        testElement: SpecificTestElement,
        elementAction: suspend SpecificTestElement.() -> Unit
    ) {
        val executionTraversalContext = ExecutionTraversalContext.current()
        if (executionTraversalContext != null) {
            executionTraversalContext.executeInside(testElement) {
                executionWrappingAction.wrapIfNotNull(testElement, elementAction)
            }
        } else {
            executionWrappingAction.wrapIfNotNull(testElement, elementAction)
        }
    }

    internal suspend fun withExecutionReportSetup(
        testElement: TestElement,
        executionReportingAction: suspend (additionalReports: Iterable<TestExecutionReport>?) -> Unit
    ) {
        if (executionReportSetupAction != null) {
            executionReportSetupAction(testElement) {
                executionReportingAction(ReportContext.additionalReports())
            }
        } else {
            executionReportingAction(ReportContext.additionalReports())
        }
    }

    /** The initial (empty) test configuration. */
    public companion object : TestConfig(null, null, null)
}

private typealias ParameterizingAction = TestElement.() -> Unit
private typealias ExecutionReportSetupAction =
    suspend TestElement.(elementAction: suspend TestElement.() -> Unit) -> Unit

/**
 * An action wrapping a [TestElement]'s execution.
 *
 * `elementAction` can be the element's primary action, or a cumulative action, which includes wrapping actions,
 * plus the elements primary action.
 *
 * Requirements:
 * - An [TestElementExecutionWrappingAction] must invoke `elementAction` exactly once.
 *
 * Requirements for [TestElement]s of type [Test]:
 * - If `elementAction` throws, it is considered a test failure. If [TestElementExecutionWrappingAction] catches the exception,
 *   it should re-throw, or the test failure will be muted.
 * - [TestElementExecutionWrappingAction] may throw an exception on its own initiative, which will be considered a test
 *   failure.
 *
 * Requirements for [TestElement]s of types other than [Test]:
 * - If `elementAction` throws, it is considered a failure of the test framework.
 * - [TestElementExecutionWrappingAction] should not throw to indicate a failing test or to block further tests from
 *   executing.
 */
public typealias TestElementExecutionWrappingAction = suspend TestElement.(
    elementAction: suspend TestElement.() -> Unit
) -> Unit

/**
 * Returns a test configuration which disables test execution for a [TestElement] tree.
 *
 * It disables test execution for the [TestElement] it is configured for and all elements below it.
 */
public fun TestConfig.disable(): TestConfig = parameterizing { testElementIsEnabled = false }

/**
 * Returns a test configuration which wraps a coroutine [context] around the [TestElement]'s execution.
 *
 * Child elements inherit the [context].
 */
public fun TestConfig.coroutineContext(context: CoroutineContext): TestConfig = executionWrapping { elementAction ->
    withContext(context) {
        elementAction()
    }
}

/**
 * Returns a test configuration which wraps [executionWrappingAction] around a single test element.
 *
 * [executionWrappingAction] wraps around the [TestElement]'s cumulative action (a cumulative action includes
 * all wrapping actions following this one, and the elements primary action).
 * See [TestElementExecutionWrappingAction] for requirements.
 *
 * The [executionWrappingAction] is performed only on the [TestElement] it is configured for.
 * Child elements do not inherit it.
 *
 * Example:
 * ```
 * configuration = TestConfig.aroundAll { elementAction ->
 *     withTimeout(2.seconds) {
 *         elementAction()
 *     }
 * }
 * ```
 */
public fun TestConfig.aroundAll(executionWrappingAction: TestElementExecutionWrappingAction): TestConfig =
    executionWrapping(executionWrappingAction)

/**
 * Returns a test configuration which wraps [executionWrappingAction] around each element of a [TestElement] tree.
 *
 * [executionWrappingAction] operates on the [TestElement] it is configured for and all elements below it.
 * [executionWrappingAction] wraps around each [TestElement]'s cumulative action (a cumulative action includes
 * all wrapping actions following this one, and the elements primary action).
 * See [TestElementExecutionWrappingAction] for requirements.
 *
 * Multiple [aroundEach] invocations nest outside-in in the order of appearance.
 *
 * Note: If you need an [aroundEach] wrapper with a shared context, use [TestConfig.traversal].
 *
 * Example:
 * ```
 * testConfig = TestConfig.aroundEach { elementAction ->
 *     println("$elementPath aroundEach entering")
 *     elementAction()
 *     println("$elementPath aroundEach exiting")
 * }
 * ```
 */
public fun TestConfig.aroundEach(executionWrappingAction: TestElementExecutionWrappingAction): TestConfig =
    traversal(AroundEachTraversal(executionWrappingAction))

private class AroundEachTraversal(val executionWrappingAction: TestElementExecutionWrappingAction) :
    TestExecutionTraversal {
    override suspend fun aroundEach(testElement: TestElement, elementAction: suspend TestElement.() -> Unit) {
        testElement.executionWrappingAction {
            testElement.elementAction()
        }
    }
}

/**
 * Returns a test configuration which establishes a "fail fast" strategy on a [TestElement] tree.
 *
 * If more than [maxFailureCount] tests fail, any subsequent test will abandon further testing:
 * - On platforms supporting it, the test session stops, optionally marking all remaining tests as skipped.
 * - On platforms not supporting a premature shutdown, all remaining tests will fail with a [FailFastException],
 *   without being executed.
 *
 * The strategy covers the [TestElement] it is configured for and all elements below it.
 */
public fun TestConfig.failFast(maxFailureCount: Int): TestConfig = traversal(FailFastStrategy(maxFailureCount))

private class FailFastStrategy(val maxFailureCount: Int) : TestExecutionTraversal {
    private val testFailureCount = atomic(0)

    override suspend fun aroundEach(testElement: TestElement, elementAction: suspend TestElement.() -> Unit) {
        if (testFailureCount.value > maxFailureCount && testElement is Test) {
            throw FailFastException(testFailureCount.value)
        }
        try {
            testElement.elementAction()
        } catch (throwable: Throwable) {
            if (testElement is Test) {
                testFailureCount.incrementAndGet()
            }
            throw throwable
        }
    }
}

internal class FailFastException(val failureCount: Int) : Error("Failing fast after $failureCount failed tests")

/**
 * Returns a test configuration which applies a [TestExecutionTraversal] to each element of a [TestElement] tree.
 *
 * The traversal covers the [TestElement] it is configured for and all elements below it.
 * Multiple traversals nest outside-in in the order of appearance.
 *
 * Note: If you don't need a shared context, use [TestConfig.aroundEach].
 */
public fun TestConfig.traversal(executionTraversal: TestExecutionTraversal): TestConfig =
    executionWrapping { elementAction ->
        val testElement = this
        withContext(ExecutionTraversalContext(executionTraversal)) {
            // The context element enables the traversal for this element's children only. To cover this element as well,
            // we explicitly invoke its traversal action.
            executionTraversal.aroundEach(testElement, elementAction)
        }
    }

/**
 * A traversal following the execution across all [TestElement]s of a (partial) test element tree.
 *
 * For an example, see the implementation of [TestConfig.failFast].
 */
public interface TestExecutionTraversal {
    /**
     * A method wrapping each [TestElement]'s cumulative [elementAction].
     *
     * The cumulative [elementAction] includes all wrapping actions following this one, and the elements primary action.
     */
    public suspend fun aroundEach(testElement: TestElement, elementAction: suspend TestElement.() -> Unit)
}

private class ExecutionTraversalContext private constructor(
    /** [TestExecutionTraversal]s in an inside-out order of wrapping (innermost action first). */
    private val executionTraversals: List<TestExecutionTraversal>
) : AbstractCoroutineContextElement(Key) {

    /** Wraps the execution for this context's traversals, then executes [elementAction] on [testElement]. */
    suspend inline fun executeInside(testElement: TestElement, noinline elementAction: suspend TestElement.() -> Unit) {
        executionTraversals.fold(elementAction) { action, traversal ->
            { traversal.aroundEach(testElement, action) }
        }.invoke(testElement)
    }

    companion object {
        private val Key = object : CoroutineContext.Key<ExecutionTraversalContext> {}

        /** Returns a new [ExecutionTraversalContext], adding [executionTraversal] to the current ones. */
        suspend operator fun invoke(executionTraversal: TestExecutionTraversal): ExecutionTraversalContext {
            val current = current()
            return ExecutionTraversalContext(
                if (current != null) {
                    listOf(executionTraversal) + current.executionTraversals
                } else {
                    listOf(executionTraversal)
                }
            )
        }

        suspend fun current(): ExecutionTraversalContext? = currentCoroutineContext()[Key]
    }
}

private suspend inline fun <SpecificTestElement : TestElement> TestElementExecutionWrappingAction?.wrapIfNotNull(
    testElement: SpecificTestElement,
    crossinline elementAction: suspend SpecificTestElement.() -> Unit
) {
    if (this != null) {
        this(testElement) {
            testElement.elementAction()
        }
    } else {
        testElement.elementAction()
    }
}

/**
 * Returns a test configuration which specifies an invocation [mode] for all [TestSuite]s of a [TestElement] tree.
 *
 * The [mode] applies to the [TestSuite] it is configured for and all [TestSuite]s below it,
 * unless configured otherwise.
 */
public fun TestConfig.invocation(mode: TestInvocation): TestConfig = coroutineContext(InvocationContext(mode))

/**
 * The mode in which a [TestSuite] executes its child [TestElement]s.
 */
public enum class TestInvocation {
    /** Execute child [TestElement]s sequentially. */
    SEQUENTIAL,

    /** Execute child [TestElement]s concurrently. */
    CONCURRENT;

    internal companion object {
        internal suspend fun current(): TestInvocation =
            currentCoroutineContext()[InvocationContext.Key]?.mode ?: SEQUENTIAL
    }
}

private class InvocationContext(val mode: TestInvocation) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<InvocationContext>
}

/**
 * Returns a test configuration which wraps a single-threaded dispatcher around a [TestElement]'s execution.
 *
 * Child elements inherit the single-threaded dispatcher as part of their [CoroutineContext], unless they specify
 * another dispatcher.
 */
@TestBalloonExperimentalApi
public fun TestConfig.singleThreaded(): TestConfig = executionWrapping { elementAction ->
    withSingleThreadedDispatcher { dispatcher ->
        withContext(dispatcher) {
            elementAction()
        }
    }
}

/**
 * Returns a test configuration which specifies a main dispatcher ([Dispatchers.setMain]) for a [TestElement] tree.
 *
 * If [dispatcher] is `null`, a single-threaded dispatcher is used.
 *
 * Note: Only one main dispatcher may exist at any point in time. Therefore,
 * – this configuration may not be overridden at lower levels of the [TestElement] hierarchy, and
 * - multiple [TestElement] trees with a [mainDispatcher] configuration may not execute concurrently.
 * Child elements inherit the main dispatcher as part of their [CoroutineContext].
 */
@TestBalloonExperimentalApi
public fun TestConfig.mainDispatcher(dispatcher: CoroutineDispatcher? = null): TestConfig =
    executionWrapping { elementAction ->
        withMainDispatcher(dispatcher) {
            elementAction()
        }
    }

/**
 * Returns a test configuration which enables/disables a [kotlinx.coroutines.test.TestScope] for a [TestElement] tree.
 *
 * If [isEnabled] is true, [Test]s will run in a [kotlinx.coroutines.test.TestScope] with the given [timeout].
 * Setting [isEnabled] to false will disable a previously enabled `TestScope` setting.
 *
 * Child elements inherit this setting, unless configured otherwise.
 */
public fun TestConfig.testScope(isEnabled: Boolean, timeout: Duration = 60.seconds): TestConfig =
    coroutineContext(TestScopeContext(isEnabled, timeout))

/**
 * A context element, which, when present and enabled, makes a test execute in [kotlinx.coroutines.test.TestScope].
 */
internal class TestScopeContext(internal val isEnabled: Boolean, val timeout: Duration) :
    AbstractCoroutineContextElement(Key) {
    companion object {
        private val Key = object : CoroutineContext.Key<TestScopeContext> {}

        suspend fun current(): TestScopeContext? = currentCoroutineContext()[Key]?.run { if (isEnabled) this else null }
    }
}

/**
 * Runs [action] with [dispatcher] set up as the main dispatcher, which will be reset afterward.
 *
 * If [dispatcher] is `null`, a single-threaded dispatcher is used.
 *
 * See [Dispatchers.setMain] for details. This function, if used exclusively, ensures that only one main dispatcher
 * is active at any point in time.
 */
@TestBalloonExperimentalApi
public suspend fun withMainDispatcher(dispatcher: CoroutineDispatcher? = null, action: suspend () -> Unit) {
    val previouslyChanged = mainDispatcherChanged.getAndSet(true)
    require(!previouslyChanged) {
        "Another invocation of withMainDispatcher() is still active." +
            " Redirecting Dispatchers.Main again would introduce a conflict in its global state.\n" +
            "\tPlease avoid concurrent changes to Dispatchers.Main by executing tests" +
            " in isolation (e.g. in a separate UI test compartment)."
    }

    suspend fun withDispatcherOrSingleThreaded(action: suspend (mainDispatcher: CoroutineDispatcher) -> Unit) {
        if (dispatcher != null) {
            action(dispatcher)
        } else {
            withSingleThreadedDispatcher {
                action(it)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    withDispatcherOrSingleThreaded { mainDispatcher ->
        Dispatchers.setMain(mainDispatcher)
        try {
            action()
        } finally {
            Dispatchers.resetMain()
            check(mainDispatcherChanged.getAndSet(false))
        }
    }
}

private val mainDispatcherChanged = atomic(false)

/**
 * Returns a test configuration which adds a [TestExecutionReport] to a test element tree.
 *
 * The report covers the [TestElement] it is configured for and all elements below it. A [TestElement] can be
 * covered by multiple reports.
 */
public fun TestConfig.report(report: TestExecutionReport): TestConfig = reportSetup { elementAction ->
    val testElement = this
    withContext(ReportContext(report)) {
        elementAction(testElement)
    }
}

private class ReportContext private constructor(
    /** [TestExecutionReport]s in definition order. */
    val additionalReports: List<TestExecutionReport>
) : AbstractCoroutineContextElement(Key) {

    companion object {
        private val Key = object : CoroutineContext.Key<ReportContext> {}

        /** Returns a new [ReportContext], adding [additionalReport] to the current ones. */
        suspend operator fun invoke(additionalReport: TestExecutionReport): ReportContext {
            val additionalReports = additionalReports()
            return ReportContext(
                if (additionalReports != null) {
                    additionalReports + listOf(additionalReport)
                } else {
                    listOf(additionalReport)
                }
            )
        }

        suspend fun additionalReports(): Iterable<TestExecutionReport>? =
            currentCoroutineContext()[Key]?.additionalReports
    }
}
