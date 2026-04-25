package de.infix.testBalloon.framework.core.internal

import de.infix.testBalloon.framework.core.TestElement
import de.infix.testBalloon.framework.core.TestSession
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.internal.integration.TeamCityTestExecutionReport
import de.infix.testBalloon.framework.core.internal.integration.ThrowingTestSetupReport
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.internal.ReportingMode
import kotlinx.coroutines.test.TestResult
import kotlin.also
import kotlin.collections.listOf
import kotlin.getValue
import kotlin.test.Test
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
    fun basicOutputIntelliJ() = basicOutputTest(
        ReportingMode.GradleIntellijIdea,
        @Suppress("LongLine", "ktlint:standard:max-line-length")
        listOf(
            "##teamcity[flowStarted flowId='topSuite']",
            "##teamcity[testSuiteStarted name='${iBegin}topSuite${iCSep}topSuite$iEnd.topSuite' timestamp='...' flowId='topSuite']",
            "##teamcity[flowStarted flowId='topSuite${iESep}test1' parent='topSuite']",
            "##teamcity[testStarted name='${iBegin}topSuite${iESep}test1${iCSep}test1$iEnd.test1' timestamp='...' flowId='topSuite${iESep}test1']",
            "##teamcity[testFinished name='${iBegin}topSuite${iESep}test1${iCSep}test1$iEnd.test1' timestamp='...' flowId='topSuite${iESep}test1']",
            "##teamcity[flowFinished flowId='topSuite${iESep}test1']",
            "##teamcity[flowStarted flowId='topSuite${iESep}test2' parent='topSuite']",
            "##teamcity[testStarted name='${iBegin}topSuite${iESep}test2${iCSep}test2$iEnd.test2' timestamp='...' flowId='topSuite${iESep}test2']",
            "##teamcity[testFailed name='${iBegin}topSuite${iESep}test2${iCSep}test2$iEnd.test2' timestamp='...' message='intentionally' details='AssertionError' flowId='topSuite${iESep}test2']",
            "##teamcity[testFinished name='${iBegin}topSuite${iESep}test2${iCSep}test2$iEnd.test2' timestamp='...' flowId='topSuite${iESep}test2']",
            "##teamcity[flowFinished flowId='topSuite${iESep}test2']",
            "##teamcity[flowStarted flowId='topSuite${iESep}subSuite1' parent='topSuite']",
            "##teamcity[testSuiteStarted name='${iBegin}topSuite${iESep}subSuite1${iCSep}topSuite${dESep}subSuite1$iEnd.subSuite1' timestamp='...' flowId='topSuite${iESep}subSuite1']",
            "##teamcity[flowStarted flowId='topSuite${iESep}subSuite1${iESep}test1' parent='topSuite${iESep}subSuite1']",
            "##teamcity[testStarted name='${iBegin}topSuite${iESep}subSuite1${iESep}test1${iCSep}test1$iEnd.test1' timestamp='...' flowId='topSuite${iESep}subSuite1${iESep}test1']",
            "##teamcity[testFinished name='${iBegin}topSuite${iESep}subSuite1${iESep}test1${iCSep}test1$iEnd.test1' timestamp='...' flowId='topSuite${iESep}subSuite1${iESep}test1']",
            "##teamcity[flowFinished flowId='topSuite${iESep}subSuite1${iESep}test1']",
            "##teamcity[testSuiteFinished name='${iBegin}topSuite${iESep}subSuite1${iCSep}topSuite${dESep}subSuite1$iEnd.subSuite1' timestamp='...' flowId='topSuite${iESep}subSuite1']",
            "##teamcity[flowFinished flowId='topSuite${iESep}subSuite1']",
            "##teamcity[flowStarted flowId='topSuite${iESep}test3' parent='topSuite']",
            "##teamcity[testStarted name='${iBegin}topSuite${iESep}test3${iCSep}test3$iEnd.test3' timestamp='...' flowId='topSuite${iESep}test3']",
            "##teamcity[testFinished name='${iBegin}topSuite${iESep}test3${iCSep}test3$iEnd.test3' timestamp='...' flowId='topSuite${iESep}test3']",
            "##teamcity[flowFinished flowId='topSuite${iESep}test3']",
            "##teamcity[flowStarted flowId='topSuite${iESep}subSuite2' parent='topSuite']",
            "##teamcity[testSuiteStarted name='${iBegin}topSuite${iESep}subSuite2${iCSep}topSuite${dESep}subSuite2$iEnd.subSuite2' timestamp='...' flowId='topSuite${iESep}subSuite2']",
            "##teamcity[flowStarted flowId='topSuite${iESep}subSuite2${iESep}test1' parent='topSuite${iESep}subSuite2']",
            "##teamcity[testStarted name='${iBegin}topSuite${iESep}subSuite2${iESep}test1${iCSep}test1$iEnd.test1' timestamp='...' flowId='topSuite${iESep}subSuite2${iESep}test1']",
            "##teamcity[testFailed name='${iBegin}topSuite${iESep}subSuite2${iESep}test1${iCSep}test1$iEnd.test1' timestamp='...' message='intentionally' details='AssertionError' flowId='topSuite${iESep}subSuite2${iESep}test1']",
            "##teamcity[testFinished name='${iBegin}topSuite${iESep}subSuite2${iESep}test1${iCSep}test1$iEnd.test1' timestamp='...' flowId='topSuite${iESep}subSuite2${iESep}test1']",
            "##teamcity[flowFinished flowId='topSuite${iESep}subSuite2${iESep}test1']",
            "##teamcity[testSuiteFinished name='${iBegin}topSuite${iESep}subSuite2${iCSep}topSuite${dESep}subSuite2$iEnd.subSuite2' timestamp='...' flowId='topSuite${iESep}subSuite2']",
            "##teamcity[flowFinished flowId='topSuite${iESep}subSuite2']",
            "##teamcity[testSuiteFinished name='${iBegin}topSuite${iCSep}topSuite$iEnd.topSuite' timestamp='...' flowId='topSuite']",
            "##teamcity[flowFinished flowId='topSuite']"
        )
    )

    @Test
    fun basicOutputFiles() = basicOutputTest(
        ReportingMode.GradleFiles,
        listOf(
            "##teamcity[flowStarted flowId='topSuite']",
            "##teamcity[testSuiteStarted name='topSuite' timestamp='...' flowId='topSuite']",
            "##teamcity[flowStarted flowId='topSuite${iESep}test1' parent='topSuite']",
            "##teamcity[testStarted name='test1' timestamp='...' flowId='topSuite${iESep}test1']",
            "##teamcity[testFinished name='test1' timestamp='...' flowId='topSuite${iESep}test1']",
            "##teamcity[flowFinished flowId='topSuite${iESep}test1']",
            "##teamcity[flowStarted flowId='topSuite${iESep}test2' parent='topSuite']",
            "##teamcity[testStarted name='test2' timestamp='...' flowId='topSuite${iESep}test2']",
            "##teamcity[testFailed name='test2' timestamp='...' message='intentionally' details='AssertionError' flowId='topSuite${iESep}test2']",
            "##teamcity[testFinished name='test2' timestamp='...' flowId='topSuite${iESep}test2']",
            "##teamcity[flowFinished flowId='topSuite${iESep}test2']",
            "##teamcity[flowStarted flowId='topSuite${iESep}subSuite1' parent='topSuite']",
            "##teamcity[testSuiteStarted name='subSuite1' timestamp='...' flowId='topSuite${iESep}subSuite1']",
            "##teamcity[flowStarted flowId='topSuite${iESep}subSuite1${iESep}test1' parent='topSuite${iESep}subSuite1']",
            "##teamcity[testStarted name='test1' timestamp='...' flowId='topSuite${iESep}subSuite1${iESep}test1']",
            "##teamcity[testFinished name='test1' timestamp='...' flowId='topSuite${iESep}subSuite1${iESep}test1']",
            "##teamcity[flowFinished flowId='topSuite${iESep}subSuite1${iESep}test1']",
            "##teamcity[testSuiteFinished name='subSuite1' timestamp='...' flowId='topSuite${iESep}subSuite1']",
            "##teamcity[flowFinished flowId='topSuite${iESep}subSuite1']",
            "##teamcity[flowStarted flowId='topSuite${iESep}test3' parent='topSuite']",
            "##teamcity[testStarted name='test3' timestamp='...' flowId='topSuite${iESep}test3']",
            "##teamcity[testFinished name='test3' timestamp='...' flowId='topSuite${iESep}test3']",
            "##teamcity[flowFinished flowId='topSuite${iESep}test3']",
            "##teamcity[flowStarted flowId='topSuite${iESep}subSuite2' parent='topSuite']",
            "##teamcity[testSuiteStarted name='subSuite2' timestamp='...' flowId='topSuite${iESep}subSuite2']",
            "##teamcity[flowStarted flowId='topSuite${iESep}subSuite2${iESep}test1' parent='topSuite${iESep}subSuite2']",
            "##teamcity[testStarted name='test1' timestamp='...' flowId='topSuite${iESep}subSuite2${iESep}test1']",
            "##teamcity[testFailed name='test1' timestamp='...' message='intentionally' details='AssertionError' flowId='topSuite${iESep}subSuite2${iESep}test1']",
            "##teamcity[testFinished name='test1' timestamp='...' flowId='topSuite${iESep}subSuite2${iESep}test1']",
            "##teamcity[flowFinished flowId='topSuite${iESep}subSuite2${iESep}test1']",
            "##teamcity[testSuiteFinished name='subSuite2' timestamp='...' flowId='topSuite${iESep}subSuite2']",
            "##teamcity[flowFinished flowId='topSuite${iESep}subSuite2']",
            "##teamcity[testSuiteFinished name='topSuite' timestamp='...' flowId='topSuite']",
            "##teamcity[flowFinished flowId='topSuite']"
        )
    )

    private fun basicOutputTest(reportingMode: ReportingMode, expectedElements: List<String>): TestResult =
        FrameworkTestUtilities.withTestFramework(TestSession(reportingMode = reportingMode)) {
            val suite by testSuite(propertyFqn = "topSuite") {
                test("test1") {}

                test("test2") { fail("intentionally") }

                testSuite("subSuite1") {
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

                val actualElements = output.comparableElements()
                actualElements.assertContainsInOrder(
                    expectedElements,
                    exhaustive = true
                )
            }
        }

    @Test
    fun concurrentOutput() =
        FrameworkTestUtilities.withTestFramework(TestSession(reportingMode = ReportingMode.GradleFiles)) {
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

            suite // use the lazy value
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

            val actualElements = output.comparableElements()
            actualElements.assertContainsInOrder(
                listOf(
                    "##teamcity[flowStarted flowId='concurrent']",
                    "##teamcity[testSuiteStarted name='concurrent' timestamp='...' flowId='concurrent']",
                    "##teamcity[flowStarted flowId='concurrent${iESep}suite-1' parent='concurrent']",
                    "##teamcity[testSuiteStarted name='suite-1' timestamp='...' flowId='concurrent${iESep}suite-1']",
                    "##teamcity[flowStarted flowId='concurrent${iESep}suite-1${iESep}suite-1-test2' parent='concurrent${iESep}suite-1']",
                    "##teamcity[testStarted name='suite-1-test2' timestamp='...' flowId='concurrent${iESep}suite-1${iESep}suite-1-test2']",
                    "##teamcity[testFinished name='suite-1-test2' timestamp='...' flowId='concurrent${iESep}suite-1${iESep}suite-1-test2']",
                    "##teamcity[flowFinished flowId='concurrent${iESep}suite-1${iESep}suite-1-test2']",
                    "##teamcity[flowStarted flowId='concurrent${iESep}suite-1${iESep}suite-1-test1' parent='concurrent${iESep}suite-1']",
                    "##teamcity[testStarted name='suite-1-test1' timestamp='...' flowId='concurrent${iESep}suite-1${iESep}suite-1-test1']",
                    "##teamcity[testFinished name='suite-1-test1' timestamp='...' flowId='concurrent${iESep}suite-1${iESep}suite-1-test1']",
                    "##teamcity[flowFinished flowId='concurrent${iESep}suite-1${iESep}suite-1-test1']",
                    "##teamcity[testSuiteFinished name='suite-1' timestamp='...' flowId='concurrent${iESep}suite-1']",
                    "##teamcity[flowFinished flowId='concurrent${iESep}suite-1']",
                    "##teamcity[flowStarted flowId='concurrent${iESep}suite-2' parent='concurrent']",
                    "##teamcity[testSuiteStarted name='suite-2' timestamp='...' flowId='concurrent${iESep}suite-2']",
                    "##teamcity[flowStarted flowId='concurrent${iESep}suite-2${iESep}suite-2-test1' parent='concurrent${iESep}suite-2']",
                    "##teamcity[testStarted name='suite-2-test1' timestamp='...' flowId='concurrent${iESep}suite-2${iESep}suite-2-test1']",
                    "##teamcity[testFinished name='suite-2-test1' timestamp='...' flowId='concurrent${iESep}suite-2${iESep}suite-2-test1']",
                    "##teamcity[flowFinished flowId='concurrent${iESep}suite-2${iESep}suite-2-test1']",
                    "##teamcity[flowStarted flowId='concurrent${iESep}suite-2${iESep}suite-2-test2' parent='concurrent${iESep}suite-2']",
                    "##teamcity[testStarted name='suite-2-test2' timestamp='...' flowId='concurrent${iESep}suite-2${iESep}suite-2-test2']",
                    "##teamcity[testFinished name='suite-2-test2' timestamp='...' flowId='concurrent${iESep}suite-2${iESep}suite-2-test2']",
                    "##teamcity[flowFinished flowId='concurrent${iESep}suite-2${iESep}suite-2-test2']",
                    "##teamcity[testSuiteFinished name='suite-2' timestamp='...' flowId='concurrent${iESep}suite-2']",
                    "##teamcity[flowFinished flowId='concurrent${iESep}suite-2']",
                    "##teamcity[flowStarted flowId='concurrent${iESep}test2' parent='concurrent']",
                    "##teamcity[testStarted name='test2' timestamp='...' flowId='concurrent${iESep}test2']",
                    "##teamcity[testFinished name='test2' timestamp='...' flowId='concurrent${iESep}test2']",
                    "##teamcity[flowFinished flowId='concurrent${iESep}test2']",
                    "##teamcity[flowStarted flowId='concurrent${iESep}test1' parent='concurrent']",
                    "##teamcity[testStarted name='test1' timestamp='...' flowId='concurrent${iESep}test1']",
                    "##teamcity[testFinished name='test1' timestamp='...' flowId='concurrent${iESep}test1']",
                    "##teamcity[flowFinished flowId='concurrent${iESep}test1']",
                    "##teamcity[testSuiteFinished name='concurrent' timestamp='...' flowId='concurrent']",
                    "##teamcity[flowFinished flowId='concurrent']"
                ),
                exhaustive = true
            )
        }

    private fun FrameworkTestUtilities.ConcurrentList<String>.comparableElements() = elements()
        .map { it.replace(timestampRegex, "timestamp='...'").replace(detailsRegex, "details='AssertionError'") }
}
