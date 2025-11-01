package com.example

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals

// Use plain Kotlin to dynamically create a set of data-driven tests.
val Dynamic by testSuite {
    val testCases = mapOf(
        "one" to 3,
        "two" to 3,
        "three" to 5
    )

    for ((string, expectedLength) in testCases) {
        test("length of '$string' is $expectedLength") {
            assertEquals(expectedLength, string.length)
        }
    }
}
