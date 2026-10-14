package de.infix.testBalloon.gradlePlugin

import de.infix.testBalloon.framework.shared.internal.DebugLevel
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi

open class TestBalloonGradleExtension {
    /**
     * A regex pattern of environment variable names which are safe to export into browser's simulated environments.
     *
     * If set, it overrides the Gradle property with the same name.
     *
     * Example:
     * ```
     * browserSafeEnvironmentPattern = "^(CI|TEST.*)$"
     * ```
     */
    var browserSafeEnvironmentPattern: String? = null

    /**
     * A regex pattern of environment variable names which are safe to export into an Apple simulator's environment.
     *
     * If set, it overrides the Gradle property with the same name.
     *
     * Example:
     * ```
     * simulatorSafeEnvironmentPattern = "^(CI|TEST.*)$"
     * ```
     */
    var simulatorSafeEnvironmentPattern: String? = null

    /**
     * `junit4AutoIntegrationEnabled` controls JUnit 4 auto-integration on the JVM.
     *
     * This property controls whether TestBalloon creates a JUnit 4 runner if JUnit 4 is on the classpath.
     * If disabled, the framework will only support JUnit Platform on the JVM.
     *
     * If set, it overrides the Gradle property with the same name.
     */
    var junit4AutoIntegrationEnabled: Boolean? = null

    /**
     * `browserAutoIntegrationEnabled` controls JS and Wasm auto-integration for browser targets.
     *
     * This property controls whether TestBalloon automatically enables itself for tests on JS and Wasm browser
     * targets.
     *
     * If set, it overrides the Gradle property with the same name.
     */
    var browserAutoIntegrationEnabled: Boolean? = null

    /**
     * `jsTestFilteringResetEnabled` controls resetting Gradle test filtering for Kotlin/JS test tasks.
     *
     * If enabled, this prevents Mocha from seeing incompatible TestBalloon patterns.
     *
     * If set, it overrides the Gradle property with the same name.
     */
    var jsTestFilteringResetEnabled: Boolean? = null

    /**
     * Names of JS and JS/Wasm browser targets with Playwright integration enabled.
     */
    @TestBalloonInternalApi
    val jsBrowserTargetsUsingPlaywrightIntegration: MutableSet<String> = mutableSetOf()

    /** The debug level controlling reporting by the Gradle and compiler plugin. */
    var debugLevel: DebugLevel = DebugLevel.NONE
}
