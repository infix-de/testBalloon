package de.infix.testBalloon.integration.roboelectric

import de.infix.testBalloon.framework.core.TestElement
import de.infix.testBalloon.framework.core.internal.logDebug
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import local.org.robolectric.runner.common.ExperimentalRunnerApi
import local.org.robolectric.runner.common.ManifestResolver
import local.org.robolectric.runner.common.RobolectricDependencies
import local.org.robolectric.runner.common.SandboxConfigurator
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.ResourcesMode
import org.robolectric.annotation.SQLiteMode
import org.robolectric.interceptors.AndroidInterceptors
import org.robolectric.internal.AndroidSandbox
import org.robolectric.internal.bytecode.InstrumentationConfiguration
import org.robolectric.internal.bytecode.Interceptors
import org.robolectric.pluginapi.Sdk
import org.robolectric.pluginapi.config.ConfigurationStrategy
import org.robolectric.plugins.SdkCollection
import kotlin.reflect.KClass
import kotlin.reflect.jvm.javaMethod

internal val TestElement.roboelectricContext
    get() = testElementParameter(RoboelectricContext.Key) ?: RoboelectricContext.defaultContext

/**
 * A Roboelectric context as a test element parameter.
 */
@OptIn(ExperimentalRunnerApi::class)
internal class RoboelectricContext(
    val testElement: TestElement?,
    val config: Config,
    val settings: RoboelectricSettings?
) : TestElement.KeyedParameter(Key) {
    companion object {
        val Key = object : Key<RoboelectricContext> {}

        private val dependencies = RobolectricDependencies.create()

        private val defaultConfiguration: ConfigurationStrategy.Configuration =
            dependencies.configurationStrategy.getConfig(
                RoboelectricTestSuiteContent::class.java,
                RoboelectricTestSuiteContent::register.javaMethod
            )

        val defaultConfig: Config = defaultConfiguration.get(Config::class.java)

        val sdkCollection: SdkCollection = dependencies.injector.getInstance(SdkCollection::class.java)

        val defaultContext by lazy { RoboelectricContext(testElement = null, config = defaultConfig, settings = null) }
    }

    private val manifest = ManifestResolver.resolveManifest(config) // uses: config.manifest

    private val configuration: ConfigurationStrategy.Configuration =
        dependencies.configurationStrategy.getConfig(
            RoboelectricTestSuiteContent::class.java,
            RoboelectricTestSuiteContent::register.javaMethod
        ).apply {
            map()[Config::class.java] = config
            settings?.conscryptMode?.let { map()[ConscryptMode::class.java] = it }
        }

    private val sandbox: AndroidSandbox by lazy {
        val defaultSdk: Sdk by lazy { dependencies.sdkPicker.selectSdks(configuration, manifest).first() }

        val sdk = (config.sdk.firstOrNull()?.let { sdkCollection.getSdk(it) } ?: defaultSdk).also {
            @OptIn(TestBalloonInternalApi::class)
            logDebug { "${testPlatform.displayName}: Using Roboelectric configured for $it" }
        }

        val configurator =
            SandboxConfigurator(
                dependencies.androidConfigurer,
                dependencies.shadowProviders,
                dependencies.classHandlerBuilder
            )

        val instrumentationConfiguration = run {
            val builder = InstrumentationConfiguration.newBuilder()
            val interceptors = Interceptors(AndroidInterceptors.all())
            dependencies.androidConfigurer.configure(builder, interceptors)
            dependencies.androidConfigurer.withConfig(builder, config) // uses: config.{shadows,instrumentedPackages}
            settings?.portableClasses?.forEach {
                builder.doNotAcquireClass(it.java)
            }
            settings?.portablePackages?.forEach {
                // Always make a notation a full package name, even if it does not end with '.'.
                builder.doNotAcquirePackage(if (it.endsWith('.')) it else "$it.")
            }
            @OptIn(TestBalloonInternalApi::class)
            builder
                .doNotAcquirePackage("${Constants.PACKAGE_BASE_NAME}.")
                .build()
        }

        dependencies.sandboxManager.getAndroidSandbox(
            instrumentationConfiguration,
            sdk,
            configuration.get(ResourcesMode.Mode::class.java),
            configuration.get(LooperMode.Mode::class.java),
            configuration.get(SQLiteMode.Mode::class.java),
            configuration.get(GraphicsMode.Mode::class.java)
        ).also {
            configurator.configureSandbox(it, config, sdk) // uses: config.shadows
        }
    }

    /**
     * Returns a Roboelectric sandbox-instrumented Java class for [suiteClass].
     */
    @Suppress("UNCHECKED_CAST")
    fun instrumentedClass(suiteClass: KClass<out RoboelectricTestSuiteContent>): Class<RoboelectricTestSuiteContent> =
        sandbox.robolectricClassLoader.loadClass(suiteClass.qualifiedName) as Class<RoboelectricTestSuiteContent>

    private var applicationLifecycleTestElement: TestElement? = null
    val applicationLifecycleStarted get() = applicationLifecycleTestElement != null

    /**
     * Executes [action] in a Roboelectric sandbox test environment with a fresh application setup.
     */
    suspend fun withApplicationLifecycle(testElement: TestElement, action: suspend () -> Unit) {
        require(!applicationLifecycleStarted) {
            "Cannot start an application lifecycle in $testElement." +
                " A lifecycle has already been started by $applicationLifecycleTestElement."
        }
        applicationLifecycleTestElement = testElement

        try {
            val testEnvironment = sandbox.testEnvironment

            onSandboxMainThread {
                testEnvironment.setUpApplicationState("${testElement.testElementPath}", configuration, manifest)
            }

            try {
                withSandboxContextClassLoader {
                    action()
                }
            } finally {
                onSandboxMainThread {
                    testEnvironment.tearDownApplication()
                    testEnvironment.resetState()
                }
            }
        } finally {
            applicationLifecycleTestElement = null
        }
    }

    fun onSandboxMainThread(action: () -> Unit) {
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
