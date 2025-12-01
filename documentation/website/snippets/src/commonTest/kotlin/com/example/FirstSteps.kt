package com.example

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals

// --8<-- [start:my-first-test-suite]
val MyFirstTestSuite by testSuite {
    test("string length") {
        assertEquals(8, "Test me!".length)
    }
}
// --8<-- [end:my-first-test-suite]
