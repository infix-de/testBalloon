package de.infix.testBalloon.framework.core

import de.infix.testBalloon.framework.core.internal.FrameworkTestUtilities
import de.infix.testBalloon.framework.core.internal.integration.TestBalloonJUnit4Runner
import de.infix.testBalloon.framework.core.internal.integration.ThrowingTestSetupReport
import de.infix.testBalloon.framework.core.internal.integration.newPlatformDescription
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

class JUnit4TestSuiteTests {
    @Test
    fun fixtureWithTestRule() = try {
        FrameworkTestUtilities.withTestFramework {
            val trace = FrameworkTestUtilities.ConcurrentList<String>()

            class TestMethodLoggingRule : TestRule {
                override fun apply(base: Statement, description: Description) = object : Statement() {
                    override fun evaluate() {
                        log("Before test: $description")
                        try {
                            base.evaluate()
                        } finally {
                            log("After test: $description")
                        }
                    }
                }

                fun log(message: String) = trace.add(message)
            }

            val suite1 by testSuite(propertyFqn = "suite1") {
                testFixture {
                    object : JUnit4RulesContext() {
                        val myRule = rule(TestMethodLoggingRule())
                    }
                } asContextForEach {
                    test("test 1") {
                        myRule.log("from test 1")
                    }
                }
            }

            val suite2 by testSuite(propertyFqn = "suite2") {
                testFixture {
                    JUnit4RulesContext() // A context without rules should work just fine
                } asContextForEach {
                    test("test 1") {
                        trace.add("no rule from test 1")
                    }
                }
            }

            listOf(suite1, suite2) // Make suites register with the TestSession.
            TestSession.global.setUp(TestElement.AllInSelection, ThrowingTestSetupReport())
            TestSession.global.newPlatformDescription()

            FrameworkTestUtilities.withTestReport(suite1, suite2, invokeSetup = false) { frameworkFailure ->
                assertNull(frameworkFailure)
                finishedTestEvents().assertAllSucceeded()
                assertContentEquals(
                    listOf(
                        "Before test: test 1(suite1)",
                        "from test 1",
                        "After test: test 1(suite1)",
                        "no rule from test 1"
                    ),
                    trace.elements()
                )
            }
        }
    } finally {
        TestBalloonJUnit4Runner.reset()
    }
}
