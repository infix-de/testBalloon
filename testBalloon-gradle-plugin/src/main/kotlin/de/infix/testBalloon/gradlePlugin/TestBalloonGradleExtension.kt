@file:OptIn(TestBalloonInternalApi::class)

package de.infix.testBalloon.gradlePlugin

import de.infix.testBalloon.framework.shared.internal.DebugLevel
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi

open class TestBalloonGradleExtension {
    /**
     * The list of custom environment variable names used to control testing.
     *
     * Example:
     * ```
     * environmentVariables.add("TEST_TAGS", "CI")
     * ```
     */
    val environmentVariables: MutableList<String> = mutableListOf()

    /** The framework-internal debug level. */
    var debugLevel: DebugLevel = DebugLevel.NONE

    /**
     * `jvmStandalone = true` uses a suspending `main` function to start tests on the JVM. For testing only.
     *
     * Otherwise, the framework will start up as a JUnit Platform test engine on the JVM.
     */
    var jvmStandalone: Boolean = false
}
