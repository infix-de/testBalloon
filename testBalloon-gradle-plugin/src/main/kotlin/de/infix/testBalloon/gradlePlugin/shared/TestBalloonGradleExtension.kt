package de.infix.testBalloon.gradlePlugin.shared

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

    /** The framework-internal debug level. */
    @TestBalloonInternalApi
    var debugLevel: DebugLevel = DebugLevel.NONE
}
