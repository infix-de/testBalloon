package com.example

import de.infix.testBalloon.framework.core.testSuite
import kotlin.math.max
import kotlin.math.min
import kotlin.test.assertEquals

val Nesting by testSuite {
    test("string length") {
        assertEquals(8, "Test me!".length)
    }

    testSuite("integer operations") {
        test("max") {
            assertEquals(5, max(5, 3))
        }

        test("min") {
            assertEquals(3, min(5, 3))
        }
    }
}
