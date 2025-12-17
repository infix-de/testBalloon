package de.infix.testBalloon.framework.core

import de.infix.testBalloon.framework.core.internal.GuardedBy
import de.infix.testBalloon.framework.shared.TestDisplayName
import de.infix.testBalloon.framework.shared.TestElementName
import de.infix.testBalloon.framework.shared.TestRegistering
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmName

/**
 * A test fixture is a state holder for a lazily initialized [Value] to be used in tests.
 *
 * Fixtures come in two flavors:
 *
 * A **suite-level fixture** provides a value with a lifetime of the test suite it was registered in.
 * Its value is
 *
 * - obtained by [invoking][invoke] the fixture, or
 * - passed as a context (receiver) to tests in the scope of [asContextForAll], or
 * - passed as a parameter to tests in the scope of [asParameterForAll].
 *
 * All test elements within its suite share the same value from a suite-level fixture.
 *
 * A **test-level fixture** provides its value to each single test with a lifetime of that test. Its value is
 * passed
 *
 * - as a context (receiver) to tests in the scope of [asContextForEach], or
 * - as a parameter to tests in the scope of [asParameterForEach].
 *
 * Each test gets its own fresh value from a test-level fixture.
 *
 * Common characteristics:
 * - The fixture lazily initializes its value on first use via the [value] lambda.
 * - The [value] lambda can return any Kotlin object. The `object` expression can be used to return an anonymous
 *   composite value.
 * - The [value] lambda is suspending. To create additional coroutines inside, see
 *   [TestSuite.testSuiteCoroutineScope] for details.
 * - If [Value] is an [AutoCloseable], the fixture will call `close` at the end of its lifetime, otherwise
 *   [closeWith] can specify an action to be called on close.
 * - A fixture can be used either as a suite-level fixture, or as a test-level fixture, but not both.
 *
 * Usage:
 * - For a suite-level fixture, see [invoke], [asContextForAll] and [asParameterForAll].
 * - For a test-level fixture, see [asContextForEach] and [asParameterForEach].
 */
