package de.infix.testBalloon.framework.internal.integration

import de.infix.testBalloon.framework.Test
import de.infix.testBalloon.framework.TestElement
import de.infix.testBalloon.framework.TestElementEvent
import de.infix.testBalloon.framework.TestExecutionReport
import de.infix.testBalloon.framework.TestSuite
import de.infix.testBalloon.framework.internal.GuardedBy
import de.infix.testBalloon.framework.internal.printlnFixed
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.char
import kotlin.time.ExperimentalTime

/**
 * A [TestExecutionReport] in JetBrains' TeamCity format on stdout or via an [outputEntry] function.
 *
 * Note: The TeamCity protocol supports a test element hierarchy, but it does not support concurrency.
 * Output elements must report start and finish in strict depth-first tree order:
 * - element starting
 *   - child starting
 *   - child finishing
 *   ...
 * - element finishing
 *
 * Since a [TestExecutionReport] must always supports concurrently incoming events, this report reorders events before
 * reporting them downstream via [outputEntry].
 */
@OptIn(ExperimentalTime::class)
internal class TeamCityTestExecutionReport(val outputEntry: (String) -> Unit = ::printlnFixed) : TestExecutionReport() {
    private val reorderingMutex = Mutex()

    /** The lowermost element that has started, but not finished yet. */
    @GuardedBy("reorderingMutex")
    private var elementInProgress: TestElement? = null

    override suspend fun add(event: TestElementEvent) {
        // Convert concurrently incoming events to a depth-first tree order for downstream reporting,
        // storing events and picking them up as they become eligible for downstream acceptance.

        reorderingMutex.withLock {
            val element = event.element

            when (event) {
                is TestElementEvent.Starting -> {
                    check(element.recentEvent == null) {
                        "starting event for $element preceded by ${element.recentEvent}"
                    }
                    element.recentEvent = event

                    // Report `Starting` events immediately for a test suite with a parent in progress.
                    if (element.testElementParent == elementInProgress && element is TestSuite) {
                        report(event)
                    }
                }

                is TestElementEvent.Finished -> {
                    check(element.recentEvent == event.startingEvent) {
                        "finish event for $element preceded by ${element.recentEvent}"
                    }
                    element.recentEvent = event

                    if (element == elementInProgress || element.testElementParent == elementInProgress) {
                        // Report `Finished` events immediately for an element in progress, or for an element with a parent
                        // in progress. In both cases, all children will be reported as well.
                        reportFinish(event)

                        // Report events for all siblings that have finished in the meantime.
                        element.testElementParent?.reportFinishedChildren()
                    }
                }
            }
        }
    }

    /**
     * Reports a `Finished` event downstream after replaying any non-reported events that must precede it.
     */
    private fun reportFinish(event: TestElementEvent.Finished) {
        val element = event.element

        // Report the element's `Starting` event if not already done.
        if (element.reportingState == TestElement.ReportingState.NOT_REPORTED) {
            report(event.startingEvent)
        }

        // Report `Starting` and `Finished` events of all children (recursively) if not already done.
        if (element is TestSuite) {
            element.reportFinishedChildren()
        }

        // Report the element's `Finished` event, closing its reporting tree.
        report(event)
    }

    /**
     * Reports events for all finished children that haven't been reported yet.
     */
    private fun TestSuite.reportFinishedChildren() {
        for (childElement in testElementChildren) {
            val childEvent = childElement.recentEvent
            if (childEvent is TestElementEvent.Finished &&
                childElement.reportingState != TestElement.ReportingState.FINISH_REPORTED
            ) {
                reportFinish(childEvent)
            }
        }
    }

