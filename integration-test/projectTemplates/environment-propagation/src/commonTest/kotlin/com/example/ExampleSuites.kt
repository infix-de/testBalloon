package com.example

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.aroundEach
import de.infix.testBalloon.framework.core.disable
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.core.testSuite

val SimpleSuite by testSuite(testConfig = TestConfig.logTestExecution()) {
    test("always") {
    }
    test("from property", testConfig = TestConfig.onEnvironmentVariable("FROM_PROPERTY")) {
    }
    test("from extension", testConfig = TestConfig.onEnvironmentVariable("FROM_EXTENSION")) {
    }
}

fun TestConfig.onEnvironmentVariable(name: String) = if (testPlatform.environment(name) == null) disable() else this

private fun TestConfig.logTestExecution() = aroundEach { testElementAction ->
    testElementAction()
    if (this is Test) {
        println("##LOG(${testPlatform.displayName} – $testElementPath: OK)LOG##")
    }
}
