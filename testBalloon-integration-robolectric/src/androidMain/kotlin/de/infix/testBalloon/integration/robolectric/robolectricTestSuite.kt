package de.infix.testBalloon.integration.robolectric

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestElement
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.core.aroundAll
import de.infix.testBalloon.framework.core.parameter
import de.infix.testBalloon.framework.core.traversal
import de.infix.testBalloon.framework.core.withSingleThreadedDispatcher
import de.infix.testBalloon.framework.shared.TestElementName
import de.infix.testBalloon.framework.shared.TestRegistering
import de.infix.testBalloon.integration.robolectric.internal.robolectricContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

/**
 * Registers a Robolectric test suite as a child of the [TestSuiteScope.testSuiteInScope].
 *
 * The test suite's [content] resides in a separate [RobolectricTestSuiteContent] class which will be dynamically
 * loaded by the Robolectric Sandbox class loader for instrumentation.
 *
 * Optionally, [arguments] can be provided for the [content] constructor's parameters, if it has any.
 *
 * **Note:** [arguments] is the boundary where types and values travel between the "normal" JVM world and the
 * Robolectric environment. By default, Robolectric will re-load all classes it encounters with its own sandbox
 * class loader, making them incompatible with the "same" classes in the JVM world. To make a class `MyType` and
 * all classes in `com.example.mypackage` portable between those worlds, add the following [testConfig] parameter
 * to the [robolectricTestSuite] invocation or anywhere above it in the test element hierarchy:
 *
 * ```
 * testConfig = TestConfig.robolectric {
 *     portableClasses += MyType::class
 *     portablePackages += "com.example.mypackage"
 * }
 * ```
 */
@TestRegistering
public fun TestSuiteScope.robolectricTestSuite(
    @TestElementName name: String,
    content: KClass<out RobolectricTestSuiteContent>,
    arguments: Array<Any> = arrayOf(),
    testConfig: TestConfig = TestConfig
) {
    testSuite(
        name,
        testConfig = testConfig
            .parameter(ActiveRobolectricTestSuiteMarker.Key) { marker ->
                val activeRobolectricTestSuite = marker?.testElement
                require(activeRobolectricTestSuite == null) {
                    "Robolectric test suites must not nest.\n" +
                        "\tDetected an attempt to create a Robolectric test suite $this\n" +
                        "\tinside the Robolectric test suite $activeRobolectricTestSuite.\n" +
                        "\tPlease use regular test suites (inside or outside Robolectric test suites) for nesting."
                }
                ActiveRobolectricTestSuiteMarker(this)
            }
            .aroundAll { suiteAction ->
                @OptIn(ExperimentalCoroutinesApi::class)
                withSingleThreadedDispatcher { dispatcher ->
                    withContext(dispatcher) {
                        suiteAction()
                    }
                }
            }
            .traversal(ApplicationLifecycleTraversal())
    ) {
        val robolectricContext = testSuiteInScope.robolectricContext
        robolectricContext.onSandboxMainThread {
            val instrumentedSuiteClass = robolectricContext.instrumentedClass(content)
            val instrumentedSuite =
                instrumentedSuiteClass.declaredConstructors.first().newInstance(
                    *arguments
                ) as RobolectricTestSuiteContent
            instrumentedSuite.register(testSuiteInScope)
        }
    }
}

/**
 * A Robolectric test suite's content. See [robolectricTestSuite] for details.
 *
 * Usage:
 * ```
 * class MyRobolectricTests : RobolectricTestSuiteContent({
 *     test("my first test") {
 *         // ...
 *     }
 * })
 * ```
 */
@TestRegistering
public abstract class RobolectricTestSuiteContent protected constructor(
    private val content: TestSuiteScope.() -> Unit
) : TestSuiteScope {

    override lateinit var testSuiteInScope: TestSuite

    @JvmName("register")
    internal fun register(parent: TestSuite) {
        testSuiteInScope = parent
        content()
    }
}

private class ActiveRobolectricTestSuiteMarker(val testElement: TestElement) : TestElement.KeyedParameter(Key) {
    companion object {
        val Key = object : Key<ActiveRobolectricTestSuiteMarker> {}
    }
}

/**
 * A traversal enveloping tests and/or test element trees in an application lifecycle.
 *
 * An element's [ApplicationLifetime] determines whether a test element tree shares a single lifecycle
 * or if each test gets its own lifecycle.
 */
private class ApplicationLifecycleTraversal : TestConfig.ExecutionTraversal {
    private var lifecycleRoot: TestElement? = null

    override suspend fun aroundEach(testElement: TestElement, elementAction: suspend TestElement.() -> Unit) {
        val lifecycleRoot = lifecycleRoot ?: testElement.also { lifecycleRoot = it }
        val robolectricContext = testElement.robolectricContext
        val applicationLifetime =
            robolectricContext.settings?.applicationLifetime ?: ApplicationLifetime.Test

        if ((applicationLifetime == ApplicationLifetime.Test && testElement is Test) ||
            (applicationLifetime == ApplicationLifetime.RobolectricTestSuite && testElement == lifecycleRoot)
        ) {
            robolectricContext.withApplicationLifecycle(testElement) {
                testElement.elementAction()
            }
        } else {
            testElement.elementAction()
        }
    }
}
