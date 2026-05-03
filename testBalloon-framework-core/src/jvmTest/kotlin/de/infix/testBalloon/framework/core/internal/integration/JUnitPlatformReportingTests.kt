package de.infix.testBalloon.framework.core.internal.integration

import de.infix.testBalloon.framework.core.TestElement
import de.infix.testBalloon.framework.core.TestSession
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.internal.FrameworkTestUtilities
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.internal.ReportingMode
import kotlinx.coroutines.test.TestResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import kotlin.test.Test

@Suppress("LongLine")
class JUnitPlatformReportingTests {

    @Test
    fun gradleFilesWithoutNesting() = descriptorTest(
        ReportingMode.GradleFilesWithoutNesting,
        """
            PD(uId=[engine:testing.internal]/[suite:c.e.topSuite], dN="topSuite", t=CONTAINER, s=ClassSource [className = 'c.e.topSuite', filePosition = null])
             PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[test:t-test 1], dN="t-test 1", t=TEST, s=null)
             PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[suite:middle suite], dN="middle suite", t=CONTAINER, s=null)
              PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[suite:middle suite]/[test:m-test 1], dN="middle suite ↘ m-test 1", t=TEST, s=null)
              PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[suite:middle suite]/[suite:lower suite], dN="middle suite ↘ lower suite", t=CONTAINER, s=null)
               PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[suite:middle suite]/[suite:lower suite]/[test:l-test 1], dN="middle suite ↘ lower suite ↘ l-test 1", t=TEST, s=null)
            PD(uId=[engine:testing.internal]/[suite:c.e.topSuite2], dN="top 2 named", t=CONTAINER, s=ClassSource [className = 'c.e.topSuite2', filePosition = null])
             PD(uId=[engine:testing.internal]/[suite:c.e.topSuite2]/[test:t2n-test1], dN="t2n-test1", t=TEST, s=null)
        """.trimIndent()
    )

    @Test
    fun gradleFilesWithNesting() =
        descriptorTest(ReportingMode.GradleFilesWithNesting, expectationForGradleFilesWithNesting)

    @Test
    fun amper() = descriptorTest(ReportingMode.Amper, expectationForGradleFilesWithNesting)

    private val expectationForGradleFilesWithNesting =
        """
            PD(uId=[engine:testing.internal]/[suite:c.e.topSuite], dN="topSuite", t=CONTAINER, s=null)
             PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[test:t-test 1], dN="t-test 1", t=TEST, s=null)
             PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[suite:middle suite], dN="middle suite", t=CONTAINER, s=null)
              PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[suite:middle suite]/[test:m-test 1], dN="m-test 1", t=TEST, s=null)
              PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[suite:middle suite]/[suite:lower suite], dN="lower suite", t=CONTAINER, s=null)
               PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[suite:middle suite]/[suite:lower suite]/[test:l-test 1], dN="l-test 1", t=TEST, s=null)
            PD(uId=[engine:testing.internal]/[suite:c.e.topSuite2], dN="top 2 named", t=CONTAINER, s=null)
             PD(uId=[engine:testing.internal]/[suite:c.e.topSuite2]/[test:t2n-test1], dN="t2n-test1", t=TEST, s=null)
        """.trimIndent()

    @Test
    fun gradleIntellijIdeaLegacy() = descriptorTest(
        ReportingMode.GradleIntellijIdeaLegacy,
        """
            PD(uId=[engine:testing.internal]/[suite:c.e.topSuite], dN="topSuite", t=CONTAINER, s=ClassSource [className = 'c.e.topSuite', filePosition = null])
             PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[test:t-test 1], dN="t-test 1", t=TEST, s=null)
             PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[suite:middle suite], dN="middle suite", t=CONTAINER, s=null)
              PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[suite:middle suite]/[test:m-test 1], dN="m-test 1", t=TEST, s=null)
              PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[suite:middle suite]/[suite:lower suite], dN="lower suite", t=CONTAINER, s=null)
               PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[suite:middle suite]/[suite:lower suite]/[test:l-test 1], dN="l-test 1", t=TEST, s=null)
            PD(uId=[engine:testing.internal]/[suite:c.e.topSuite2], dN="top 2 named", t=CONTAINER, s=ClassSource [className = 'c.e.topSuite2', filePosition = null])
             PD(uId=[engine:testing.internal]/[suite:c.e.topSuite2]/[test:t2n-test1], dN="t2n-test1", t=TEST, s=null)
        """.trimIndent()
    )

