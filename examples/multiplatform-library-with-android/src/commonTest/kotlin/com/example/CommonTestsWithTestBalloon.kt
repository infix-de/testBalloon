package com.example

import de.infix.testBalloon.framework.testSuite

val CommonTestsWithTestBalloon by testSuite {
    test("expected to pass") {
        check(4 == 2 + 2)
    }

    test("expected to fail") {
        check(5 == 2 + 2)
    }

    testSuite("Nested Suite") {
        test("expected to pass") {
            check(4 == 2 + 2)
        }

        test("expected to fail") {
            check(5 == 2 + 2)
        }
    }
}
