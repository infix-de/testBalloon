package com.example

import de.infix.testBalloon.framework.core.testSuite

val NestingCommon by testSuite {
    test("top 1") {
    }

    testSuite("middle") {
        test("middle 1") {
        }

        testSuite("lower") {
            test("lower 1") {
            }
        }
    }
}

val NestingCommon2 by testSuite("nesting common 2") {
    test("top 1") {
    }

    testSuite("middle") {
        test("middle 1") {
        }

        testSuite("lower") {
            test("lower 1") {
            }
        }
    }
}
