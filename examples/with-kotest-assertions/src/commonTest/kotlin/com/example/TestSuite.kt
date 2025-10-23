package com.example

import de.infix.testBalloon.framework.TestSession
import de.infix.testBalloon.framework.shared.AbstractTestElement
import de.infix.testBalloon.framework.testSuite
import de.infix.testBalloon.integration.kotest.assertions.kotestAssertionsSupport
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.string.shouldNotEndWith

class MyTestSession : TestSession(testConfig = DefaultConfiguration.kotestAssertionsSupport())

val TestSuite by testSuite {
    log("configuring $testElementPath")

    test("test1") {
        log("in $testElementPath, coroutineContext=$coroutineContext")
        "This test should fail?" shouldBeEqual "This test should fail!"
    }

    testSuite("inner suite") {
        test("test2") {
            log("in $testElementPath")
        }

        test("test3") {
            log("in $testElementPath")
            assertSoftly {
                "Expect failure 1!" shouldNotEndWith "!"
                "Expect failure 2!" shouldNotEndWith "!"
            }
        }
    }
}

private fun AbstractTestElement.log(message: String) {
    println("$testElementPath: $message\n")
}
