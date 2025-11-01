package com.example

import de.infix.testBalloon.framework.core.testSuite
import kotlin.math.max
import kotlin.math.min
import kotlin.test.assertEquals

// Nest test suites as you like, inheriting configurations and coroutine context.
// Nesting is supported on all platforms, including Kotlin/JS and Kotlin/Wasm with Node.js and browser runtimes.

// Use a val property to register a top-level test suite.
val Nesting by testSuite {
    test("string length") {
        assertEquals(8, "Test me!".length)
    }

    // Use a function call to register a nested test suite.
    testSuite("integer operations") {
        test("max") {
            assertEquals(5, max(5, 3))
        }

        test("min") {
            assertEquals(3, min(5, 3))
        }
    }
}
