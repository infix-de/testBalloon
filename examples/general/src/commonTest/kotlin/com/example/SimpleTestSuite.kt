package com.example

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals

// Register a test suite in plain Kotlin, without annotations.
val Simple by testSuite {
    // Register a test.
    test("string length") {
        // Use the assertion library of your choice.
        assertEquals(8, "Test me!".length)
    }
}

// Choose a different display name (used on platforms supporting display names).
val SimpleWithASpecialName by testSuite(displayName = "Suite with a special name") {
    test("string length") {
        assertEquals(8, "Test me!".length)
    }
}
