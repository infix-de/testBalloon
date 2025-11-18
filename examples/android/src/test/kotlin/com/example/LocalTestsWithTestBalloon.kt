package com.example

import de.infix.testBalloon.framework.core.TestBalloonExperimentalApi
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.core.testWithJUnit4Rule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

val TestsWithTestBalloon by testSuite {
    val testMethodLoggingRule = TestMethodLoggingRule()

    fun TestSuite.test2(name: String, action: suspend () -> Unit) =
        @OptIn(TestBalloonExperimentalApi::class)
        this.testWithJUnit4Rule(name, testMethodLoggingRule) { action() }

    test2("expected to pass") {
        println("inside $testElementPath")
        check(4 == 2 + 2)
    }

    test2("expected to fail") {
        check(5 == 2 + 2)
    }

    testSuite("Nested Suite") {
        test2("expected to pass") {
            println("inside $testElementPath")
            check(4 == 2 + 2)
        }

        test2("expected to fail") {
            check(5 == 2 + 2)
        }
    }
}

class TestMethodLoggingRule : TestRule {
    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            println("Before test: $description")
            try {
                base.evaluate()
            } finally {
                println("After test: $description")
            }
        }
    }
}
