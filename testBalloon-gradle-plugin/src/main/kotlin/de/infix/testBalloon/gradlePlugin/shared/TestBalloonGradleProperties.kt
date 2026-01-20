@file:OptIn(TestBalloonInternalApi::class)

package de.infix.testBalloon.gradlePlugin.shared

import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import org.gradle.api.Project
import kotlin.reflect.KProperty

internal class TestBalloonGradleProperties(val project: Project) {

    /** Name pattern for test root source sets which will receive generated entry point code. */
    val testRootSourceSetRegex by gradleTestSuiteEnhancedRegexProperty(
        """^test$|^commonTest$|^androidTest|^androidInstrumentedTest|AndroidTest$|UnitTest$"""
    )

    /**
     * Name pattern for test compilations in which the compiler plugin will look up test suites and a test session.
     *
     * The Gradle plugin will only apply the compiler plugin for compilations matching this pattern.
     */
    val testCompilationRegex by gradleTestSuiteEnhancedRegexProperty("""(^test)|Test""")

    /**
     * Name pattern for test compile tasks in which the compiler plugin will disable incremental compilation.
     *
     * WORKAROUND: IC in Kotlin < 2.3.20-Beta1 does not support compiler plugins generating top-level declarations
     */
    val nonIncrementalTestCompileTaskRegex by regexProperty("""^compileTestKotlin""")

    /**
     * Name pattern for test runtime-only configurations which will receive a JUnit Platform launcher dependency.
     */
    val junitPlatformLauncherDependentConfigurationRegex by regexProperty(
        """^(test|jvmTest)RuntimeOnly$"""
    )

    /**
     * Name pattern for test modules in which the compiler plugin will look up test suites and a test session.
     *
     * The Compiler plugin will disable itself for modules not matching this pattern.
     */
    val testModuleRegex by gradleTestSuiteEnhancedStringProperty("""(^android|_test|Test)$""")

    /**
     * Name pattern for Gradle JVM Test Suites (incubating).
     *
     * The pattern will extend [testRootSourceSetRegex], [testCompilationRegex] and [testModuleRegex] to enable
     * TestBalloon tests in Gradle JVM Test Suites, which can have arbitrary names and would otherwise not
     * be recognized as test source sets, modules, or compilations.
     */
    val gradleTestSuiteNamesRegex by stringProperty("")

    /**
     * Name pattern for browser-based test tasks using Karma.
     *
     * These tasks require a Karma configuration to forward (emulated) environment variables to the browser.
     */
    val browserTestTaskRegex by regexProperty("""BrowserTest$""")

    /**
     * Name pattern for Android host-side (a.k.a. unit) test task classes.
     *
     * These tasks require Android-specific JUnit 4 integration instead of JUnit platform.
     */
    val androidHostSideTestClassRegex by regexProperty(
        """^com\.android\.build\.gradle\.tasks\.factory\.AndroidUnitTest$"""
    )

    /**
     * Setting to enable or disable Gradle auto-configuration for JUnit Platform. `true` (default) or `false`.
     */
    val junitPlatformGradleAutoConfigurationEnabled by booleanProperty("true")

    /**
     * Setting to enable or disable Gradle auto-configuration for JUnit 4. `true` (default) or `false`.
     */
    val junit4GradleAutoConfigurationEnabled by booleanProperty("true")

    /**
     * Setting to enable or disable JUnit 4 auto-integration for test modules. `true` (default) or `false`.
     *
     * This property controls whether TestBalloon creates a JUnit 4 runner if JUnit 4 is on the classpath.
     */
    val junit4AutoIntegrationEnabled by booleanProperty("true")

    /**
     * Setting to restrict JVM test runs to TestBalloon, excluding other test frameworks. `true` or `false` (default).
     */
    val jvmTestBalloonTestsOnly by booleanProperty("false")

    /**
     * Test reporting mode. One of `auto` (default), `intellij`, `files`, `intellij-legacy`.
     *
     * The `intellij` mode supplies test element coordinates to the reporting infrastructure, supporting the
     * IDE plugin versions >= 0.5 for proper hierarchy display in IntelliJ's test run window, and navigation to test
     * element sources.
     *
     * The `intellij-legacy` mode supplies full test element paths to the reporting infrastructure, supporting proper
     * hierarchy display in IntelliJ's test run window without IDE plugin support. It is only effective if
     * TestBalloon runs under IntelliJ IDEA.
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
     * Setting to enable resetting Gradle test filtering for Kotlin/JS test tasks.
     *
     * If enabled, this prevents using TestBalloon patterns which are incompatible with Mocha.
     */
    val jsTestFilteringResetEnabled by booleanProperty("true")

    /**
     * Setting to enable patching Gradle test filtering on JVM test tasks.
     *
     * If enabled, this prevents Gradle from interpreting TestBalloon patterns and prematurely declaring
     * "No tests found for given includes" without invoking any framework.
     */
    val jvmTestFilteringPatchEnabled by booleanProperty("true")

    /**
     * A regex pattern of environment variable names which are safe to export into browser's simulated environments.
     */
    val browserSafeEnvironmentPattern by stringProperty("")

    @Suppress("SameParameterValue")
    private fun stringProperty(default: String) = Delegate(default) { it }

    @Suppress("SameParameterValue")
    private fun gradleTestSuiteEnhancedStringProperty(default: String) = Delegate(default) {
        if (gradleTestSuiteNamesRegex.isEmpty()) it else "($it)|($gradleTestSuiteNamesRegex)"
    }

    private fun regexProperty(default: String) = Delegate(default) { Regex(it) }

    @Suppress("SameParameterValue")
    private fun gradleTestSuiteEnhancedRegexProperty(default: String) = Delegate(default) {
        Regex(if (gradleTestSuiteNamesRegex.isEmpty()) it else "($it)|($gradleTestSuiteNamesRegex)")
    }

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
