package com.example

import org.junit.Assert
import org.junit.Test

class TestClassUsingJUnit4 {
    @Test
    fun expected_to_pass() {
        Assert.assertEquals(4, 2 + 2)
    }

    @Test
    fun expected_to_fail() {
        Assert.assertEquals(5, 2 + 2)
    }
}
