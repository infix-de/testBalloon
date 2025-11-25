@file:OptIn(TestBalloonInternalApi::class)

package de.infix.testBalloon.gradlePlugin

import de.infix.testBalloon.framework.shared.internal.DebugLevel
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi

open class TestBalloonGradleExtension {
    /**
     * A regex pattern of environment variable names which are safe to export into browser's simulated environments.
     *
     * Example:
     * ```
     * browserSafeEnvironmentPattern = "^CI|TEST.*$"
     * ```
     */
    var browserSafeEnvironmentPattern: String = ""

    /** The framework-internal debug level. */
    var debugLevel: DebugLevel = DebugLevel.NONE

    /**
     * `junit4AutoIntegrationEnabled` controls JUnit 4 auto-integration on the JVM.
     *
     * This property controls whether TestBalloon creates a JUnit 4 runner if JUnit 4 is on the classpath.
     * If disabled, the framework will only support JUnit Platform on the JVM.
     */
    var junit4AutoIntegrationEnabled: Boolean? = null
}
