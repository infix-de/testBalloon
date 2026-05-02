package de.infix.testBalloon.framework.core.internal

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestElement
import de.infix.testBalloon.framework.core.TestSession
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.disable
import de.infix.testBalloon.framework.core.internal.integration.TeamCityTestExecutionReport
import de.infix.testBalloon.framework.core.internal.integration.ThrowingTestSetupReport
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.internal.ReportingMode
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class TeamCityTestExecutionReportTests {
    private val timestampRegex = Regex("""timestamp='[^']+'""")
    private val detailsRegex = Regex("""details='[^:]+Error:[^']*'""")
    private val dESep = "|0x00a0|0x2198|0x00a0" // escaped report separator
    private val iESep = "|0x2198" // escaped internal separator
    private val iBegin = "|0x2329tb|0x2329"
    private val iEnd = "|0x232atb|0x232a"
    private val iCSep = "|0x2b25"

    @Test
    fun basicOutputGradleIntellijIdeaWithNesting() =
        basicOutputTest(ReportingMode.GradleIntellijIdeaWithNesting, expectationForGradleIntellijIdea)

    @Test
    fun basicOutputGradleIntellijIdeaWithoutNesting() =
        basicOutputTest(ReportingMode.GradleIntellijIdeaWithoutNesting, expectationForGradleIntellijIdea)

    @Test
    fun basicOutputGradleFilesWithNesting() =
        basicOutputTest(ReportingMode.GradleFilesWithNesting, expectationForGradleFilesWithNesting)

    @Test
    fun basicOutputAmper() = basicOutputTest(ReportingMode.GradleFilesWithNesting, expectationForGradleFilesWithNesting)

    @Test
    fun basicOutputGradleFilesWithoutNesting() =
        basicOutputTest(ReportingMode.GradleFilesWithoutNesting, expectationForGradleFilesWithoutNesting)

    @Test
    fun basicOutputGradleIntellijIdeaLegacy() =
        basicOutputTest(ReportingMode.GradleIntellijIdeaLegacy, expectationForGradleFilesWithoutNesting)

    private val expectationForGradleIntellijIdea =
        @Suppress("LongLine")
        """
            ##teamcity[flowStarted flowId='c.e.topSuite']
            ##teamcity[testSuiteStarted name='${iBegin}c.e.topSuite${iCSep}topSuite$iEnd.FOR_IDE_PLUGIN' timestamp='...' flowId='c.e.topSuite']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}test1' parent='c.e.topSuite']
            ##teamcity[testStarted name='${iBegin}c.e.topSuite${iESep}test1${iCSep}test1$iEnd.FOR_IDE_PLUGIN' timestamp='...' flowId='c.e.topSuite${iESep}test1']
            ##teamcity[testFinished name='${iBegin}c.e.topSuite${iESep}test1${iCSep}test1$iEnd.FOR_IDE_PLUGIN' timestamp='...' flowId='c.e.topSuite${iESep}test1']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}test1']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}test2' parent='c.e.topSuite']
            ##teamcity[testStarted name='${iBegin}c.e.topSuite${iESep}test2${iCSep}test2$iEnd.FOR_IDE_PLUGIN' timestamp='...' flowId='c.e.topSuite${iESep}test2']
            ##teamcity[testFailed name='${iBegin}c.e.topSuite${iESep}test2${iCSep}test2$iEnd.FOR_IDE_PLUGIN' timestamp='...' message='intentionally' details='AssertionError' flowId='c.e.topSuite${iESep}test2']
            ##teamcity[testFinished name='${iBegin}c.e.topSuite${iESep}test2${iCSep}test2$iEnd.FOR_IDE_PLUGIN' timestamp='...' flowId='c.e.topSuite${iESep}test2']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}test2']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}subSuite1' parent='c.e.topSuite']
            ##teamcity[testSuiteStarted name='${iBegin}c.e.topSuite${iESep}subSuite1${iCSep}topSuite${dESep}subSuite1$iEnd.FOR_IDE_PLUGIN' timestamp='...' flowId='c.e.topSuite${iESep}subSuite1']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}subSuite1${iESep}test1' parent='c.e.topSuite${iESep}subSuite1']
            ##teamcity[testIgnored name='${iBegin}c.e.topSuite|0x2198subSuite1|0x2198test1|0x2b25test1$iEnd.FOR_IDE_PLUGIN' timestamp='...' flowId='c.e.topSuite|0x2198subSuite1|0x2198test1']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}subSuite1${iESep}test1']
            ##teamcity[testSuiteFinished name='${iBegin}c.e.topSuite${iESep}subSuite1${iCSep}topSuite${dESep}subSuite1$iEnd.FOR_IDE_PLUGIN' timestamp='...' flowId='c.e.topSuite${iESep}subSuite1']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}subSuite1']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}test3' parent='c.e.topSuite']
            ##teamcity[testStarted name='${iBegin}c.e.topSuite${iESep}test3${iCSep}test3$iEnd.FOR_IDE_PLUGIN' timestamp='...' flowId='c.e.topSuite${iESep}test3']
            ##teamcity[testFinished name='${iBegin}c.e.topSuite${iESep}test3${iCSep}test3$iEnd.FOR_IDE_PLUGIN' timestamp='...' flowId='c.e.topSuite${iESep}test3']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}test3']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}subSuite2' parent='c.e.topSuite']
            ##teamcity[testSuiteStarted name='${iBegin}c.e.topSuite${iESep}subSuite2${iCSep}topSuite${dESep}subSuite2$iEnd.FOR_IDE_PLUGIN' timestamp='...' flowId='c.e.topSuite${iESep}subSuite2']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}subSuite2${iESep}test1' parent='c.e.topSuite${iESep}subSuite2']
            ##teamcity[testStarted name='${iBegin}c.e.topSuite${iESep}subSuite2${iESep}test1${iCSep}test1$iEnd.FOR_IDE_PLUGIN' timestamp='...' flowId='c.e.topSuite${iESep}subSuite2${iESep}test1']
            ##teamcity[testFailed name='${iBegin}c.e.topSuite${iESep}subSuite2${iESep}test1${iCSep}test1$iEnd.FOR_IDE_PLUGIN' timestamp='...' message='intentionally' details='AssertionError' flowId='c.e.topSuite${iESep}subSuite2${iESep}test1']
            ##teamcity[testFinished name='${iBegin}c.e.topSuite${iESep}subSuite2${iESep}test1${iCSep}test1$iEnd.FOR_IDE_PLUGIN' timestamp='...' flowId='c.e.topSuite${iESep}subSuite2${iESep}test1']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}subSuite2${iESep}test1']
            ##teamcity[testSuiteFinished name='${iBegin}c.e.topSuite${iESep}subSuite2${iCSep}topSuite${dESep}subSuite2$iEnd.FOR_IDE_PLUGIN' timestamp='...' flowId='c.e.topSuite${iESep}subSuite2']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}subSuite2']
            ##teamcity[testSuiteFinished name='${iBegin}c.e.topSuite${iCSep}topSuite$iEnd.FOR_IDE_PLUGIN' timestamp='...' flowId='c.e.topSuite']
            ##teamcity[flowFinished flowId='c.e.topSuite']
        """.trimIndent()

    private val expectationForGradleFilesWithNesting =
        @Suppress("LongLine")
        """
            ##teamcity[flowStarted flowId='c.e.topSuite']
            ##teamcity[testSuiteStarted name='topSuite' timestamp='...' flowId='c.e.topSuite']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}test1' parent='c.e.topSuite']
            ##teamcity[testStarted name='test1' timestamp='...' flowId='c.e.topSuite${iESep}test1']
            ##teamcity[testFinished name='test1' timestamp='...' flowId='c.e.topSuite${iESep}test1']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}test1']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}test2' parent='c.e.topSuite']
            ##teamcity[testStarted name='test2' timestamp='...' flowId='c.e.topSuite${iESep}test2']
            ##teamcity[testFailed name='test2' timestamp='...' message='intentionally' details='AssertionError' flowId='c.e.topSuite${iESep}test2']
            ##teamcity[testFinished name='test2' timestamp='...' flowId='c.e.topSuite${iESep}test2']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}test2']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}subSuite1' parent='c.e.topSuite']
            ##teamcity[testSuiteStarted name='subSuite1' timestamp='...' flowId='c.e.topSuite${iESep}subSuite1']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}subSuite1${iESep}test1' parent='c.e.topSuite${iESep}subSuite1']
            ##teamcity[testIgnored name='test1' timestamp='...' flowId='c.e.topSuite|0x2198subSuite1|0x2198test1']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}subSuite1${iESep}test1']
            ##teamcity[testSuiteFinished name='subSuite1' timestamp='...' flowId='c.e.topSuite${iESep}subSuite1']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}subSuite1']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}test3' parent='c.e.topSuite']
            ##teamcity[testStarted name='test3' timestamp='...' flowId='c.e.topSuite${iESep}test3']
            ##teamcity[testFinished name='test3' timestamp='...' flowId='c.e.topSuite${iESep}test3']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}test3']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}subSuite2' parent='c.e.topSuite']
            ##teamcity[testSuiteStarted name='subSuite2' timestamp='...' flowId='c.e.topSuite${iESep}subSuite2']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}subSuite2${iESep}test1' parent='c.e.topSuite${iESep}subSuite2']
            ##teamcity[testStarted name='test1' timestamp='...' flowId='c.e.topSuite${iESep}subSuite2${iESep}test1']
            ##teamcity[testFailed name='test1' timestamp='...' message='intentionally' details='AssertionError' flowId='c.e.topSuite${iESep}subSuite2${iESep}test1']
            ##teamcity[testFinished name='test1' timestamp='...' flowId='c.e.topSuite${iESep}subSuite2${iESep}test1']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}subSuite2${iESep}test1']
            ##teamcity[testSuiteFinished name='subSuite2' timestamp='...' flowId='c.e.topSuite${iESep}subSuite2']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}subSuite2']
            ##teamcity[testSuiteFinished name='topSuite' timestamp='...' flowId='c.e.topSuite']
            ##teamcity[flowFinished flowId='c.e.topSuite']
        """.trimIndent()

    private val expectationForGradleFilesWithoutNesting =
        @Suppress("LongLine")
        """
            ##teamcity[flowStarted flowId='c.e.topSuite']
            ##teamcity[testSuiteStarted name='c.e.topSuite' timestamp='...' flowId='c.e.topSuite']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}test1' parent='c.e.topSuite']
            ##teamcity[testStarted name='test1' timestamp='...' flowId='c.e.topSuite${iESep}test1']
            ##teamcity[testFinished name='test1' timestamp='...' flowId='c.e.topSuite${iESep}test1']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}test1']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}test2' parent='c.e.topSuite']
            ##teamcity[testStarted name='test2' timestamp='...' flowId='c.e.topSuite${iESep}test2']
            ##teamcity[testFailed name='test2' timestamp='...' message='intentionally' details='AssertionError' flowId='c.e.topSuite${iESep}test2']
            ##teamcity[testFinished name='test2' timestamp='...' flowId='c.e.topSuite${iESep}test2']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}test2']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}subSuite1' parent='c.e.topSuite']
            ##teamcity[testSuiteStarted name='subSuite1' timestamp='...' flowId='c.e.topSuite${iESep}subSuite1']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}subSuite1${iESep}test1' parent='c.e.topSuite${iESep}subSuite1']
            ##teamcity[testIgnored name='test1' timestamp='...' flowId='c.e.topSuite|0x2198subSuite1|0x2198test1']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}subSuite1${iESep}test1']
            ##teamcity[testSuiteFinished name='subSuite1' timestamp='...' flowId='c.e.topSuite${iESep}subSuite1']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}subSuite1']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}test3' parent='c.e.topSuite']
            ##teamcity[testStarted name='test3' timestamp='...' flowId='c.e.topSuite${iESep}test3']
            ##teamcity[testFinished name='test3' timestamp='...' flowId='c.e.topSuite${iESep}test3']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}test3']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}subSuite2' parent='c.e.topSuite']
            ##teamcity[testSuiteStarted name='subSuite2' timestamp='...' flowId='c.e.topSuite${iESep}subSuite2']
            ##teamcity[flowStarted flowId='c.e.topSuite${iESep}subSuite2${iESep}test1' parent='c.e.topSuite${iESep}subSuite2']
            ##teamcity[testStarted name='test1' timestamp='...' flowId='c.e.topSuite${iESep}subSuite2${iESep}test1']
            ##teamcity[testFailed name='test1' timestamp='...' message='intentionally' details='AssertionError' flowId='c.e.topSuite${iESep}subSuite2${iESep}test1']
            ##teamcity[testFinished name='test1' timestamp='...' flowId='c.e.topSuite${iESep}subSuite2${iESep}test1']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}subSuite2${iESep}test1']
            ##teamcity[testSuiteFinished name='subSuite2' timestamp='...' flowId='c.e.topSuite${iESep}subSuite2']
            ##teamcity[flowFinished flowId='c.e.topSuite${iESep}subSuite2']
            ##teamcity[testSuiteFinished name='c.e.topSuite' timestamp='...' flowId='c.e.topSuite']
            ##teamcity[flowFinished flowId='c.e.topSuite']
        """.trimIndent()

    private fun basicOutputTest(reportingMode: ReportingMode, expectedElements: String): TestResult =
        FrameworkTestUtilities.withTestFramework(TestSession(reportingMode = reportingMode)) {
            val suite by testSuite(propertyFqn = "c.e.topSuite") {
                test("test1") {}

                test("test2") { fail("intentionally") }

                testSuite("subSuite1", testConfig = TestConfig.disable()) {
                    test("test1") {}
                }

                test("test3") {}

                testSuite("subSuite2") {
                    test("test1") { fail("intentionally") }
                }
            }

            FrameworkTestUtilities.withTestReport(suite) {
                val output = FrameworkTestUtilities.ConcurrentList<String>()
                val report = TeamCityTestExecutionReport { output.add(it) }
                allEvents().forEach { report.add(it) }

                val actualElements = output.comparableElements().joinToString("\n")
                assertEquals(expectedElements, actualElements)
            }
        }

    @Test
    fun concurrentOutput() =
        FrameworkTestUtilities.withTestFramework(TestSession(reportingMode = ReportingMode.GradleFilesWithNesting)) {
            val suite by testSuite(propertyFqn = "concurrent") {
                test("test1") {}
                test("test2") {}

                testSuite("suite-1") {
                    test("suite-1-test1") {}
                    test("suite-1-test2") {}
                }

                testSuite("suite-2") {
                    test("suite-2-test1") {}
                    test("suite-2-test2") {}
                }
            }

            suite // Make suite register with the TestSession.
            TestSession.global.setUp(TestElement.AllInSelection, ThrowingTestSetupReport())

            val output = FrameworkTestUtilities.ConcurrentList<String>()
            val report = TeamCityTestExecutionReport { output.add(it) }

            val sessionStartingEvent = TestElement.Event.Starting(TestSession.global).also { report.add(it) }
            val compartmentStartingEvent =
                TestElement.Event.Starting(TestSession.global.defaultCompartment).also { report.add(it) }

            val startingEvents = mutableMapOf<TestElement, TestElement.Event.Starting>()
            listOf(
                "/S",
                "suite-1/S",
                "suite-2/S", // concurrent start, delayed reporting
                "suite-2|suite-2-test2", // reported in hierarchy order (2)
                "suite-2|suite-2-test1", // reported in hierarchy order (1)
                "suite-1|suite-1-test2", // reported in start order (1)
                "suite-2/F", // finished before suite-1, children reported in hierarchy order, not start order
                "suite-1|suite-1-test1", // reported in start order (2)
                "suite-1/F",
                "test2",
                "test1",
                "/F"
            ).forEach { elementPathPlusOptionalSuffix ->
                val elementPath = elementPathPlusOptionalSuffix.substringBeforeLast("/")
                val suffix = elementPathPlusOptionalSuffix.substringAfterLast("/", "")
                val elements = if (elementPath.isEmpty()) listOf() else elementPath.split("|")
                val element = elements.fold<String, TestElement>(suite) { element, childName ->
                    (element as TestSuite).testElementChildren.first { it.testElementName == childName }
                }
                when (suffix) {
                    "S" -> report.add(TestElement.Event.Starting(element).also { startingEvents[element] = it })

                    "F" -> report.add(TestElement.Event.Finished(element, startingEvents[element]!!))

                    else -> {
                        val startingEvent = TestElement.Event.Starting(element)
                        report.add(startingEvent)
                        report.add(TestElement.Event.Finished(element, startingEvent))
                    }
                }
            }

            report.add(TestElement.Event.Finished(compartmentStartingEvent.element, compartmentStartingEvent))
            report.add(TestElement.Event.Finished(sessionStartingEvent.element, sessionStartingEvent))

            val actualElements = output.comparableElements().joinToString("\n")
            val expectedElements =
                @Suppress("LongLine")
                """
                    ##teamcity[flowStarted flowId='concurrent']
                    ##teamcity[testSuiteStarted name='concurrent' timestamp='...' flowId='concurrent']
                    ##teamcity[flowStarted flowId='concurrent${iESep}suite-1' parent='concurrent']
                    ##teamcity[testSuiteStarted name='suite-1' timestamp='...' flowId='concurrent${iESep}suite-1']
                    ##teamcity[flowStarted flowId='concurrent${iESep}suite-1${iESep}suite-1-test2' parent='concurrent${iESep}suite-1']
                    ##teamcity[testStarted name='suite-1-test2' timestamp='...' flowId='concurrent${iESep}suite-1${iESep}suite-1-test2']
                    ##teamcity[testFinished name='suite-1-test2' timestamp='...' flowId='concurrent${iESep}suite-1${iESep}suite-1-test2']
                    ##teamcity[flowFinished flowId='concurrent${iESep}suite-1${iESep}suite-1-test2']
                    ##teamcity[flowStarted flowId='concurrent${iESep}suite-1${iESep}suite-1-test1' parent='concurrent${iESep}suite-1']
                    ##teamcity[testStarted name='suite-1-test1' timestamp='...' flowId='concurrent${iESep}suite-1${iESep}suite-1-test1']
                    ##teamcity[testFinished name='suite-1-test1' timestamp='...' flowId='concurrent${iESep}suite-1${iESep}suite-1-test1']
                    ##teamcity[flowFinished flowId='concurrent${iESep}suite-1${iESep}suite-1-test1']
                    ##teamcity[testSuiteFinished name='suite-1' timestamp='...' flowId='concurrent${iESep}suite-1']
                    ##teamcity[flowFinished flowId='concurrent${iESep}suite-1']
                    ##teamcity[flowStarted flowId='concurrent${iESep}suite-2' parent='concurrent']
                    ##teamcity[testSuiteStarted name='suite-2' timestamp='...' flowId='concurrent${iESep}suite-2']
                    ##teamcity[flowStarted flowId='concurrent${iESep}suite-2${iESep}suite-2-test1' parent='concurrent${iESep}suite-2']
                    ##teamcity[testStarted name='suite-2-test1' timestamp='...' flowId='concurrent${iESep}suite-2${iESep}suite-2-test1']
                    ##teamcity[testFinished name='suite-2-test1' timestamp='...' flowId='concurrent${iESep}suite-2${iESep}suite-2-test1']
                    ##teamcity[flowFinished flowId='concurrent${iESep}suite-2${iESep}suite-2-test1']
                    ##teamcity[flowStarted flowId='concurrent${iESep}suite-2${iESep}suite-2-test2' parent='concurrent${iESep}suite-2']
                    ##teamcity[testStarted name='suite-2-test2' timestamp='...' flowId='concurrent${iESep}suite-2${iESep}suite-2-test2']
                    ##teamcity[testFinished name='suite-2-test2' timestamp='...' flowId='concurrent${iESep}suite-2${iESep}suite-2-test2']
                    ##teamcity[flowFinished flowId='concurrent${iESep}suite-2${iESep}suite-2-test2']
                    ##teamcity[testSuiteFinished name='suite-2' timestamp='...' flowId='concurrent${iESep}suite-2']
                    ##teamcity[flowFinished flowId='concurrent${iESep}suite-2']
                    ##teamcity[flowStarted flowId='concurrent${iESep}test2' parent='concurrent']
                    ##teamcity[testStarted name='test2' timestamp='...' flowId='concurrent${iESep}test2']
                    ##teamcity[testFinished name='test2' timestamp='...' flowId='concurrent${iESep}test2']
                    ##teamcity[flowFinished flowId='concurrent${iESep}test2']
                    ##teamcity[flowStarted flowId='concurrent${iESep}test1' parent='concurrent']
                    ##teamcity[testStarted name='test1' timestamp='...' flowId='concurrent${iESep}test1']
                    ##teamcity[testFinished name='test1' timestamp='...' flowId='concurrent${iESep}test1']
                    ##teamcity[flowFinished flowId='concurrent${iESep}test1']
                    ##teamcity[testSuiteFinished name='concurrent' timestamp='...' flowId='concurrent']
                    ##teamcity[flowFinished flowId='concurrent']
                """.trimIndent()
            assertEquals(expectedElements, actualElements)
        }

    private fun FrameworkTestUtilities.ConcurrentList<String>.comparableElements() = elements()
        .map { it.replace(timestampRegex, "timestamp='...'").replace(detailsRegex, "details='AssertionError'") }
}
