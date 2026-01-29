package com.example

import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.core.testSuite

val EnvironmentSuite by testSuite {
    test("print") {
        for (name in listOf("TEST_ONE", "CUSTOM_ONE", "CUSTOM_TWO")) {
            if (testPlatform.environment(name) != null) println("##LOG($name)LOG##")
        }
    }
}
