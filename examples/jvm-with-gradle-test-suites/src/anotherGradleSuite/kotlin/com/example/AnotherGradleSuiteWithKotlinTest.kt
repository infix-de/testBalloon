package com.example

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AnotherGradleSuiteWithKotlinTest {
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
