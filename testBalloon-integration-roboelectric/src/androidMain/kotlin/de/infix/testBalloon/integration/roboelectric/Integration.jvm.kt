package de.infix.testBalloon.integration.roboelectric

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.core.aroundEachTest
import de.infix.testBalloon.framework.core.internal.logInfo
import de.infix.testBalloon.framework.core.mainDispatcher
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.shared.TestElementName
import de.infix.testBalloon.framework.shared.TestRegistering
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import local.org.robolectric.runner.common.ExperimentalRunnerApi
import local.org.robolectric.runner.common.ManifestResolver
import local.org.robolectric.runner.common.RobolectricDependencies
import local.org.robolectric.runner.common.SandboxConfigurator
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.ResourcesMode
import org.robolectric.annotation.SQLiteMode
import org.robolectric.interceptors.AndroidInterceptors
import org.robolectric.internal.AndroidSandbox
import org.robolectric.internal.bytecode.InstrumentationConfiguration
import org.robolectric.internal.bytecode.Interceptors
import org.robolectric.pluginapi.config.ConfigurationStrategy
import kotlin.reflect.KClass
import kotlin.reflect.jvm.javaMethod

/**
 * Registers a [RoboelectricTestSuite] as a child of the [TestSuiteScope.testSuiteInScope].
 *
 * Roboelectric test suites will be dynamically loaded by the Roboelectric Sandbox class loader for instrumentation.
 */
@TestRegistering
public fun TestSuiteScope.roboelectricTestSuite(
    @TestElementName name: String,
    suiteClass: KClass<out RoboelectricTestSuite>,
    testConfig: TestConfig = TestConfig
) {
    val instrumentedSuite = roboelectricAdapter.withSandboxContextClassLoader {
        val instrumentedSuiteClass = roboelectricAdapter.instrumentedSuiteClass(suiteClass)
        instrumentedSuiteClass.declaredConstructors.first().newInstance() as RoboelectricTestSuite
    }

    instrumentedSuite.register(testSuiteInScope, name, testConfig)
}

/**
 * A test suite with Roboelectric instrumentation.
 *
 * A specific test suite must be a subclass with a single no-argument constructor.
 *
 * Usage:
 * ```
 * class MyRoboelectricTests : RoboelectricTestSuite({
 *     test("my first test") {
 *         // ...
 *     }
 * })
 * ```
 */
public open class RoboelectricTestSuite(private val content: TestSuite.() -> Unit) {
    @JvmName("register")
    internal fun register(parent: TestSuite, name: String, testConfig: TestConfig) {
        with(parent) {
            testSuite(
                name,
                testConfig = testConfig
                    .mainDispatcher() // use a single-threaded dispatcher as Main dispatcher
                    .aroundEachTest { suiteAction ->
                        roboelectricAdapter.withApplicationSetup("$testElementPath") {
                            suiteAction()
                        }
                    },
                content = content
            )
        }
    }
}

private val roboelectricAdapter by lazy { RoboelectricAdapter() }

/**
 * An adapter connecting TestBalloon to Roboelectric with a minimal API surface.
 */
@OptIn(ExperimentalRunnerApi::class)
private class RoboelectricAdapter {
    private val dependencies = RobolectricDependencies.create()

    private val configuration: ConfigurationStrategy.Configuration =
        dependencies.configurationStrategy.getConfig(
            RoboelectricTestSuite::class.java,
            RoboelectricTestSuite::register.javaMethod
        )

    private val config: Config = configuration.get(Config::class.java)
    private val manifest = ManifestResolver.resolveManifest(config)
    private val sdk = dependencies.sdkPicker.selectSdks(configuration, manifest).first().also {
        logInfo { "${testPlatform.displayName}: Using Roboelectric configured for $it" }
    }

    private val configurator =
        SandboxConfigurator(
            dependencies.androidConfigurer,
            dependencies.shadowProviders,
            dependencies.classHandlerBuilder
        )

    private val instrumentationConfiguration = run {
        val builder = InstrumentationConfiguration.newBuilder()
        val interceptors = Interceptors(AndroidInterceptors.all())
        dependencies.androidConfigurer.configure(builder, interceptors)
        dependencies.androidConfigurer.withConfig(builder, config)
        builder
            .doNotAcquirePackage("de.infix.testBalloon.")
            .build()
    }

    // TODO: support sandboxes parameterized with different `Config` configurations.
    private val sandbox: AndroidSandbox = dependencies.sandboxManager.getAndroidSandbox(
        instrumentationConfiguration,
        sdk,
        configuration.get(ResourcesMode.Mode::class.java),
        configuration.get(LooperMode.Mode::class.java),
        configuration.get(SQLiteMode.Mode::class.java),
        configuration.get(GraphicsMode.Mode::class.java)
    ).also {
        configurator.configureSandbox(it, config, sdk)
    }

    /**
     * Returns a Roboelectric sandbox-instrumented Java class for [suiteClass].
     */
    @Suppress("UNCHECKED_CAST")
    fun instrumentedSuiteClass(suiteClass: KClass<out RoboelectricTestSuite>): Class<RoboelectricTestSuite> =
        sandbox.robolectricClassLoader.loadClass(suiteClass.qualifiedName) as Class<RoboelectricTestSuite>

    /**
     * Executes [action] in a Roboelectric sandbox test environment with a fresh application setup.
     */
    suspend fun withApplicationSetup(temporaryDirectoryName: String, action: suspend () -> Unit) {
        val testEnvironment = sandbox.testEnvironment

        onSandboxMainThread {
            testEnvironment.setUpApplicationState(temporaryDirectoryName, configuration, manifest)
        }

        withContext(Dispatchers.Main) {
            withSandboxContextClassLoader {
                try {
                    action()
                } finally {
                    onSandboxMainThread {
                        testEnvironment.tearDownApplication()
                        testEnvironment.resetState()
                    }
                }
            }
        }
    }

    private fun onSandboxMainThread(action: () -> Unit) {
        sandbox.runOnMainThread {
            withSandboxContextClassLoader {
                action()
            }
        }
    }

    /**
     * Executes [action] with a Thread-local sandbox class loader in effect.
     *
     * Note: 'inline' makes this function work in suspending and blocking contexts.
     */
    inline fun <Result> withSandboxContextClassLoader(action: () -> Result): Result {
        val originalContextClassLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = sandbox.robolectricClassLoader
        try {
            return action()
        } finally {
            Thread.currentThread().contextClassLoader = originalContextClassLoader
        }
    }
}