    /**
     * Reports an event downstream.
     */
    private fun report(event: TestElementEvent) {
        val element = event.element
        val elementParent = element.testElementParent
        val isIgnoredTest = !element.testElementIsEnabled && element is Test

        when (event) {
            is TestElementEvent.Starting -> {
                check(element.reportingState == TestElement.ReportingState.NOT_REPORTED) {
                    "start report for $element preceded by ${element.reportingState}"
                }
                element.reportingState = TestElement.ReportingState.START_REPORTED
                elementInProgress = element

                if (isIgnoredTest) {
                    eventMessage(event, eventName = "Ignored")
                    // Note: TeamCity does not recognize an ignored suite, so all suites are reported normally.
                } else {
                    eventMessage(event, eventName = "Started")
                    if (element is TestSuite) {
                        message(messageName = "flowStarted") {
                            flowId(flowId = element.testElementPath, parentId = elementParent?.testElementPath)
                        }
                    }
                }
            }

            is TestElementEvent.Finished -> {
                check(element.reportingState == TestElement.ReportingState.START_REPORTED) {
                    "finish report for $element preceded by ${element.reportingState}"
                }
                element.reportingState = TestElement.ReportingState.FINISH_REPORTED
                elementInProgress = element.testElementParent

                if (!isIgnoredTest) {
                    if (element is TestSuite) {
                        message(messageName = "flowFinished") {
                            flowId(flowId = element.testElementPath)
                        }
                    }
                    event.throwable?.let { throwable ->
                        eventMessage(event, eventName = "Failed") {
                            throwable.message?.let { attribute("message", it) }
                            attribute("details", throwable.stackTraceToString())
                            // TODO: report `expect` and `actual` with type `comparisonFailure`
                        }
                    }
                    eventMessage(event, eventName = "Finished")
                }
            }
        }
    }

    private fun eventMessage(event: TestElementEvent, eventName: String, content: Message.() -> Unit = {}) {
        val elementName = if (event.element is Test) "test" else "testSuite"
        message("$elementName$eventName") {
            name(if (event.element is Test) event.element.testElementDisplayName else event.element.flattenedPath)
            timestamp(event.instant)
            content()
        }
    }

    private fun message(messageName: String, content: Message.() -> Unit) {
        val entry = StringBuilder()
        Message(entry).content()
        val message = "##teamcity[$messageName$entry]"
        // printlnFixed(message.replaceFirst("##", "§§"))
        outputEntry(message)
    }

    private class Message(private val entry: StringBuilder) {
        // For the TeamCity test report format, see:
        // https://www.jetbrains.com/help/teamcity/service-messages.html#Reporting+Tests
        fun name(name: String) {
            attribute("name", name)
        }

        fun timestamp(timestamp: Instant) {
            attribute("timestamp", timestamp.format(dateTimeFormat))
        }

        fun flowId(flowId: String, parentId: String? = null) {
            attribute("flowId", flowId)
            if (parentId != null) attribute("parent", parentId)
        }

        fun attribute(name: String, value: String) {
            entry.append(" $name='${value.asAttributeValue()}'")
        }
    }
}

/**
 * Returns the TeamCity attribute value encoding for this character sequence.
 */
private fun CharSequence.asAttributeValue() = buildString {
    // Reference: https://www.jetbrains.com/help/teamcity/service-messages.html#Escaped+Values
    for (c in this@asAttributeValue) {
        when (c) {
            '\'' -> append("|'")
            '\n' -> append("|n")
            '\r' -> append("|r")
            '|' -> append("||")
            '[' -> append("|[")
            ']' -> append("|]")

            in SAFE_ATTRIBUTE_VALUE_CHAR_SET -> append(c)

            else -> {
                append("|0x")
                @OptIn(ExperimentalStdlibApi::class)
                append(c.code.toShort().toHexString(HexFormat.Default))
                // `c.code` is an `Int` which would result in 8 hex digits, Short translates into 4 hex digits.
            }
        }
    }
}

private val SAFE_ATTRIBUTE_VALUE_CHAR_SET =
    (
        listOf('a'..'z', 'A'..'Z', '0'..'9')
            .map { it.toSet() }
            .reduce { accumulation, component -> accumulation + component }
        ) + " -()+,./:=?;!*#@${'$'}_%".toSet()

private val dateTimeFormat = DateTimeComponents.Format {
    year()
    char('-')
    monthNumber()
    char('-')
    dayOfMonth()
    char('T')
    hour()
    char(':')
    minute()
    char(':')
    second()
    char('.')
    secondFraction(fixedLength = 3)
    // NOTE: The docs say a 'Z' prefix is supported, but that doesn't seem to work.
}
