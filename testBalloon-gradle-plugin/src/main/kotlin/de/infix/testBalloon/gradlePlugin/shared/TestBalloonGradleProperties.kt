@file:OptIn(TestBalloonInternalApi::class)

package de.infix.testBalloon.gradlePlugin.shared

import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import org.gradle.api.Project
import kotlin.reflect.KProperty

internal class TestBalloonGradleProperties(val project: Project) {

    /**
     * Name pattern for source sets in which the compiler plugin will look up test suites and a test session.
     *
     * The Gradle plugin will only apply the compiler plugin for compilations of source sets matching this pattern.
     *
     * Naming of test-related source sets created by various Gradle plugins, (`*` marks source sets containing utility
     * code, not tests):
     * - `kotlin("multiplatform")`: ends with `Test`
     * - `kotlin("jvm")`: `test`
     * - `com.android.application` (AGP 8.13.2):
     *     - default:
     *         - `androidTest`, `androidTestDebug`, `androidTestRelease`,
     *           `debugAndroidTest`, `debugUnitTest`, `releaseUnitTest`,
     *           `test`, `testDebug`, `testRelease`
     *         - also: `testFixtures` *, `testFixturesDebug` *, `testFixturesRelease` *
     *     - with a single flavor dimension and flavor `prod`, additionally:
     *         - `androidTestProd`, `androidTestProdDebug`,
     *           `prodDebugAndroidTest`, `prodDebugUnitTest`, `prodReleaseUnitTest`,
     *           `testProd`, `testProdDebug`, `testProdRelease`
     *         - also: `testFixturesProd` *
     * - `com.android.kotlin.multiplatform.library` (AGP 8.13.2): ends with `Test` (like Kotlin/Multiplatform)
     * - `java-test-fixtures`: `testFixtures` *
     *
     * IMPLEMENTATION NOTES: Before using this value, ensure that all plugins are applied to the project.
     */
    val testSourceSetsRegex by testsOnlyProjectDependentRegexProperty(
        defaultValue = """[tT]est(?!Fixtures)""",
        testsOnlyProjectValue = { testSourceSetsRegexTestsOnlyProject }
    )
    private val testSourceSetsRegexTestsOnlyProject by stringProperty(".*")

    /**
     * Name pattern for test source sets in which the Gradle plugin will disable incremental compilation.
     * This pattern will only be used on source sets matching [testSourceSetsRegex].
     *
     * A non-matching pattern (like `^---NONE---$`) indicates that incremental compilation can be left enabled
     * for all source sets.
     *
     * IMPLEMENTATION NOTE: Before using this value, ensure that all plugins are applied to the project.
     */
    val notIncrementallyCompilableTestSourceSetsRegex by regexProperty("""^---NONE---$""")

    /**
     * Name pattern for test runtime-only configurations which will receive a JUnit Platform launcher dependency.
     *
     * This extra launcher dependency is required per the following Gradle docs:
     * - https://docs.gradle.org/8.3/userguide/upgrading_version_8.html#manually_declaring_dependencies
     * - https://docs.gradle.org/current/userguide/java_testing.html#sec:java_testing_basics
     * - https://docs.gradle.org/current/userguide/java_testing.html#test_process_starts_but_test_dependencies_are_missing
     * (The launcher dependency is not required for Gradle JVM Test Suites, but we don't differentiate between those
     * and other test-only project plugins.)
     *
     * IMPLEMENTATION NOTE: Before using this value, ensure that all plugins are applied to the project.
     */
    val junitPlatformLauncherDependentConfigurationRegex by testsOnlyProjectDependentRegexProperty(
        defaultValue = """^(test|jvmTest)RuntimeOnly$""",
        testsOnlyProjectValue = { junitPlatformLauncherDependentConfigurationRegexTestsOnlyProject }
    )
    private val junitPlatformLauncherDependentConfigurationRegexTestsOnlyProject by stringProperty(
        """RuntimeOnly$"""
    )

    /**
     * Comma-separated list of Gradle plugins which use all source sets for tests.
     *
     * If one of those plugins is present in a project, TestBalloon will cover all compilations and all source sets.
     */
    private val testsOnlyProjectPlugins by stringProperty("jvm-test-suite,com.android.test")

    /**
     * Returns true if [project] is a tests-only project which uses all source sets for tests.
     *
     * IMPLEMENTATION NOTE: Before using this value, ensure that all plugins are applied to the project.
     */
    private val isTestsOnlyProject by lazy {
        testsOnlyProjectPlugins.split(',').any { project.pluginManager.hasPlugin(it) }
    }

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
     * Setting to enable or disable Gradle autoconfiguration for JUnit Platform. `true` (default) or `false`.
     */
    val junitPlatformGradleAutoConfigurationEnabled by booleanProperty("true")

    /**
     * Setting to enable or disable Gradle autoconfiguration for JUnit 4. `true` (default) or `false`.
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
    val browserSafeEnvironmentPattern by stringProperty("^(CI|TEST.*)$")

    /**
     * A regex pattern of environment variable names which are safe to export into an Apple simulator's environments.
     */
    val simulatorSafeEnvironmentPattern by stringProperty("^(CI|TEST.*)$")

    @Suppress("SameParameterValue")
    private fun stringProperty(default: String) = Delegate(default) { it }

    private fun regexProperty(default: String) = Delegate(default) { Regex(it) }

    @Suppress("SameParameterValue")
    private fun testsOnlyProjectDependentRegexProperty(defaultValue: String, testsOnlyProjectValue: () -> String) =
        Delegate(defaultValue) {
            Regex(if (isTestsOnlyProject) testsOnlyProjectValue() else it)
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
