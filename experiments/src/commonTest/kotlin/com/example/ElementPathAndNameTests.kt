@file:OptIn(TestBalloonInternalApi::class)

package com.example

import de.infix.testBalloon.framework.TestConfig
import de.infix.testBalloon.framework.aroundEach
import de.infix.testBalloon.framework.internal.TestBalloonInternalApi
import de.infix.testBalloon.framework.internal.printlnFixed
import de.infix.testBalloon.framework.testSuite

/*
val Suite_Level_1 by testSuite(
    testConfig = TestConfig.aroundEach { elementAction ->
        printlnFixed("$testElementPath")
        elementAction()
    }
) {
    test("suite-level-1-test") {}
    test("blank suite-level-1-test-blank") {}
    test("dot.suite-level-1-test-dot") {}
    testSuite("suite-level-2") {
        test("suite-level-2-test") {}
        test("blank suite-level-2-test-blank") {}
        test("dot.suite-level-2-test-dot") {}
    }
    testSuite("blank suite-level-2-blank") {
        test("suite-level-2-blank-test") {}
    }
    testSuite("dot.suite-level-2-dot") {
        test("suite-level-2-dot-test") {}
    }
}
*/

val DisplayNames by testSuite(displayName = "dn") {
    testSuite("suite 1", displayName = "s1") {
        test("test 1", displayName = "t1") {
            printlnFixed("$testElementPath")
        }
        testSuite("suite 1.1", displayName = "s1.1") {
            test("test 1.1", displayName = "t1.1") {
                printlnFixed("$testElementPath")
            }
        }
    }
}

/*
val NameTests by testSuite {
    testSuite("accepted characters") {
        for (ordinal in 32..127) {
            val char = ordinal.toChar()
            test("ord=${ordinal.toString().padStart(5, '0')} `$char´") {}
        }
    }

    testSuite("x") {
        testSuite("l") {
            testSuite("lengths") {
                for (length in listOf(50, 100, 200, 400, 800, 1600)) {
                    test(length.toString().padStart(5, '0').padEnd(length, '-')) {
                        println("path: $testElementPath")
                        println("path length: ${testElementPath.toString().length}")
                    }
                }
            }
        }
    }
}
*/
