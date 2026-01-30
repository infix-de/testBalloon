package de.infix.testBalloon.integration.robolectric.examples

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals

/**
 * Example of a test suite that does NOT use Robolectric.
 *
 * This suite runs as a standard JVM test without any Android framework dependencies.
 * It demonstrates that non-Robolectric tests remain completely unaffected by the
 * Robolectric integration.
 */
val ExampleWithoutRobolectric by testSuite {
    test("simple arithmetic") {
        assertEquals(4, 2 + 2)
    }

    test("string concatenation") {
        val result = "Hello" + " " + "World"
        assertEquals("Hello World", result)
    }

    testSuite("nested suite") {
        test("list operations") {
            val list = listOf(1, 2, 3)
            assertEquals(3, list.size)
            assertEquals(6, list.sum())
        }
    }
}
