package com.example

import de.infix.testBalloon.framework.core.testSuite
import org.junit.Assert

val AndroidDeviceTestBalloon by testSuite {
    test("expected to pass") {
        Assert.assertEquals(4, 2 + 2)
    }

    test("expected to fail") {
        Assert.assertEquals(5, 2 + 2)
    }

    testSuite("Nested Suite") {
        test("expected to pass") {
            Assert.assertEquals(4, 2 + 2)
        }

        test("expected to fail") {
            Assert.assertEquals(5, 2 + 2)
        }
    }
}