public class TestFixture<Value : Any> internal constructor(
    private val testSuite: TestSuite,
    value: suspend TestSuite.() -> Value
) {
    private val newValue = value

    internal enum class Level {
        Suite,
        Test
    }

    private var level: Level? = null

    // The following two properties are only relevant if this is a suite-level fixture.
    @GuardedBy("suiteLevelValueMutex")
    private var suiteLevelValue: Value? = null
    private val suiteLevelValueMutex = Mutex()

    private var close: suspend Value.() -> Unit = { (this as? AutoCloseable)?.close() }

    /**
     * Returns the value of `this` fixture, which is implied to be a suite-level fixture.
     *
     * The fixture's value will be instantiated on first use, then shared on subsequent invocations and across
     * tests. It has a lifetime of the suite it was registered in.
     *
     * Usage:
     *
     * Register a suite-level fixture like this:
     * ```
     * val repository = testFixture { MyRepository(this) } closeWith { disconnect() }
     * ```
     *
     * Use its value in the suite's tests by invoking the fixture like this:
     * ```
     * repository().getScore(...)
     * ```
     */
    public suspend operator fun invoke(): Value = suiteLevelValue()

    /**
     * Provides the value from `this` fixture as a context for all tests declared in the [content]'s scope.
     *
     * The fixture is implied to be a suite-level fixture. Its value will be instantiated on first use, then
     * shared across tests. It has a lifetime of the suite it was registered in.
     *
     * Note: As the context for tests in this scope is the fixture's value, the usual [Test.ExecutionScope] is
     * unavailable as a context. [Test.ExecutionScope] is, however, provided as a parameter to each test.
     *
     * Usage:
     *
     * ```
     * testFixture {
     *     MyRepository(this)
     * } asContextForAll {
     *     test("verify score") {
     *         getScore(...)          // call a method of MyRepository
     *     }
     * }
     * ```
     */
    public infix fun asContextForAll(
        content: Scope<suspend Value.(testExecutionScope: Test.ExecutionScope) -> Unit>.() -> Unit
    ): TestFixture<Value> {
        Scope<suspend Value.(testExecutionScope: Test.ExecutionScope) -> Unit>(
            testSuiteInScope = testSuite,
            envelopeValue = null,
            scopingAction = { testExecutionScope, fixtureScopedAction ->
                suiteLevelValue().fixtureScopedAction(testExecutionScope)
            }
        ).content()

        return this
    }

    /**
     * Provides the value from `this` fixture as a parameter for all tests declared in the [content]'s scope.
     *
     * The fixture is implied to be a suite-level fixture. Its value will be instantiated on first use, then
     * shared across tests. It has a lifetime of the suite it was registered in.
     *
     * Usage:
     *
     * ```
     * testFixture {
     *     MyRepository(this)
     * } asParameterForAll {
     *     test("verify score") { repository ->
     *         repository.getScore(...)
     *     }
     * }
     * ```
     */
    public infix fun asParameterForAll(
        content: Scope<suspend Test.ExecutionScope.(value: Value) -> Unit>.() -> Unit
    ): TestFixture<Value> {
        Scope<suspend Test.ExecutionScope.(value: Value) -> Unit>(
            testSuiteInScope = testSuite,
            envelopeValue = null,
            scopingAction = { testExecutionScope, fixtureScopedAction ->
                fixtureScopedAction(testExecutionScope, suiteLevelValue())
            }
        ).content()

        return this
    }

    /**
     * Provides a fresh value from `this` fixture as a context for each test declared in the [content]'s scope.
     *
     * Using this function implies a test-level fixture, whose value will be instantiated per test, and has
     * a lifetime of that test. This is safe for concurrent test invocation, because values are isolated.
     *
     * Note: As the context for tests in this scope is the fixture's value, the usual [Test.ExecutionScope] is
     * unavailable as a context. [Test.ExecutionScope] is, however, provided as a parameter to each test.
     *
     * Usage:
     *
     * ```
     * testFixture {
     *     object {
     *         var balance = 42.0
     *         fun add(value: Double) {
     *             balance += value
     *         }
     *     }
     * } asContextForEach {
     *     test("add 11.0") {
     *         add(11.0)
     *         assertEquals(53.0, balance)
     *     }
     *     test("add -11.0") {
     *         add(-11.0)
     *         assertEquals(31.0, balance)
     *     }
     * }
     * ```
     */
    public infix fun asContextForEach(
        content: Scope<suspend Value.(testExecutionScope: Test.ExecutionScope) -> Unit>.() -> Unit
    ): TestFixture<Value> {
        Scope<suspend Value.(testExecutionScope: Test.ExecutionScope) -> Unit>(
            testSuiteInScope = testSuite,
            envelopeValue = { newTestLevelValue() },
            scopingAction = { testExecutionScope, fixtureScopedAction ->
                withTestLevelValue { value ->
                    value.fixtureScopedAction(testExecutionScope)
                }
            }
        ).content()

        return this
    }

    /**
     * Provides a fresh value from `this` fixture as a parameter for each test declared in the [content]'s scope.
     *
     * Using this function implies a test-level fixture, whose value will be instantiated per test, and has
     * a lifetime of that test. This is safe for concurrent test invocation, because values are isolated.
     *
     * Usage:
     *
     * ```
     * testFixture {
     *     Account().apply { setBalance(42.0) }
     * } asParameterForEach {
     *     test("add 10.0") { account ->
     *         account.add(10.0)
     *         assertEquals(52.0, account.balance)
     *     }
     *     test("add -10.0") { account ->
     *         account.add(-10.0)
     *         assertEquals(32.0, account.balance)
     *     }
     * }
     * ```
     */
    public infix fun asParameterForEach(
        content: Scope<suspend Test.ExecutionScope.(value: Value) -> Unit>.() -> Unit
    ): TestFixture<Value> {
        Scope<suspend Test.ExecutionScope.(value: Value) -> Unit>(
            testSuiteInScope = testSuite,
            envelopeValue = { newTestLevelValue() },
            scopingAction = { testExecutionScope, fixtureScopedAction ->
                withTestLevelValue { value ->
                    fixtureScopedAction(testExecutionScope, value)
                }
            }
        ).content()

        return this
    }

    /**
     * A scope inside which tests receive a fixture-provided value.
     *
     * The fixture-provided value normally lives inside the [scopingAction]'s `fixtureScopedAction`. For
     * test-level fixtures, [envelopeValue] provides this value early for examination before the test is
     * executed. For details and motivation, see [TestEnvelopeContext].
     */
    public class Scope<FixtureScopedAction> internal constructor(
        override val testSuiteInScope: TestSuite,
        private val envelopeValue: (suspend () -> Any)?,
        private val scopingAction: suspend (
            testExecutionScope: Test.ExecutionScope,
            fixtureScopedAction: FixtureScopedAction
        ) -> Unit
    ) : TestSuiteScope {
        /**
         * Registers a [TestSuite] as a child of the scope's test suite.
         */
        @TestRegistering
        @JvmName("testSuiteFixtureScoped")
        public fun testSuite(
            @TestElementName name: String,
            @TestDisplayName displayName: String = name,
            testConfig: TestConfig = TestConfig,
            content: Scope<FixtureScopedAction>.() -> Unit
        ) {
            testSuiteInScope.testSuite(name = name, displayName = displayName, testConfig = testConfig) {
                Scope(this.testSuiteInScope, envelopeValue, scopingAction).content()
            }
        }

        /**
         * Registers a [Test] as a child of the scope's test suite.
         */
        @TestRegistering
        @JvmName("testFixtureScoped")
        public fun test(
            @TestElementName name: String,
            @TestDisplayName displayName: String = name,
            testConfig: TestConfig = TestConfig,
            fixtureScopedAction: FixtureScopedAction
        ) {
            testSuiteInScope.test(
                name = name,
                displayName = displayName,
                testConfig = testConfig.envelopeContext(envelopeValue)
            ) {
                scopingAction(this, fixtureScopedAction)
            }
        }

        private fun TestConfig.envelopeContext(envelopeValue: (suspend () -> Any)?): TestConfig =
            if (envelopeValue != null) coroutineContext(TestEnvelopeContext(envelopeValue)) else this
    }

    /**
     * A test execution envelope wrapping a test action with blocking code.
     *
     * This construct is intended to push unavoidable blocking operations, which wrap coroutines, down to the
     * lowest level possible, in order to avoid multiple switches between coroutines and blocking worlds.
     */
    internal interface BlockingEnvelope {
        /** Executes the envelope's blocking code, and the [elementAction] wrapped inside. */
        fun execute(test: Test, elementAction: () -> Unit)
    }

    /**
     * A context element providing a test execution envelope value.
     *
     * Normally, a fixture value is captured directly into the inner test lambda, without the test being aware
     * of it. For a [BlockingEnvelope], this is insufficient, as it needs to be examined outside the execution of
     * the inner test lambda. This coroutine context element, inserted via the test's [TestConfig], provides
     * the envelope directly to its test for examination before execution.
     */
    internal class TestEnvelopeContext(private val newValue: suspend () -> Any) :
        AbstractCoroutineContextElement(Key) {

        private var envelopeValue: Any? = null

        internal suspend fun envelopeValue(): Any = envelopeValue ?: newValue().also { envelopeValue = it }

        companion object {
            private val Key = object : CoroutineContext.Key<TestEnvelopeContext> {}

            suspend fun current() = currentCoroutineContext()[Key]
        }
    }

    /** Registers [action] to be called when this fixture's lifetime ends. */
    public infix fun closeWith(action: suspend Value.() -> Unit): TestFixture<Value> {
        close = action
        return this
    }

    internal suspend fun close() {
        require(level != Level.Test) { "$testSuite: close() cannot be used with a test-level fixture." }

        suiteLevelValue?.let {
            suiteLevelValue = null
            it.close()
        }
    }

    /**
     * Returns the value of `this` fixture, which is implied to be a suite-level fixture.
     */
    private suspend fun suiteLevelValue(): Value {
        when (level) {
            null -> level = Level.Suite

            Level.Suite -> {}

            Level.Test -> throw IllegalStateException(
                "An attempt was detected to reuse a test-level fixture as a suite-level fixture" +
                    " in ${testSuite.testElementPath}."
            )
        }

        suiteLevelValueMutex.withLock {
            if (suiteLevelValue == null) {
                suiteLevelValue = testSuite.newValue()
                testSuite.suiteLevelFixturesMutex.withLock {
                    testSuite.suiteLevelFixtures.add(0, this)
                }
            }
            return suiteLevelValue!!
        }
    }

    /**
     * Returns a fresh value from `this` fixture, which is implied to be a test-level fixture.
     */
    private suspend fun newTestLevelValue(): Value {
        when (level) {
            null -> level = Level.Test

            Level.Test -> {}

            Level.Suite -> throw IllegalStateException(
                "An attempt was detected to reuse a suite-level fixture as a test-level fixture" +
                    " in ${testSuite.testElementPath}."
            )
        }

        return testSuite.newValue()
    }

    /**
     * Invokes [action] with a fresh value from `this` fixture, which is implied to be a test-level fixture.
     */
    private suspend fun withTestLevelValue(action: suspend (value: Value) -> Unit) {
        require(level == Level.Test) {
            "A test-level invocation was expected in ${testSuite.testElementPath} with an actual level of $level."
        }

        var actionException: Throwable? = null

        var value: Value? = null
        try {
            val testEnvelopeContext = TestEnvelopeContext.current() ?: throw IllegalStateException(
                "A test parameter was expected, but unavailable in the coroutine context."
            )
            @Suppress("UNCHECKED_CAST")
            value = testEnvelopeContext.envelopeValue() as Value
            action(value)
        } catch (exception: Throwable) {
            actionException = exception
        } finally {
            withContext(NonCancellable) {
                try {
                    value?.close()
                } catch (closeException: Throwable) {
                    if (actionException == null) {
                        actionException = closeException
                    } else {
                        actionException.addSuppressed(closeException)
                    }
                }
            }
        }

        if (actionException != null) throw actionException
    }
}
