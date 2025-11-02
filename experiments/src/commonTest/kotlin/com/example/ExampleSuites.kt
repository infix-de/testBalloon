package com.example

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.aroundEach
import de.infix.testBalloon.framework.core.disable
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.core.testSuite

val SimpleSuite by testSuite(testConfig = TestConfig.logTestExecution()) {
    test("test 1") {
    }
    testSuite("suite 1") {
        test("test s1-1") {
        }
        test("test s1-2") {
        }
        testSuite("suite 1.1", testConfig = TestConfig.onlyTagged(MyTag.CI, MyTag.SimulatedCI)) {
            test("test s1.1-1") {
            }
            test("test s1.1-2") {
            }
        }
    }
}

private fun TestConfig.logTestExecution() = aroundEach { testElementAction ->
    testElementAction()
    if (this is Test) {
        println("##LOG(${testPlatform.displayName} – $testElementPath: OK)LOG##")
    }
}

fun TestConfig.onlyTagged(vararg tags: MyTag) = if (tags.any { it.exists() }) this else disable()

enum class MyTag {
    CI,
    SimulatedCI,
    Release;

    fun value() = testPlatform.environment("TEST_TAGS")?.split(',')?.last { it == name }

    fun exists() = value() != null
}
