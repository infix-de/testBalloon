package de.infix.testBalloon.framework.core

import de.infix.testBalloon.framework.core.TestSuite.Companion.UNIQUE_APPENDIX_LENGTH_LIMIT
import de.infix.testBalloon.framework.core.internal.GuardedBy
import de.infix.testBalloon.framework.core.internal.TestSetupReport
import de.infix.testBalloon.framework.shared.AbstractTestSuite
import de.infix.testBalloon.framework.shared.TestDisplayName
import de.infix.testBalloon.framework.shared.TestElementName
import de.infix.testBalloon.framework.shared.TestRegistering
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Registers a top-level [TestSuite].
 *
 * [compartment] is the optional [TestCompartment] the test suite belongs to.
 *
 * Usage:
 * ```
 * val myTestSuite by testSuite(compartment = TestCompartment.Concurrent) {
 *     // test suite content
 * }
 * ```
 *
 * Note: [name] and [displayName] are usually initialized by the compiler plugin, with [name] being the
 * fully qualified name of the corresponding property and [displayName] being the simple name of that property.
 * If you provide a [name] explicitly, you are responsible for making it unique within the compilation module.
 */
@TestRegistering
public fun testSuite(
    @TestElementName name: String = "",
    @TestDisplayName displayName: String = name,
    compartment: () -> TestCompartment,
    testConfig: TestConfig = TestConfig,
    content: TestSuite.() -> Unit
): Lazy<TestSuite> = lazy {
    TestSuite(
        parent = compartment(),
        name = name,
        displayName = displayName,
        testConfig = testConfig,
        content = content
    )
}

/**
 * Registers a top-level [TestSuite].
 *
 * Usage:
 * ```
 * val myTestSuite by testSuite {
 *     // test suite content
 * }
 * ```
 *
 * Note: [name] and [displayName] are usually initialized by the compiler plugin, with [name] being the
 * fully qualified name of the corresponding property and [displayName] being the simple name of that property.
 * If you provide a [name] explicitly, you are responsible for making it unique within the compilation module.
 */
@TestRegistering
public fun testSuite(
    @TestElementName name: String = "",
    @TestDisplayName displayName: String = name,
    testConfig: TestConfig = TestConfig,
    content: TestSuite.() -> Unit
): Lazy<TestSuite> = lazy {
    TestSuite(
        parent = TestSession.global.defaultCompartment,
        name = name,
        displayName = displayName,
        testConfig = testConfig,
        content = content
    )
}

/**
 * A test suite containing child [TestElement]s (tests and/or suites). A suite may not contain test logic.
 */
