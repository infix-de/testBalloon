package de.infix.testBalloon.framework.core

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * A report containing a sequence of test events, each of which will be [add]ed during execution.
 *
 * During execution, a report is expected to contain [TestElementEvent.Starting] and [TestElementEvent.Finished]
 * for every element in the test element hierarchy. This includes events for disabled elements.
 */
public abstract class TestExecutionReport {
    public abstract suspend fun add(event: TestElementEvent)
}

/**
 * An event occurring as part of a test element's setup or execution.
 */
public sealed class TestElementEvent(public val element: TestElement) {
    @ExperimentalTime
    public val instant: Instant = Clock.System.now()

    public class Starting(element: TestElement) : TestElementEvent(element)

    public class Finished(
        element: TestElement,
        public val startingEvent: Starting,
        public val throwable: Throwable? = null
    ) : TestElementEvent(element) {

        public val succeeded: Boolean get() = throwable == null
        public val failed: Boolean get() = throwable != null

        override fun toString(): String = "${super.toString()} – throwable=$throwable"
    }

    override fun toString(): String = "$element: ${this::class.simpleName}"
}
