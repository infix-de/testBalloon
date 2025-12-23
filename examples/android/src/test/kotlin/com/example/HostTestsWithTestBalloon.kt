package com.example

import de.infix.testBalloon.framework.core.JUnit4RulesContext
import de.infix.testBalloon.framework.core.TestBalloonExperimentalApi
import de.infix.testBalloon.framework.core.testSuite
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

val TestsWithTestBalloon by testSuite {
    testFixture {
        @OptIn(TestBalloonExperimentalApi::class)
        object : JUnit4RulesContext() {
            init {
                rule(TestMethodLoggingRule())
            }
        }
    } asContextForEach {
        test("expected to pass") {
            println("inside $testElementPath")
            check(4 == 2 + 2)
        }

        test("expected to fail") {
            check(5 == 2 + 2)
        }

        testSuite("Nested Suite") {
            test("expected to pass") {
                println("inside $testElementPath")
                check(4 == 2 + 2)
            }

            test("expected to fail") {
                check(5 == 2 + 2)
            }
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
