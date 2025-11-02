package com.example

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.aroundEach
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
        testSuite("suite 1.1") {
            test("test s1.1-1") {
            }
            test("test s1.1-2") {
            }
        }
    }
}

val DisplayNameSuite by testSuite(
    displayName = "display d-DisplayNameSuite-d",
    testConfig = TestConfig.logTestExecution()
) {
    test("test 1", displayName = "display test d-1-d") {
    }
    testSuite("suite 1", displayName = "display suite d-1-d") {
        test("test s1-1", displayName = "display test d-s1-1-d") {
        }
    }
}

private fun TestConfig.logTestExecution() = aroundEach { testElementAction ->
    testElementAction()
    if (this is Test) {
        println("##LOG(${testPlatform.displayName} – $testElementPath: OK)LOG##")
    }
}
