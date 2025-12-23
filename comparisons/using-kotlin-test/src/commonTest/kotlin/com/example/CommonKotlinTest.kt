package com.example

import kotlin.test.Test
import kotlin.test.assertEquals

class CommonKotlinTest {
    @Test
    fun test1() {
        log("in CommonKotlinTest.test1")
        assertEquals("This test should fail!", "This test should fail?")
    }

    @Test
    fun test2() {
        log("in CommonKotlinTest.test2")
    }
}

private fun log(message: String) {
    println("$message\n")
}
