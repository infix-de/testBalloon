package de.infix.testBalloon.framework.internal.integration

import de.infix.testBalloon.framework.Test
import de.infix.testBalloon.framework.TestElementEvent
import de.infix.testBalloon.framework.TestExecutionReport
import de.infix.testBalloon.framework.TestSuite
import de.infix.testBalloon.framework.internal.printlnFixed
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.char
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * A [TestExecutionReport] in JetBrains' TeamCity format on stdout or via an [outputEntry] function.
 *
 * This class is a [SequencingExecutionReport] because the TeamCity protocol does not support concurrency.
 */
@OptIn(ExperimentalTime::class)
internal class TeamCityTestExecutionReport(val outputEntry: (String) -> Unit = ::printlnFixed) :
    SequencingExecutionReport() {

    /**
     * Forwards an event downstream.
     */
    override suspend fun forward(event: TestElementEvent) {
        val element = event.element
        val elementParent = element.testElementParent
        val isIgnoredTest = !element.testElementIsEnabled && element is Test

        when (event) {
            is TestElementEvent.Starting -> {
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
    day()
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
