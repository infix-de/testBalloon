package com.example

import de.infix.testBalloon.framework.core.internal.printlnFixed
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi

@OptIn(TestBalloonInternalApi::class)
val CommonTestSuite by testSuite {
    test("environment") {
        for (name in listOf("SHELL", "USERNAME")) {
            printlnFixed("$name=${testPlatform.environment(name)}")
        }
    }
}
