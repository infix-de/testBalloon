package de.infix.testBalloon.integration.robolectric

import android.app.Application
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestElement
import de.infix.testBalloon.framework.core.parameter
import de.infix.testBalloon.integration.robolectric.internal.RobolectricContext
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import kotlin.reflect.KClass

/**
 * Returns a test configuration specifying a [RobolectricSettings] for a [TestElement] hierarchy.
 *
 * Settings configured as part of a [robolectricTestSuite] affect that suite directly.
 * Child elements inherit these settings, unless configured otherwise.
 */
public fun TestConfig.robolectric(settings: RobolectricSettings.() -> Unit): TestConfig =
    parameter(RobolectricContext.Key) { inheritedContext ->
        val inheritedSettings = inheritedContext?.settings
        val settings = RobolectricSettings(
            portableClasses = inheritedSettings?.portableClasses?.toMutableSet() ?: mutableSetOf(),
            portablePackages = inheritedSettings?.portablePackages?.toMutableSet() ?: mutableSetOf(),
            conscryptMode = inheritedSettings?.conscryptMode,
            applicationLifetime = inheritedSettings?.applicationLifetime
        ).apply { settings() }

        val inheritedConfig = inheritedContext?.config ?: RobolectricContext.defaultConfig
        val config = Config.Builder(inheritedConfig).overlay(settings.config()).build()

        RobolectricContext(testElement = this, config = config, settings = settings)
    }

/**
 * Settings for a Robolectric test suite.
 */
public class RobolectricSettings internal constructor(
    // region – Attributes inherited via Config.Builder.overlay() from a parent's Config.

    /** The Android SDK level to emulate. This value will also be set as Build.VERSION.SDK_INT. */
    public var sdk: Int? = null,

    /**
     * The default font scale. In U+, users will have a slider to determine font scale. In all
     * previous APIs, font scales are either small (0.85f), normal (1.0f), large (1.15f) or huge
     * (1.3f)
     */
    public var fontScale: Float? = null,

    /**
     * The [Application] class to use in the test, this takes precedence over any
     * application specified in the AndroidManifest.xml.
     */
    public var application: KClass<out Application>? = null,

    /**
     * Qualifiers specifying device configuration and resource resolution, such as "fr-normal-port-hdpi".
     *
     * If the string is prefixed with '+', the qualifiers that follow modify more broadly-scoped qualifiers
     * instead of a narrow-scoped qualifier replacing the broadly-scoped qualifier with defaults filled in.
     *
     * @see <a href="https://robolectric.org/device-configuration">Device Configuration</a> for details.
     */
    public var qualifiers: String? = null,

    /**
     * A set of shadow classes to enable, in addition to those that are already present.
     */
    public val shadows: MutableSet<KClass<*>> = mutableSetOf(),

    /**
     * A set of instrumented packages to enable, in addition to those that are already present.
     */
    public val instrumentedPackages: MutableSet<String> = mutableSetOf(),

    // endregion

    // region – Attributes inherited by direct initialization from a parent's RobolectricSettings.

    /**
     * Classes specified to be portable between Robolectric and the outside JVM.
     *
     * Such classes will be excluded from being instrumented by Robolectric. The set includes classes inherited
     * from a parent's [RobolectricSettings]. It may be extended, or cleared and populated anew.
     */
    public val portableClasses: MutableSet<KClass<*>>,

    /**
     * Packages whose declarations are specified to be portable between Robolectric and the outside JVM.
     *
     * Such packages will be excluded from being instrumented by Robolectric. The set includes packages inherited
     * from a parent's [RobolectricSettings]. It may be extended, or cleared and populated anew.
     */
    public val portablePackages: MutableSet<String>,

    /**
     * The mode of choosing a security provider (can be used to prefer Bouncy Castle over the default Conscrypt).
     *
     * @see <a href="https://robolectric.org/configuring/#conscryptmode">ConscryptMode</a> for details.
     */
    public var conscryptMode: ConscryptMode.Mode?,

    /**
     * The lifetime of an application (the default is per test).
     */
    public var applicationLifetime: ApplicationLifetime?

    // endregion
) {
    internal fun config(): Config = Config.Builder().also { builder ->
        fun <Value : Any> Value?.ifSet(set: (Value) -> Unit) {
            this?.let { set(it) }
        }

        sdk.ifSet { builder.setSdk(it) }
        fontScale.ifSet(builder::setFontScale)
        application.ifSet { builder.setApplication(it.java) }
        qualifiers.ifSet(builder::setQualifiers)
        shadows.ifSet { shadows -> builder.setShadows(*shadows.map { it.java }.toTypedArray()) }
        instrumentedPackages.ifSet { builder.setInstrumentedPackages(*it.toTypedArray()) }
    }.build()
}

/**
 * The lifetime an application.
 */
public enum class ApplicationLifetime {
    /**
     * The application's lifetime is the execution of a single test.
     *
     * Each test executes with a freshly set up application.
     */
    Test,

    /**
     * The application's lifetime is the execution of a Robolectric test suite.
     *
     * All tests of a Robolectric test suite execute in a shared application.
     *
     * **Note:** Non-Robolectric test suites do not control the application's lifetime.
     */
    RobolectricTestSuite
}
