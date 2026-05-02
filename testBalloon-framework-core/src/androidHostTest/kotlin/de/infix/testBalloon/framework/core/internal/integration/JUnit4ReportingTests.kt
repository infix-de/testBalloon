package de.infix.testBalloon.framework.core.internal.integration

import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.TestElement
import de.infix.testBalloon.framework.core.TestElement.Selection
import de.infix.testBalloon.framework.core.TestSession
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.internal.FrameworkTestUtilities
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.internal.ReportingMode
import kotlinx.coroutines.test.TestResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.Description
import kotlin.test.Test

class JUnit4ReportingTests {
    @Test
    fun gradleFilesWithoutNesting() =
        descriptionTest(ReportingMode.GradleFilesWithoutNesting, expectationForGradleFilesWithoutNesting)

    @Test
    fun gradleIntellijIdeaLegacy() =
        descriptionTest(ReportingMode.GradleIntellijIdeaLegacy, expectationForGradleFilesWithoutNesting)

    @Test
    fun androidDevice() = descriptionTest(ReportingMode.AndroidDevice, expectationForGradleFilesWithoutNesting)

    @Test
    fun gradleFilesWithNesting() =
        descriptionTest(ReportingMode.GradleFilesWithNesting, expectationForGradleFilesWithNesting)

    @Test
    fun amper() = descriptionTest(ReportingMode.Amper, expectationForGradleFilesWithNesting)

    private val expectationForGradleFilesWithoutNesting =
        """
            TestSession
             c.e.topSuite
              t-test 1(c.e.topSuite)
              c.e.topSuite ↘ middle suite
               middle suite ↘ m-test 1(c.e.topSuite)
               c.e.topSuite ↘ middle suite ↘ maybe excluded suite
                middle suite ↘ maybe excluded suite ↘ e-test 1(c.e.topSuite)
               c.e.topSuite ↘ middle suite ↘ lower suite
                middle suite ↘ lower suite ↘ l-test 1(c.e.topSuite)
             c.e.topSuite2
              t2n-test1(c.e.topSuite2)
        """.trimIndent()

    private val expectationForGradleFilesWithNesting =
        """
            TestSession
             topSuite
              t-test 1(c.e.topSuite)
              middle suite
               m-test 1(c.e.topSuite)
               maybe excluded suite
                e-test 1(c.e.topSuite)
               lower suite
                l-test 1(c.e.topSuite)
             top 2 named
              t2n-test1(c.e.topSuite2)
        """.trimIndent()

    @Test
    fun gradleIntellijIdeaWithNesting() = descriptionTest(
        ReportingMode.GradleIntellijIdeaWithNesting,
        """
            〈tb〈TestSession⬥TestSession〉tb〉
             〈tb〈c.e.topSuite⬥topSuite〉tb〉
              〈tb〈c.e.topSuite↘t-test 1⬥t-test 1〉tb〉(c.e.topSuite)
              〈tb〈c.e.topSuite↘middle suite⬥middle suite〉tb〉
               〈tb〈c.e.topSuite↘middle suite↘m-test 1⬥m-test 1〉tb〉(c.e.topSuite)
               〈tb〈c.e.topSuite↘middle suite↘maybe excluded suite⬥maybe excluded suite〉tb〉
                〈tb〈c.e.topSuite↘middle suite↘maybe excluded suite↘e-test 1⬥e-test 1〉tb〉(c.e.topSuite)
               〈tb〈c.e.topSuite↘middle suite↘lower suite⬥lower suite〉tb〉
                〈tb〈c.e.topSuite↘middle suite↘lower suite↘l-test 1⬥l-test 1〉tb〉(c.e.topSuite)
             〈tb〈c.e.topSuite2⬥top 2 named〉tb〉
              〈tb〈c.e.topSuite2↘t2n-test1⬥t2n-test1〉tb〉(c.e.topSuite2)
        """.trimIndent()
    )

