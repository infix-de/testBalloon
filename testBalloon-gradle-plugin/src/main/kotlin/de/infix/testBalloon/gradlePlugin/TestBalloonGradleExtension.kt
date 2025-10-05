@file:OptIn(TestBalloonInternalApi::class)

package de.infix.testBalloon.gradlePlugin

import de.infix.testBalloon.framework.internal.DebugLevel
import de.infix.testBalloon.framework.internal.TestBalloonInternalApi

open class TestBalloonGradleExtension {
    var debugLevel: DebugLevel = DebugLevel.NONE

    /**
     * `jvmStandalone = true` uses a suspending `main` function to start tests on the JVM. For testing only.
     *
     * Otherwise, the framework will start up as a JUnit Platform test engine on the JVM.
     */
    var jvmStandalone: Boolean = false
}
