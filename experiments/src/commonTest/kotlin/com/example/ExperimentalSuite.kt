package com.example

import de.infix.testBalloon.framework.core.TestBalloonExperimentalApi
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSession
import de.infix.testBalloon.framework.core.aroundEachTest
import de.infix.testBalloon.framework.core.internal.LogLevel
import de.infix.testBalloon.framework.core.internal.testFrameworkLogLevel
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.core.testSuite
import kotlin.math.max
import kotlin.math.min
import kotlin.test.assertEquals

val ExperimentalSuite1 by testSuite(testConfig = TestConfig.logTestExecution()) {
    test("string length") {
        assertEquals(8, "Test me!".length)
    }

    val specials = "< > : \" / \\ | ? *"

    // Use a function call to register a nested test suite.
    testSuite("middle $specials middle") {
        test("max $specials max") {
            assertEquals(5, max(5, 3))
        }

        testSuite("lowest $specials lowest") {
            test("min $specials min") {
                assertEquals(3, min(5, 3))
            }
        }
    }
}

private fun TestConfig.logTestExecution() = aroundEachTest { testElementAction ->
    testElementAction()
    println("${testPlatform.displayName} – $testElementPath")
}

class ModuleTestSession : TestSession() {
    init {
        @OptIn(TestBalloonExperimentalApi::class)
        testFrameworkLogLevel = LogLevel.DEBUG
    }
}