    @Test
    fun gradleIntellijIdeaWithoutNesting() =
        @Suppress("LongLine")
        descriptionTest(
            ReportingMode.GradleIntellijIdeaWithoutNesting,
            """
                〈tb〈TestSession⬥TestSession〉tb〉
                 〈tb〈c.e.topSuite⬥c.e.topSuite〉tb〉
                  〈tb〈c.e.topSuite↘t-test 1⬥topSuite ↘ t-test 1〉tb〉(c.e.topSuite)
                  〈tb〈c.e.topSuite↘middle suite⬥c.e.topSuite ↘ middle suite〉tb〉
                   〈tb〈c.e.topSuite↘middle suite↘m-test 1⬥topSuite ↘ middle suite ↘ m-test 1〉tb〉(c.e.topSuite)
                   〈tb〈c.e.topSuite↘middle suite↘maybe excluded suite⬥c.e.topSuite ↘ middle suite ↘ maybe excluded suite〉tb〉
                    〈tb〈c.e.topSuite↘middle suite↘maybe excluded suite↘e-test 1⬥topSuite ↘ middle suite ↘ maybe excluded suite ↘ e-test 1〉tb〉(c.e.topSuite)
                   〈tb〈c.e.topSuite↘middle suite↘lower suite⬥c.e.topSuite ↘ middle suite ↘ lower suite〉tb〉
                    〈tb〈c.e.topSuite↘middle suite↘lower suite↘l-test 1⬥topSuite ↘ middle suite ↘ lower suite ↘ l-test 1〉tb〉(c.e.topSuite)
                 〈tb〈c.e.topSuite2⬥c.e.topSuite2〉tb〉
                  〈tb〈c.e.topSuite2↘t2n-test1⬥top 2 named ↘ t2n-test1〉tb〉(c.e.topSuite2)
            """.trimIndent()
        )

    private fun descriptionTest(reportingMode: ReportingMode, expectedOutput: String): TestResult = try {
        FrameworkTestUtilities.withTestFramework(TestSession(reportingMode = reportingMode)) {
            val suite1 by testSuite(propertyFqn = "c.e.topSuite") {
                test("t-test 1") {}

                testSuite("middle suite") {
                    test("m-test 1") {}

                    testSuite("excluded suite") {
                        test("e-test 1") {}
                    }

                    testSuite("maybe excluded suite") {
                        test("e-test 1") {}
                    }

                    testSuite("lower suite") {
                        test("l-test 1") {}
                    }
                }
            }

            val suite2 by testSuite("top 2 named", propertyFqn = "c.e.topSuite2") {
                test("t2n-test1") {}
            }

            listOf(suite1, suite2) // Make suites register with the TestSession.
            val session = TestSession.global.apply {
                setUp(
                    object : Selection {
                        override fun includes(testElement: TestElement): Boolean =
                            testElement.testElementName !in setOf("excluded suite", "maybe excluded suite")

                        override fun mayInclude(testSuite: TestSuite): Boolean =
                            testSuite.testElementName != "excluded suite"
                    },
                    ThrowingTestSetupReport()
                )
            }

            val output = buildList {
                ElementAndDescription(session, session.newPlatformDescription()).onEach { level ->
                    add(" ".repeat(level) + "$description")
                }
            }.joinToString("\n")

            assertEquals(expectedOutput, output)
        }
    } finally {
        TestBalloonJUnit4Runner.reset()
    }

    private class ElementAndDescription(val element: TestElement, val description: Description)

    private fun ElementAndDescription.onEach(level: Int = 0, action: ElementAndDescription.(level: Int) -> Unit) {
        action(level)
        if (element is TestSuite) {
            val suiteChildren = if (element is TestSession) {
                element.testElementChildren.flatMap { (it as TestCompartment).testElementChildren }
            } else {
                element.testElementChildren
            }.filter { it.isIncluded }
            val descriptionChildren = description.children.toList()

            assertEquals(suiteChildren.size, descriptionChildren.size) {
                "$element: ${suiteChildren.size} children, but ${descriptionChildren.size} descriptors"
            }
            suiteChildren.zip(descriptionChildren).forEach {
                ElementAndDescription(it.first, it.second).onEach(level + 1, action)
            }
        }
    }
}
