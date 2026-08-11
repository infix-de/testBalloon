package com.example

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.core.testSuite

val SimpleSuite by testSuite {
    test("test 1") {
        println("##LOG(${testPlatform.displayName} – $testElementPath: OK)LOG##")
    }
}
