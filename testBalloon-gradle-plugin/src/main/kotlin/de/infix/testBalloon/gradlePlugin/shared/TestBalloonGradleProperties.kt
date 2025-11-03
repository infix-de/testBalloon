@file:OptIn(TestBalloonInternalApi::class)

package de.infix.testBalloon.gradlePlugin.shared

import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import org.gradle.api.Project
import kotlin.reflect.KProperty

internal class TestBalloonGradleProperties(val project: Project) {

    /** Name pattern for test root source sets which will receive generated entry point code. */
    val testRootSourceSetRegex by regexProperty(
        """^(test$|commonTest$|androidTest|androidInstrumentedTest)"""
    )

    /**
     * Name pattern for test compilations in which the compiler plugin will look up test suites and a test session.
     *
     * The Gradle plugin will only apply the compiler plugin for compilations matching this pattern.
     */
    val testCompilationRegex by regexProperty("""(^test)|Test""")

    /**
     * Name pattern for test runtime-only configurations which will receive a JUnit Platform launcher dependency.
     *
     * NOTE: Android local/host tests do not use JUnit Platform, yet they require a dependency on TestBalloon's JVM
     * artifact. As this introduces an (unused) JUnit Platform dependency, we must provide a launcher in the
     * 'androidHostTestRuntimeClasspath' configuration.
     */
    val junitPlatformLauncherDependentConfigurationRegex by regexProperty(
        """^(test|jvmTest)RuntimeOnly|androidHostTestRuntimeClasspath$"""
    )

    /**
     * Name pattern for test modules in which the compiler plugin will look up test suites and a test session.
     *
     * The Compiler plugin will disable itself for modules not matching this pattern.
     */
    val testModuleRegex by stringProperty("""(_test|Test)$""")

    /**
     * Name pattern for browser-based test tasks using Karma.
     *
     * These tasks require a Karma configuration to forward (emulated) environment variables to the browser.
     */
    val browserTestTaskRegex by regexProperty("""BrowserTest$""")

    /**
     * Test reporting mode. One of `auto` (default), `intellij`, `files`.
     *
     * The mode `intellij` supplies full test element paths to the reporting infrastructure, supporting proper
     * hierarchy display in IntelliJ's test run window.
     *
     * The mode `files` supplies test element names instead of full paths, supporting proper XML and HTML report
     * files, avoiding duplicate path elements leading to `file name too long' errors.
     *
     * `auto` detects whether tests run under IntelliJ IDEA and chooses the mode accordingly.
     */
    val reportingMode by stringProperty("auto")

    /**
     * Setting to enable or disable file-based reports. One of `auto` (default), `true`, `false`.
     *
     * The `auto` setting disables reports if tests run under IntelliJ or the [reportingMode] is `intellij`.
     */
    val reportsEnabled by booleanProperty("auto")

    /**
     * Maximum length of the test element path in reporting. Defaults to a platform-specific limit if empty.
     *
     * Use this to solve platform reporting limitations ("path too long").
     */
    val reportingPathLimit by intProperty("")

    /**
     * Setting to enable resetting Gradle test filtering when executing TestBalloon-recognized test tasks.
     *
     * If enabled, this prevents using the original patterns to filter in ways which are incompatible with
     * TestBalloon's own include/exclude patterns on JS (by Mocha) and the JVM (by JUnit Platform).
     * This might be disabled if other test frameworks exist in the same module alongside TestBalloon, and the
     * resetting interferes with their expectations.
     */
    val testFilteringResetEnabled by booleanProperty("true")

    /**
     * A regex pattern of environment variable names which are safe to export into browser's simulated environments.
     */
    val browserSafeEnvironmentPattern by stringProperty("")

    @Suppress("SameParameterValue")
    private fun stringProperty(default: String) = Delegate(default) { it }

    private fun regexProperty(default: String) = Delegate(default) { Regex(it) }

    @Suppress("SameParameterValue")
    private fun intProperty(default: String) = Delegate(default) { it.toIntOrNull() }

    @Suppress("SameParameterValue")
    private fun booleanProperty(default: String) = Delegate(default) { it.toBooleanStrictOrNull() }

    inner class Delegate<Result>(val default: String, val conversion: (String) -> Result) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Result = conversion(
            project.findProperty("${Constants.GRADLE_PROPERTY_PREFIX}.${property.name}")?.toString() ?: default
        )
    }
}
