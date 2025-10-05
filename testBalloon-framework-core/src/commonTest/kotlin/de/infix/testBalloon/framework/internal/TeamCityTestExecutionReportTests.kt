package de.infix.testBalloon.framework.internal

import de.infix.testBalloon.framework.ConcurrentList
import de.infix.testBalloon.framework.TestElement
import de.infix.testBalloon.framework.TestElementEvent
import de.infix.testBalloon.framework.TestSession
import de.infix.testBalloon.framework.TestSuite
import de.infix.testBalloon.framework.assertContainsInOrder
import de.infix.testBalloon.framework.internal.integration.TeamCityTestExecutionReport
import de.infix.testBalloon.framework.internal.integration.ThrowingTestConfigurationReport
import de.infix.testBalloon.framework.testSuite
import de.infix.testBalloon.framework.withTestFramework
import de.infix.testBalloon.framework.withTestReport
import kotlin.also
import kotlin.getValue
import kotlin.test.Test
import kotlin.test.fail

class TeamCityTestExecutionReportTests {
    private val timestampRegex = Regex("""timestamp='[^']+'""")
    private val detailsRegex = Regex("""details='[^:]+Error:[^']*'""")

    @Test
    fun basicOutputIntelliJ() = basicOutputTest(ReportingMode.INTELLIJ_IDEA, "topSuite|0x00a0|0x2198|0x00a0")

    @Test
    fun basicOutputFiles() = basicOutputTest(ReportingMode.FILES, "")

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
                            "##teamcity[flowStarted flowId='topSuite' parent='@Default']",
                            "##teamcity[testStarted name='test1' timestamp='...']",
                            "##teamcity[testFinished name='test1' timestamp='...']",
                            "##teamcity[testStarted name='test2' timestamp='...']",
                            "##teamcity[testFailed name='test2' timestamp='...' message='intentionally'" +
                                " details='AssertionError']",
                            "##teamcity[testFinished name='test2' timestamp='...']",
                            "##teamcity[testSuiteStarted name='${extraPath}subSuite1' timestamp='...']",
                            "##teamcity[flowStarted flowId='topSuite||subSuite1' parent='topSuite']",
                            "##teamcity[testStarted name='test1' timestamp='...']",
                            "##teamcity[testFinished name='test1' timestamp='...']",
                            "##teamcity[flowFinished flowId='topSuite||subSuite1']",
                            "##teamcity[testSuiteFinished name='${extraPath}subSuite1' timestamp='...']",
                            "##teamcity[testStarted name='test3' timestamp='...']",
                            "##teamcity[testFinished name='test3' timestamp='...']",
                            "##teamcity[testSuiteStarted name='${extraPath}subSuite2' timestamp='...']",
                            "##teamcity[flowStarted flowId='topSuite||subSuite2' parent='topSuite']",
                            "##teamcity[testStarted name='test1' timestamp='...']",
                            "##teamcity[testFailed name='test1' timestamp='...' message='intentionally'" +
                                " details='AssertionError']",
                            "##teamcity[testFinished name='test1' timestamp='...']",
                            "##teamcity[flowFinished flowId='topSuite||subSuite2']",
                            "##teamcity[testSuiteFinished name='${extraPath}subSuite2' timestamp='...']",
                            "##teamcity[flowFinished flowId='topSuite']",
                            "##teamcity[testSuiteFinished name='topSuite' timestamp='...']"
                        ),
                        exhaustive = true
                    )
            }
        }

    @Test
    fun concurrentOutput() = withTestFramework(TestSession(reportingMode = ReportingMode.FILES)) {
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
        TestSession.global.parameterize(TestElement.AllInSelection, ThrowingTestConfigurationReport())

        val output = ConcurrentList<String>()
        val report = TeamCityTestExecutionReport { output.add(it) }

        // Session and compartment must be part of the report, otherwise it would remain empty.
        val sessionStartingEvent = TestElementEvent.Starting(TestSession.global).also { report.add(it) }
        val compartmentStartingEvent =
            TestElementEvent.Starting(TestSession.global.defaultCompartment).also { report.add(it) }

        val startingEvents = mutableMapOf<TestElement, TestElementEvent.Starting>()
        listOf(
            "/S",
            "suite-1/S",
            "suite-2/S", // concurrent start, delayed reporting
            "suite-2.suite-2-test2", // reported in tree order (2)
            "suite-2.suite-2-test1", // reported in tree order (1)
            "suite-1.suite-1-test2", // reported in start order (1)
            "suite-2/F", // finished before suite-1, children reported in tree order, not start order
            "suite-1.suite-1-test1", // reported in start order (2)
            "suite-1/F",
            "test2",
            "test1",
            "/F"
        ).forEach { elementPathPlusOptionalSuffix ->
            val elementPath = elementPathPlusOptionalSuffix.substringBeforeLast("/")
            val suffix = elementPathPlusOptionalSuffix.substringAfterLast("/", "")
            val segments = if (elementPath.isEmpty()) listOf() else elementPath.split(".")
            val element = segments.fold<String, TestElement>(suite) { element, childName ->
                (element as TestSuite).testElementChildren.first { it.testElementName == childName }
            }
            when (suffix) {
                "S" -> report.add(TestElementEvent.Starting(element).also { startingEvents[element] = it })
                "F" -> report.add(TestElementEvent.Finished(element, startingEvents[element]!!))
                else -> {
                    val startingEvent = TestElementEvent.Starting(element)
                    report.add(startingEvent)
                    report.add(TestElementEvent.Finished(element, startingEvent))
                }
            }
        }

        report.add(TestElementEvent.Finished(compartmentStartingEvent.element, compartmentStartingEvent))
        report.add(TestElementEvent.Finished(sessionStartingEvent.element, sessionStartingEvent))

        output.comparableElements().assertContainsInOrder(
            listOf(
                "##teamcity[testSuiteStarted name='concurrent' timestamp='...']",
                "##teamcity[flowStarted flowId='concurrent' parent='@Default']",
                "##teamcity[testSuiteStarted name='suite-1' timestamp='...']",
                "##teamcity[flowStarted flowId='concurrent||suite-1' parent='concurrent']",
                "##teamcity[testStarted name='suite-1-test2' timestamp='...']",
                "##teamcity[testFinished name='suite-1-test2' timestamp='...']",
                "##teamcity[testStarted name='suite-1-test1' timestamp='...']",
                "##teamcity[testFinished name='suite-1-test1' timestamp='...']",
                "##teamcity[flowFinished flowId='concurrent||suite-1']",
                "##teamcity[testSuiteFinished name='suite-1' timestamp='...']",
                "##teamcity[testSuiteStarted name='suite-2' timestamp='...']",
                "##teamcity[flowStarted flowId='concurrent||suite-2' parent='concurrent']",
                "##teamcity[testStarted name='suite-2-test1' timestamp='...']",
                "##teamcity[testFinished name='suite-2-test1' timestamp='...']",
                "##teamcity[testStarted name='suite-2-test2' timestamp='...']",
                "##teamcity[testFinished name='suite-2-test2' timestamp='...']",
                "##teamcity[flowFinished flowId='concurrent||suite-2']",
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
        .drop(4) // testSuiteStarted+flowStarted for session, compartment
        .dropLast(4) // testSuiteFinished+flowFinished for compartment, session
}
