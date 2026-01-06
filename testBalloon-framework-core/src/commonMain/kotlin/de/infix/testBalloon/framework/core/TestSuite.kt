package de.infix.testBalloon.framework.core

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
 * A test suite containing child [TestElement]s (tests and/or suites).
 *
 * Please see [TestSuiteScope] for details.
 */
@TestRegistering
public open class TestSuite internal constructor(
    parent: TestSuite?,
    name: String,
    displayName: String = name,
    testConfig: TestConfig = TestConfig,
    private val content: TestSuite.() -> Unit = {}
) : TestElement(parent, name = name, displayName = displayName, testConfig),
    AbstractTestSuite,
    TestSuiteScope {

    override val testSuiteInScope: TestSuite get() = this

    internal val testElementChildren: Iterable<TestElement> by ::children

    private val children: MutableList<TestElement> = mutableListOf()

    override val testElementIsEnabled: Boolean get() = super.testElementIsEnabled && enabledChildExists

    private var enabledChildExists: Boolean = false

    private val childElementNameCount: MutableMap<String, Int> = mutableMapOf()

    private val childDisplayNameCount: MutableMap<String, Int> = mutableMapOf()

    /**
     * The test suite's [CoroutineScope], valid only during the suite's execution.
     *
     * Use [testSuiteCoroutineScope] to launch additional coroutines in test fixtures. Such coroutines must complete
     * or be canceled explicitly when their fixture closes. The test suite execution will wait for coroutines in
     * [testSuiteCoroutineScope] to finish before completing.
     */
    public val testSuiteCoroutineScope: CoroutineScope
        get() = executionContext?.let { CoroutineScope(it) } ?: throw IllegalStateException(
            "$testElementPath: testSuiteCoroutineScope is only available during execution"
        )

    /** The [CoroutineContext] used by this suite during execution. */
    private var executionContext: CoroutineContext? = null

    private var privateConfiguration: TestConfig = TestConfig.suiteLifecycleAction()

    /** Suite-level fixtures registered with this suite, in reverse order of registration. */
    @GuardedBy("suiteLevelFixturesMutex") // for adding only
    internal val suiteLevelFixtures = mutableListOf<TestFixture<*>>()
    internal val suiteLevelFixturesMutex = Mutex()

    internal enum class ChildNameType {
        Element,
        Display
    }

    /**
     * Returns a [type] name for [originalName] which is unique among children of this suite.
     *
     * Guarantees that [originalName] will not grow beyond [UNIQUE_APPENDIX_LENGTH_LIMIT].
     */
    internal fun uniqueChildName(originalName: String, type: ChildNameType): String {
        val registeredCount = if (type == ChildNameType.Element) childElementNameCount else childDisplayNameCount
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
                    TestConfig.Permit.SuiteWithoutChildren in parameters.permits
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
                        TestConfig.Invocation.Sequential
                    } else {
                        TestConfig.Invocation.current()
                    }
                    coroutineScope {
                        for (childElement in testElementChildren) {
                            when (invocation) {
                                TestConfig.Invocation.Sequential -> {
                                    childElement.execute(report)
                                }

                                TestConfig.Invocation.Concurrent -> {
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

    private fun TestConfig.suiteLifecycleAction(): TestConfig = executionWrapping { elementAction ->
        var actionException: Throwable? = null

        this@TestSuite.executionContext = currentCoroutineContext()

        try {
            elementAction()
        } catch (exception: Throwable) {
            actionException = exception
        } finally {
            withContext(NonCancellable) {
                for (fixture in this@TestSuite.suiteLevelFixtures) {
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
