package com.example

import kotlin.test.Test
import kotlin.test.assertEquals

class JvmTestsWithJUnitJupiter {
    @Test
    fun expected_to_pass() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun expected_to_fail() {
        assertEquals(5, 2 + 2)
    }
}
