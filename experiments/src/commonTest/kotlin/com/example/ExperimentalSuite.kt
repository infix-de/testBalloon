package com.example

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.aroundEach
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.TestElementName
import de.infix.testBalloon.framework.shared.TestRegistering

val ExperimentalSuite1 by testSuite(testConfig = TestConfig.logTestExecution()) {
    test("test 1") {
    }
}

val ExperimentalSuite2 by myTopLevelSuite()

@TestRegistering
fun myTopLevelSuite(@TestElementName name: String = "") =
    testSuite(name = name, testConfig = TestConfig.logTestExecution()) {
        test("test 1") {
        }
    }

private fun TestConfig.logTestExecution() = aroundEach { testElementAction ->
    testElementAction()
    if (this is Test) {
        println("##LOG(${testPlatform.displayName} – $testElementPath: OK)LOG##")
    }
}
