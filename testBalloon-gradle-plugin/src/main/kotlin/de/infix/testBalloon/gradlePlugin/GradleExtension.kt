package de.infix.testBalloon.gradlePlugin

open class GradleExtension {
    var debugLevel: DebugLevel = DebugLevel.NONE

    /**
     * `jvmStandalone = true` uses a suspending `main` function to start tests on the JVM. For testing only.
     *
     * Otherwise, the framework will start up as a JUnit Platform test engine on the JVM.
     */
    var jvmStandalone: Boolean = false

    /** Name pattern for test root source sets which will receive generated entry point code. */
    var testRootSourceSetRegex: String = """^(test${'$'}|commonTest${'$'}|androidTest|androidInstrumentedTest)"""

    /**
     * Name pattern for test compilations in which the compiler plugin will look up test suites and a test session.
     *
     * The Gradle plugin will not apply the compiler plugin for compilations not matching this pattern.
     */
    var testCompilationRegex: String = """(^test)|Test"""

    /**
     * Name pattern for test modules in which the compiler plugin will look up test suites and a test session.
     *
     * The Compiler plugin will disable itself for modules not matching this pattern.
     */
    var testModuleRegex: String = """(_test|Test)$"""
}

enum class DebugLevel {
    NONE,
    BASIC,
    DISCOVERY,
    CODE
}