@TestRegistering
public open class TestSuite internal constructor(
    parent: TestSuite?,
    name: String,
    displayName: String = name,
    testConfig: TestConfig = TestConfig,
    private val content: TestSuite.() -> Unit = {}
) : TestElement(parent, name = name, displayName = displayName, testConfig),
    AbstractTestSuite {

    internal val testElementChildren: Iterable<TestElement> by ::children

    private val children: MutableList<TestElement> = mutableListOf()

    override val testElementIsEnabled: Boolean get() = super.testElementIsEnabled && enabledChildExists

    private var enabledChildExists: Boolean = false

    private val childElementNameCount: MutableMap<String, Int> = mutableMapOf()

    private val childDisplayNameCount: MutableMap<String, Int> = mutableMapOf()

    /**
     * The test suite's [CoroutineScope], valid only during the suite's execution.
     *
     * Use [testSuiteScope] to launch coroutines in test fixtures. Such coroutines must complete or be canceled
     * explicitly when their fixture closes. The test suite execution will wait for coroutines in [testSuiteScope]
     * before completing.
     */
    public val testSuiteScope: CoroutineScope
        get() = executionContext?.let { CoroutineScope(it) }
            ?: throw IllegalStateException("$testElementPath: testSuiteScope is only available during execution")

    /** The [CoroutineContext] used by this suite during execution. */
    private var executionContext: CoroutineContext? = null

    private var privateConfiguration: TestConfig = TestConfig.suiteLifecycleAction()

    /** Fixtures created while executing this suite, in reverse order of fixture creation. */
    @GuardedBy("fixturesMutex") // for adding only
    private val fixtures = mutableListOf<Fixture<*>>()
    private val fixturesMutex = Mutex()

    // region – We need these constructor variants only for top-level test suites registered as classes.
    //
    // The constructor variants ensure proper overload resolution with default parameters,
    // because Kotlin determines argument positions before default values are filled in.
    // See https://youtrack.jetbrains.com/issue/KT-48521.

    @Deprecated(
        "Using a class to register a top-level test suite will be dropped in a future version." +
            " Please use a top-level property instead."
    )
    protected constructor(
        content: TestSuite.() -> Unit,
        @TestElementName name: String = "",
        @TestDisplayName displayName: String = name
    ) : this(
        parent = TestSession.global.defaultCompartment,
        name = name,
        displayName = displayName,
        content = content
    )

    @Deprecated(
        "Using a class to register a top-level test suite will be dropped in a future version." +
            " Please use a top-level property instead."
    )
    protected constructor(
        testConfig: TestConfig,
        content: TestSuite.() -> Unit,
        @TestElementName name: String = "",
        @TestDisplayName displayName: String = name
    ) :
        this(
            parent = TestSession.global.defaultCompartment,
            name = name,
            displayName = displayName,
            testConfig = testConfig,
            content = content
        )

    @Deprecated(
        "Using a class to register a top-level test suite will be dropped in a future version." +
            " Please use a top-level property instead."
    )
    protected constructor(
        compartment: TestCompartment,
        content: TestSuite.() -> Unit,
        @TestElementName name: String = "",
        @TestDisplayName displayName: String = name
    ) : this(
        parent = compartment,
        name = name,
        displayName = displayName,
        content = content
    )

    @Deprecated(
        "Using a class to register a top-level test suite will be dropped in a future version." +
            " Please use a top-level property instead."
    )
    protected constructor(
        compartment: TestCompartment,
        testConfig: TestConfig,
        content: TestSuite.() -> Unit,
        @TestElementName name: String = "",
        @TestDisplayName displayName: String = name
    ) : this(
        parent = compartment,
        name = name,
        displayName = displayName,
        testConfig = testConfig,
        content = content
    )

    @Deprecated(
        "Using a class to register a top-level test suite will be dropped in a future version." +
            " Please use a top-level property instead."
    )
    protected constructor(
        name: String,
        testConfig: TestConfig,
        content: TestSuite.() -> Unit,
        @TestDisplayName displayName: String = name
    ) : this(
        parent = TestSession.global.defaultCompartment,
        name = name,
        displayName = displayName,
        testConfig = testConfig,
        content = content
    )

    @Deprecated(
        "Using a class to register a top-level test suite will be dropped in a future version." +
            " Please use a top-level property instead."
    )
    protected constructor(
        name: String,
        compartment: TestCompartment,
        content: TestSuite.() -> Unit,
        @TestDisplayName displayName: String = name
    ) : this(
        parent = compartment,
        name = name,
        displayName = displayName,
        content = content
    )

    @Deprecated(
        "Using a class to register a top-level test suite will be dropped in a future version." +
            " Please use a top-level property instead."
    )
    protected constructor(
        name: String,
        compartment: TestCompartment,
        testConfig: TestConfig,
        content: TestSuite.() -> Unit,
        @TestDisplayName displayName: String = name
    ) : this(
        parent = compartment,
        name = name,
        displayName = displayName,
        testConfig = testConfig,
        content = content
    )

    // endregion

    internal enum class ChildNameType {
        ELEMENT,
        DISPLAY
    }

    /**
     * Returns a [type] name for [originalName] which is unique among children of this suite.
     *
     * Guarantees that [originalName] will not grow beyond [UNIQUE_APPENDIX_LENGTH_LIMIT].
     */
    internal fun uniqueChildName(originalName: String, type: ChildNameType): String {
        val registeredCount = if (type == ChildNameType.ELEMENT) childElementNameCount else childDisplayNameCount
        val nameCount = (registeredCount[originalName] ?: 0) + 1
        registeredCount[originalName] = nameCount
        return if (nameCount == 1) {
            originalName
        } else {
            val appendix = appendix(nameCount)
            require(appendix.length <= UNIQUE_APPENDIX_LENGTH_LIMIT) {
                "$this failed to provide a unique name. The required appendix '$appendix' is longer" +
                    " than $UNIQUE_APPENDIX_LENGTH_LIMIT characters.\n" +
                    "\tOriginal name: $originalName"
            }
            "$originalName$appendix"
        }
    }

    internal fun registerChildElement(childElement: TestElement) {
        require(
            this == suitesInRegistrationScope.firstOrNull() ||
                ( // TestCompartments and TestSession accept children without being in registration scope,
                    testElementParent?.testElementParent == null &&
                        // but only if no suite below a compartment is in registration scope.
                        suitesInRegistrationScope.isEmpty()
                    )
        ) {
            "$childElement tried to register as a child of $this," +
                " which currently is not the closest registration scope.\n" +
                "\tThe closest registration scope at this point is ${suitesInRegistrationScope.firstOrNull()}."
        }
        children.add(childElement)
    }

    /**
     * Executes [action] on all child elements, recursively, in depth-first order.
     */
    internal suspend fun forEachChildElement(action: suspend (element: TestElement) -> Unit) {
        for (childElement in testElementChildren) {
            if (childElement is TestSuite) {
                childElement.forEachChildElement(action)
            }
            action(childElement)
        }
    }

    /**
     * Registers an [executionWrappingAction] which wraps the execution actions of this test suite.
     *
     * [executionWrappingAction] wraps around the [TestElement]'s primary `testSuiteAction`, which accumulates
     * the execution actions of its children.
     * The wrapping action will be invoked only if at least one (direct or indirect) child [Test] executes.
     * See also [TestElementExecutionWrappingAction] for requirements.
     *
     * Note: [TestSuite.aroundAll] will not wrap around fixtures registered in its [TestSuite]. Fixtures (which are
     * lazily created on their first invocation) will close _after_ any [aroundAll] actions registered in their
     * [TestSuite]. Use the suite's `testConfig` parameter with `TestConfig.aroundAll` to register an action which
     * also wraps around the suite's fixtures.
     *
     * Usage:
     * ```
     *     aroundAll { testSuiteAction ->
     *         withContext(CoroutineName("parent coroutine configured by aroundAll")) {
     *             testSuiteAction()
     *         }
     *     }
     * ```
     */
    @Deprecated(
        "Please use the test suite's parameter testConfig = TestConfig.aroundAll { ... } instead." +
            " Scheduled for removal in TestBalloon 0.8."
    )
    public fun aroundAll(executionWrappingAction: TestSuiteExecutionWrappingAction) {
        privateConfiguration = privateConfiguration.aroundAll { elementAction ->
            executionWrappingAction { elementAction() }
        }
    }

    /**
     * Registers a [TestSuite] as a child of this test suite.
     */
    @TestRegistering
    public fun testSuite(
        @TestElementName name: String,
        @TestDisplayName displayName: String = name,
        testConfig: TestConfig = TestConfig,
        content: TestSuite.() -> Unit
    ) {
        TestSuite(this, name = name, displayName = displayName, testConfig = testConfig, content = content)
    }

    /**
     * Registers a [Test] as a child of this test suite.
     */
    @TestRegistering
    public fun test(
        @TestElementName name: String,
        @TestDisplayName displayName: String = name,
        testConfig: TestConfig = TestConfig,
        action: suspend TestExecutionScope.() -> Unit
    ) {
        Test(this, name = name, displayName = displayName, testConfig = testConfig, action)
    }

    override fun setUp(selection: Selection, report: TestSetupReport) {
        if (!selection.mayInclude(this)) {
            // Short-circuit test registration, if possible.
            // If the selection is sure not to include this suite, do not create any children below it.
            // This helps to keep the test element hierarchy small, speeding up the test registration phase.
            isIncluded = false
            return
        }

        setUpReporting(report) {
            inRegistrationScope {
                content()
            }

            super.setUp(selection, report)

            check(
                testElementChildren.any() ||
                    this is TestSession ||
                    TestPermit.SUITE_WITHOUT_CHILDREN in parameters.permits
            ) {
                buildString {
                    append("$this does not contain any child tests or test suites.\n")
                    append("\tPlease add at least one test or test suite to this test suite, or remove it.")
                }
            }

            testElementChildren.forEach {
                it.setUp(selection, report)
            }

            // Propagate inclusion status bottom up: A suite without children excludes itself.
            isIncluded = testElementChildren.any { it.isIncluded }
            // Propagate enabled status bottom up: A suite without enabled children disables itself.
            enabledChildExists = isIncluded && testElementChildren.any { it.isIncluded && it.testElementIsEnabled }
        }
    }

    override suspend fun execute(report: TestExecutionReport) {
        // short-circuit excluded suites, but report at least the session
        if (!isIncluded && this !is TestSession) return

        executeReporting(report) {
            if (testElementIsEnabled) {
                @Suppress("DEPRECATION")
                testConfig.chainedWith(privateConfiguration).executeWrapped(this) {
                    val invocation = if (testElementParent == null) {
                        // A TestSession (no parent) must always execute its compartments sequentially.
                        TestInvocation.SEQUENTIAL
                    } else {
                        TestInvocation.current()
                    }
                    coroutineScope {
                        for (childElement in testElementChildren) {
                            when (invocation) {
                                TestInvocation.SEQUENTIAL -> {
                                    childElement.execute(report)
                                }

                                TestInvocation.CONCURRENT -> {
                                    launch {
                                        childElement.execute(report)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // "Execute" disabled child elements for reporting only.
                for (childElement in testElementChildren) {
                    childElement.execute(report)
                }
            }
        }
    }

    internal companion object {
        /** The maximum length which a unique appendix is guaranteed not to exceed. */
        internal val UNIQUE_APPENDIX_LENGTH_LIMIT = appendix(999_999).length

        /** Returns an appendix for [number]. */
        private fun appendix(number: Int) = " 〈$number〉"

        /** A stack of suites in registration scope, innermost scope first */
        private val suitesInRegistrationScope = mutableListOf<TestSuite>()

        /** Executes [action] in the registration scope of [this] suite. */
        private fun TestSuite.inRegistrationScope(action: () -> Unit) {
            suitesInRegistrationScope.add(0, this)
            try {
                action()
            } finally {
                check(suitesInRegistrationScope.removeAt(0) == this)
            }
        }
    }

    /**
     * Registers a fixture, a state holder for a lazily initialized [Value] to be used in tests.
     *
     * For details, see [Fixture].
     */
    public fun <Value : Any> testFixture(value: suspend TestSuite.() -> Value): Fixture<Value> = Fixture(this, value)

    /**
     * A fixture is a state holder for a lazily initialized [Value] to be used in tests.
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
     * - If [Value] is an [AutoCloseable], the fixture will call `close` at the end of its lifetime, otherwise
     *   [closeWith] can specify an action to be called on close.
     * - A fixture can be used either as a suite-level fixture, or as a test-level fixture, but not both.
     *
     * Usage:
     * - For a suite-level fixture, see [invoke], [asContextForAll] and [asParameterForAll].
     * - For a test-level fixture, see [asContextForEach] and [asParameterForEach].
     */
    public class Fixture<Value : Any> internal constructor(
        private val suite: TestSuite,
        private val newValue: suspend TestSuite.() -> Value
    ) {
        internal enum class Level {
            Suite,
            Test
        }

        private var level: Level? = null

        // The following two properties are only relevant if this is a suite-level fixture.
        @GuardedBy("valueMutex")
        private var value: Value? = null
        private val valueMutex = Mutex()

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
         * Note: As the context for tests in this scope is the fixture's value, the usual [TestExecutionScope] is
         * unavailable as a context. [TestExecutionScope] is, however, provided as a parameter to each test.
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
            content: Scope<suspend Value.(testExecutionScope: TestExecutionScope) -> Unit>.() -> Unit
        ): Fixture<Value> {
            Scope<suspend Value.(testExecutionScope: TestExecutionScope) -> Unit>(
                suite = suite,
                scopingAction = { fixtureScopedAction ->
                    suiteLevelValue().fixtureScopedAction(this)
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
         * } asContextForAll {
         *     test("verify score") { repository ->
         *         repository.getScore(...)
         *     }
         * }
         * ```
         */
        public infix fun asParameterForAll(
            content: Scope<suspend TestExecutionScope.(value: Value) -> Unit>.() -> Unit
        ): Fixture<Value> {
            Scope<suspend TestExecutionScope.(value: Value) -> Unit>(
                suite = suite,
                scopingAction = { fixtureScopedAction ->
                    fixtureScopedAction(suiteLevelValue())
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
         * Note: As the context for tests in this scope is the fixture's value, the usual [TestExecutionScope] is
         * unavailable as a context. [TestExecutionScope] is, however, provided as a parameter to each test.
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
            content: Scope<suspend Value.(testExecutionScope: TestExecutionScope) -> Unit>.() -> Unit
        ): Fixture<Value> {
            Scope<suspend Value.(testExecutionScope: TestExecutionScope) -> Unit>(
                suite = suite,
                scopingAction = { fixtureScopedAction ->
                    withTestLevelValue { value ->
                        value.fixtureScopedAction(this)
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
            content: Scope<suspend TestExecutionScope.(value: Value) -> Unit>.() -> Unit
        ): Fixture<Value> {
            Scope<suspend TestExecutionScope.(value: Value) -> Unit>(
                suite = suite,
                scopingAction = { fixtureScopedAction ->
                    withTestLevelValue { value ->
                        fixtureScopedAction(value)
                    }
                }
            ).content()

            return this
        }

        @DslMarker
        private annotation class ScopeDsl

        /**
         * A scope where tests receive a fixture-provided value.
         */
        @ScopeDsl
        public class Scope<FixtureScopedAction>(
            internal val suite: TestSuite,
            private val scopingAction: suspend TestExecutionScope.(fixtureScopedAction: FixtureScopedAction) -> Unit
        ) {
            /**
             * Registers an [executionWrappingAction] which wraps the execution actions of this scope's test suite.
             *
             * See [TestSuite.aroundAll] for details.
             */
            @Deprecated(
                "Please use the test suite's parameter testConfig = TestConfig.aroundAll { ... } instead." +
                    " Scheduled for removal in TestBalloon 0.8."
            )
            public fun aroundAll(executionWrappingAction: TestSuiteExecutionWrappingAction) {
                suite.aroundAll(executionWrappingAction)
            }

            /**
             * Registers a [TestSuite] as a child of the scope's test suite.
             */
            @TestRegistering
            public fun testSuite(
                @TestElementName name: String,
                @TestDisplayName displayName: String = name,
                testConfig: TestConfig = TestConfig,
                content: Scope<FixtureScopedAction>.() -> Unit
            ) {
                suite.testSuite(name = name, displayName = displayName, testConfig = testConfig) {
                    Scope(this, scopingAction).content()
                }
            }

            /**
             * Registers a [Test] as a child of the scope's test suite.
             */
            @TestRegistering
            public fun test(
                @TestElementName name: String,
                @TestDisplayName displayName: String = name,
                testConfig: TestConfig = TestConfig,
                fixtureScopedAction: FixtureScopedAction
            ) {
                suite.test(name = name, displayName = displayName, testConfig = testConfig) {
                    scopingAction(fixtureScopedAction)
                }
            }

            /**
             * Registers a fixture, a state holder for a lazily initialized [Value].
             *
             * For details, see [Fixture].
             */
            public fun <Value : Any> testFixture(value: suspend TestSuite.() -> Value): Fixture<Value> =
                suite.testFixture(value)
        }

        /** Registers [action] to be called when this fixture's lifetime ends. */
        public infix fun closeWith(action: suspend Value.() -> Unit): Fixture<Value> {
            close = action
            return this
        }

        internal suspend fun close() {
            value?.let {
                value = null
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
                        " in ${suite.testElementPath}."
                )
            }

            valueMutex.withLock {
                if (value == null) {
                    value = suite.newValue()
                    suite.fixturesMutex.withLock {
                        suite.fixtures.add(0, this)
                    }
                }
                return value!!
            }
        }

        /**
         * Invokes [action] with a fresh value from `this` fixture, which is implied to be a test-level fixture.
         */
        private suspend fun withTestLevelValue(action: suspend (value: Value) -> Unit) {
            when (level) {
                null -> level = Level.Test

                Level.Test -> {}

                Level.Suite -> throw IllegalStateException(
                    "An attempt was detected to reuse a suite-level fixture as a test-level fixture" +
                        " in ${suite.testElementPath}."
                )
            }

            var actionException: Throwable? = null
            var value: Value? = null

            try {
                value = suite.newValue()
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

    private fun TestConfig.suiteLifecycleAction(): TestConfig = executionWrapping { elementAction ->
        var actionException: Throwable? = null

        this@TestSuite.executionContext = currentCoroutineContext()

        try {
            elementAction()
        } catch (exception: Throwable) {
            actionException = exception
        } finally {
            withContext(NonCancellable) {
                for (fixture in this@TestSuite.fixtures) {
                    try {
                        fixture.close()
                    } catch (closeException: Throwable) {
                        if (actionException == null) {
                            actionException = closeException
                        } else {
                            actionException.addSuppressed(closeException)
                        }
                    }
                }
            }

            this@TestSuite.executionContext = null

            if (actionException != null) throw actionException
        }
    }
}

/**
 * An action wrapping the actions of a [TestSuite].
 *
 * See also [TestElementExecutionWrappingAction] for requirements.
 */
public typealias TestSuiteExecutionWrappingAction = suspend (testSuiteAction: suspend TestSuite.() -> Unit) -> Unit
