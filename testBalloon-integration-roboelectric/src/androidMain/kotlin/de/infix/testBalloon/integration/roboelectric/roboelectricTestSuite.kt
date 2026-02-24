package de.infix.testBalloon.integration.roboelectric

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
import de.infix.testBalloon.integration.roboelectric.internal.roboelectricContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

/**
 * Registers a Roboelectric test suite as a child of the [TestSuiteScope.testSuiteInScope].
 *
 * The test suite's [content] resides in a separate [RoboelectricTestSuiteContent] class which will be dynamically
 * loaded by the Roboelectric Sandbox class loader for instrumentation.
 *
 * Optionally, [arguments] can be provided for the [content] constructor's parameters, if it has any.
 *
 * **Note:** [arguments] is the boundary where types and values travel between the "normal" JVM world and the
 * Roboelectric environment. By default, Roboelectric will re-load all classes it encounters with its own sandbox
 * class loader, making them incompatible with the "same" classes in the JVM world. To make a class `MyType` and
 * all classes in `com.example.mypackage` portable between those worlds, add the following [testConfig] parameter
 * to the [roboelectricTestSuite] invocation or anywhere above it in the test element hierarchy:
 *
 * ```
 * testConfig = TestConfig.roboelectric {
 *     portableClasses += MyType::class
 *     portablePackages += "com.example.mypackage"
 * }
 * ```
 */
@TestRegistering
public fun TestSuiteScope.roboelectricTestSuite(
    @TestElementName name: String,
    content: KClass<out RoboelectricTestSuiteContent>,
    arguments: Array<Any> = arrayOf(),
    testConfig: TestConfig = TestConfig
) {
    testSuite(
        name,
        testConfig = testConfig
            .parameter(ActiveRoboelectricTestSuiteMarker.Key) { marker ->
                val activeRoboelectricTestSuite = marker?.testElement
                require(activeRoboelectricTestSuite == null) {
                    "Roboelectric test suites must not nest.\n" +
                        "\tDetected an attempt to create a Roboelectric test suite $this\n" +
                        "\tinside the Roboelectric test suite $activeRoboelectricTestSuite.\n" +
                        "\tPlease use regular test suites (inside or outside Roboelectric test suites) for nesting."
                }
                ActiveRoboelectricTestSuiteMarker(this)
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
        val roboelectricContext = testSuiteInScope.roboelectricContext
        roboelectricContext.onSandboxMainThread {
            val instrumentedSuiteClass = roboelectricContext.instrumentedClass(content)
            val instrumentedSuite =
                instrumentedSuiteClass.declaredConstructors.first().newInstance(
                    *arguments
                ) as RoboelectricTestSuiteContent
            instrumentedSuite.register(testSuiteInScope)
        }
    }
}

/**
 * A Roboelectric test suite's content. See [roboelectricTestSuite] for details.
 *
 * Usage:
 * ```
 * class MyRoboelectricTests : RoboelectricTestSuiteContent({
 *     test("my first test") {
 *         // ...
 *     }
 * })
 * ```
 */
@TestRegistering
public abstract class RoboelectricTestSuiteContent protected constructor(
    private val content: TestSuiteScope.() -> Unit
) : TestSuiteScope {

    override lateinit var testSuiteInScope: TestSuite

    @JvmName("register")
    internal fun register(parent: TestSuite) {
        testSuiteInScope = parent
        content()
    }
}

private class ActiveRoboelectricTestSuiteMarker(val testElement: TestElement) : TestElement.KeyedParameter(Key) {
    companion object {
        val Key = object : Key<ActiveRoboelectricTestSuiteMarker> {}
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
        val roboelectricContext = testElement.roboelectricContext
        val applicationLifetime =
            roboelectricContext.settings?.applicationLifetime ?: ApplicationLifetime.Test

        if ((applicationLifetime == ApplicationLifetime.Test && testElement is Test) ||
            (applicationLifetime == ApplicationLifetime.RoboelectricTestSuite && testElement == lifecycleRoot)
        ) {
            roboelectricContext.withApplicationLifecycle(testElement) {
                testElement.elementAction()
            }
        } else {
            testElement.elementAction()
        }
    }
}
