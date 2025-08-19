package com.example

import de.infix.testBalloon.framework.testSuite
import org.junit.Assert

val TestsWithTestBalloon by testSuite {
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
