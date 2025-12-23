package com.example

import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.AbstractTestElement
import kotlin.test.assertEquals

val CommonTestBalloon by testSuite {
    log("configuring $testElementPath")

    test("test1") {
        log("in $testElementPath")
        assertEquals("This test should fail!", "This test should fail?")
    }

    testSuite("inner suite") {
        test("test2") {
            log("in $testElementPath")
        }
    }
}

private fun AbstractTestElement.log(message: String) {
    println("$testElementPath: $message\n")
}