    @Test
    fun gradleIntellijIdeaWithoutNesting() = descriptorTest(
        ReportingMode.GradleIntellijIdeaWithoutNesting,
        """
            PD(uId=[engine:testing.internal]/[suite:c.e.topSuite], dN="〈tb〈c.e.topSuite⬥topSuite〉tb〉", t=CONTAINER, s=ClassSource [className = 'c.e.topSuite', filePosition = null])
             PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[test:t-test 1], dN="〈tb〈c.e.topSuite↘t-test 1⬥t-test 1〉tb〉", t=TEST, s=null)
             PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[suite:middle suite], dN="〈tb〈c.e.topSuite↘middle suite⬥middle suite〉tb〉", t=CONTAINER, s=null)
              PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[suite:middle suite]/[test:m-test 1], dN="〈tb〈c.e.topSuite↘middle suite↘m-test 1⬥m-test 1〉tb〉", t=TEST, s=null)
              PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[suite:middle suite]/[suite:lower suite], dN="〈tb〈c.e.topSuite↘middle suite↘lower suite⬥lower suite〉tb〉", t=CONTAINER, s=null)
               PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[suite:middle suite]/[suite:lower suite]/[test:l-test 1], dN="〈tb〈c.e.topSuite↘middle suite↘lower suite↘l-test 1⬥l-test 1〉tb〉", t=TEST, s=null)
            PD(uId=[engine:testing.internal]/[suite:c.e.topSuite2], dN="〈tb〈c.e.topSuite2⬥top 2 named〉tb〉", t=CONTAINER, s=ClassSource [className = 'c.e.topSuite2', filePosition = null])
             PD(uId=[engine:testing.internal]/[suite:c.e.topSuite2]/[test:t2n-test1], dN="〈tb〈c.e.topSuite2↘t2n-test1⬥t2n-test1〉tb〉", t=TEST, s=null)
        """.trimIndent()
    )

    @Test
    fun gradleIntellijIdeaWithNesting() = descriptorTest(
        ReportingMode.GradleIntellijIdeaWithNesting,
        """
            PD(uId=[engine:testing.internal]/[suite:c.e.topSuite], dN="〈tb〈c.e.topSuite⬥topSuite〉tb〉", t=CONTAINER, s=null)
             PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[test:t-test 1], dN="〈tb〈c.e.topSuite↘t-test 1⬥t-test 1〉tb〉", t=TEST, s=null)
             PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[suite:middle suite], dN="〈tb〈c.e.topSuite↘middle suite⬥middle suite〉tb〉", t=CONTAINER, s=null)
              PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[suite:middle suite]/[test:m-test 1], dN="〈tb〈c.e.topSuite↘middle suite↘m-test 1⬥m-test 1〉tb〉", t=TEST, s=null)
              PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[suite:middle suite]/[suite:lower suite], dN="〈tb〈c.e.topSuite↘middle suite↘lower suite⬥lower suite〉tb〉", t=CONTAINER, s=null)
               PD(uId=[engine:testing.internal]/[suite:c.e.topSuite]/[suite:middle suite]/[suite:lower suite]/[test:l-test 1], dN="〈tb〈c.e.topSuite↘middle suite↘lower suite↘l-test 1⬥l-test 1〉tb〉", t=TEST, s=null)
            PD(uId=[engine:testing.internal]/[suite:c.e.topSuite2], dN="〈tb〈c.e.topSuite2⬥top 2 named〉tb〉", t=CONTAINER, s=null)
             PD(uId=[engine:testing.internal]/[suite:c.e.topSuite2]/[test:t2n-test1], dN="〈tb〈c.e.topSuite2↘t2n-test1⬥t2n-test1〉tb〉", t=TEST, s=null)
        """.trimIndent()
    )

    private fun descriptorTest(reportingMode: ReportingMode, expectedOutput: String): TestResult = try {
        FrameworkTestUtilities.withTestFramework(TestSession(reportingMode = reportingMode)) {
            val suite1 by testSuite(qualifiedPropertyName = "c.e.topSuite") {
                test("t-test 1") {}

                testSuite("middle suite") {
                    test("m-test 1") {}

                    testSuite("lower suite") {
                        test("l-test 1") {}
                    }
                }
            }

            val suite2 by testSuite("top 2 named", qualifiedPropertyName = "c.e.topSuite2") {
                test("t2n-test1") {}
            }

            val suites = listOf(suite1, suite2) // Make suites register with the TestSession.
            TestSession.global.setUp(TestElement.AllInSelection, ThrowingTestSetupReport())

            val output = buildList {
                val engineDescriptor = UniqueId.forEngine("testing.internal")
                for (suite in suites) {
                    ElementAndDescriptor(suite, suite.newPlatformDescriptor(engineDescriptor)).onEach { level ->
                        add(" ".repeat(level) + "$descriptor")
                    }
                }
            }.joinToString("\n")

            assertEquals(expectedOutput, output)
        }
    } finally {
        TestBalloonJUnitPlatformTestEngine.reset()
    }

    private class ElementAndDescriptor(val element: TestElement, val descriptor: TestDescriptor)

    private fun ElementAndDescriptor.onEach(level: Int = 0, action: ElementAndDescriptor.(level: Int) -> Unit) {
        action(level)
        if (element is TestSuite) {
            val suiteChildren = element.testElementChildren.toList()
            val descriptorChildren = descriptor.children.toList()
            assertEquals(suiteChildren.size, descriptorChildren.size) {
                "$element: ${suiteChildren.size} children, but ${descriptorChildren.size} descriptors"
            }
            suiteChildren.zip(descriptorChildren).forEach {
                ElementAndDescriptor(it.first, it.second).onEach(level + 1, action)
            }
        }
    }
}
