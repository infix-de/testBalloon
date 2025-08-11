package de.infix.testBalloon.gradlePlugin

open class GradleExtension {
    var debugLevel: DebugLevel = DebugLevel.NONE
    var jvmStandalone: Boolean = false
    var testCompilationRegex: String = """(^test)|Test"""
    var testModuleRegex: String = """(_test|Test)$"""
}

enum class DebugLevel {
    NONE,
    BASIC,
    DISCOVERY,
    CODE
}
