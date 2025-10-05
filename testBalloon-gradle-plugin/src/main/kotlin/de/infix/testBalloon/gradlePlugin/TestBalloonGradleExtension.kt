package de.infix.testBalloon.gradlePlugin

open class TestBalloonGradleExtension {
    var debugLevel: DebugLevel = DebugLevel.NONE

    /**
     * `jvmStandalone = true` uses a suspending `main` function to start tests on the JVM. For testing only.
     *
     * Otherwise, the framework will start up as a JUnit Platform test engine on the JVM.
     */
    var jvmStandalone: Boolean = false
}

enum class DebugLevel {
    NONE,
    BASIC,
    DISCOVERY,
    CODE
}
