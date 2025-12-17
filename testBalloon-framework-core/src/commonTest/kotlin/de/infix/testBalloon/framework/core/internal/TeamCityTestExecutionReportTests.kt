package de.infix.testBalloon.framework.core.internal

import de.infix.testBalloon.framework.core.ConcurrentList
import de.infix.testBalloon.framework.core.TestElement
import de.infix.testBalloon.framework.core.TestSession
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.assertContainsInOrder
import de.infix.testBalloon.framework.core.internal.integration.TeamCityTestExecutionReport
import de.infix.testBalloon.framework.core.internal.integration.ThrowingTestSetupReport
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.core.withTestFramework
import de.infix.testBalloon.framework.core.withTestReport
import de.infix.testBalloon.framework.shared.internal.ReportingMode
import kotlin.also
import kotlin.getValue
import kotlin.test.Test
import kotlin.test.fail

class TeamCityTestExecutionReportTests {
    private val timestampRegex = Regex("""timestamp='[^']+'""")
    private val detailsRegex = Regex("""details='[^:]+Error:[^']*'""")
    private val rSepEsc = "|0x00a0|0x2198|0x00a0" // escaped report separator
    private val iSepEsc = "|0x2198" // escaped internal separator

    @Test
    fun basicOutputIntelliJ() = basicOutputTest(ReportingMode.IntellijIdea, "topSuite$rSepEsc")

    @Test
    fun basicOutputFiles() = basicOutputTest(ReportingMode.Files, "")

    private fun basicOutputTest(reportingMode: ReportingMode, extraPath: String) =
        withTestFramework(TestSession(reportingMode = reportingMode)) {
            val suite by testSuite("topSuite") {
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

            withTestReport(suite) {
                val output = ConcurrentList<String>()
                val report = TeamCityTestExecutionReport { output.add(it) }
                allEvents().forEach { report.add(it) }

                output.comparableElements()
                    .assertContainsInOrder(
                        listOf(
                            "##teamcity[testSuiteStarted name='topSuite' timestamp='...']",
                            "##teamcity[flowStarted flowId='topSuite' parent='TestSession$iSepEsc@Default']",
                            "##teamcity[testStarted name='test1' timestamp='...']",
                            "##teamcity[testFinished name='test1' timestamp='...']",
                            "##teamcity[testStarted name='test2' timestamp='...']",
                            "##teamcity[testFailed name='test2' timestamp='...' message='intentionally'" +
                                " details='AssertionError']",
                            "##teamcity[testFinished name='test2' timestamp='...']",
                            "##teamcity[testSuiteStarted name='${extraPath}subSuite1' timestamp='...']",
                            "##teamcity[flowStarted flowId='topSuite${iSepEsc}subSuite1' parent='topSuite']",
                            "##teamcity[testStarted name='test1' timestamp='...']",
                            "##teamcity[testFinished name='test1' timestamp='...']",
                            "##teamcity[flowFinished flowId='topSuite${iSepEsc}subSuite1']",
                            "##teamcity[testSuiteFinished name='${extraPath}subSuite1' timestamp='...']",
                            "##teamcity[testStarted name='test3' timestamp='...']",
                            "##teamcity[testFinished name='test3' timestamp='...']",
                            "##teamcity[testSuiteStarted name='${extraPath}subSuite2' timestamp='...']",
                            "##teamcity[flowStarted flowId='topSuite${iSepEsc}subSuite2' parent='topSuite']",
                            "##teamcity[testStarted name='test1' timestamp='...']",
                            "##teamcity[testFailed name='test1' timestamp='...' message='intentionally'" +
                                " details='AssertionError']",
                            "##teamcity[testFinished name='test1' timestamp='...']",
                            "##teamcity[flowFinished flowId='topSuite${iSepEsc}subSuite2']",
                            "##teamcity[testSuiteFinished name='${extraPath}subSuite2' timestamp='...']",
                            "##teamcity[flowFinished flowId='topSuite']",
                            "##teamcity[testSuiteFinished name='topSuite' timestamp='...']"
                        ),
                        exhaustive = true
                    )
            }
        }

    @Test
    fun concurrentOutput() = withTestFramework(TestSession(reportingMode = ReportingMode.Files)) {
        val suite by testSuite("concurrent") {
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

        val output = ConcurrentList<String>()
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

        output.comparableElements().assertContainsInOrder(
            listOf(
                "##teamcity[testSuiteStarted name='concurrent' timestamp='...']",
                "##teamcity[flowStarted flowId='concurrent' parent='TestSession$iSepEsc@Default']",
                "##teamcity[testSuiteStarted name='suite-1' timestamp='...']",
                "##teamcity[flowStarted flowId='concurrent${iSepEsc}suite-1' parent='concurrent']",
                "##teamcity[testStarted name='suite-1-test2' timestamp='...']",
                "##teamcity[testFinished name='suite-1-test2' timestamp='...']",
                "##teamcity[testStarted name='suite-1-test1' timestamp='...']",
                "##teamcity[testFinished name='suite-1-test1' timestamp='...']",
                "##teamcity[flowFinished flowId='concurrent${iSepEsc}suite-1']",
                "##teamcity[testSuiteFinished name='suite-1' timestamp='...']",
                "##teamcity[testSuiteStarted name='suite-2' timestamp='...']",
                "##teamcity[flowStarted flowId='concurrent${iSepEsc}suite-2' parent='concurrent']",
                "##teamcity[testStarted name='suite-2-test1' timestamp='...']",
                "##teamcity[testFinished name='suite-2-test1' timestamp='...']",
                "##teamcity[testStarted name='suite-2-test2' timestamp='...']",
                "##teamcity[testFinished name='suite-2-test2' timestamp='...']",
                "##teamcity[flowFinished flowId='concurrent${iSepEsc}suite-2']",
                "##teamcity[testSuiteFinished name='suite-2' timestamp='...']",
                "##teamcity[testStarted name='test2' timestamp='...']",
                "##teamcity[testFinished name='test2' timestamp='...']",
                "##teamcity[testStarted name='test1' timestamp='...']",
                "##teamcity[testFinished name='test1' timestamp='...']",
                "##teamcity[flowFinished flowId='concurrent']",
                "##teamcity[testSuiteFinished name='concurrent' timestamp='...']"
            ),
            exhaustive = true
        )
    }

    private fun ConcurrentList<String>.comparableElements() = elements()
        .map { it.replace(timestampRegex, "timestamp='...'").replace(detailsRegex, "details='AssertionError'") }
}
