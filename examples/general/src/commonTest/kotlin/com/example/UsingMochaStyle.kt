package com.example

import com.example.testLibrary.describe
import com.example.testLibrary.it
import kotlin.math.max
import kotlin.math.min
import kotlin.test.assertEquals

val UsingMochaStyle by describe {
    it("Length of 'Test me!' is 8") {
        assertEquals(8, "Test me!".length)
    }

    describe("Integer operations") {
        it("max(5, 3) returns 5") {
            assertEquals(5, max(5, 3))
        }

        describe("Variants of min") {
            repeat(3) {
                it("min(5, $it) returns $it") {
                    assertEquals(it, min(5, it))
                }
            }
        }
    }
}
