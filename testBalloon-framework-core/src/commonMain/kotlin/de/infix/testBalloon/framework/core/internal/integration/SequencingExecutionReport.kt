package de.infix.testBalloon.framework.core.internal.integration

import de.infix.testBalloon.framework.core.TestElement
import de.infix.testBalloon.framework.core.TestElementEvent
import de.infix.testBalloon.framework.core.TestExecutionReport
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.internal.GuardedBy
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A [TestExecutionReport] which reorders concurrent events to forward them in a strict depth-first tree order.
 *
 * It invokes the downstream [forward] method with events ordered like this:
 * - element starting
 *   - child starting
 *   - child finishing
 *   ...
 * - element finishing
 */
internal abstract class SequencingExecutionReport : TestExecutionReport() {
    private val reorderingMutex = Mutex()

    /** The lowermost element that has started, but not finished yet. */
    @GuardedBy("reorderingMutex")
    private var elementInProgress: TestElement? = null

    final override suspend fun add(event: TestElementEvent) {
        // Convert concurrently incoming events to a depth-first tree order for downstream forwarding,
        // storing events and picking them up as they become eligible for downstream forwarding.

        reorderingMutex.withLock {
            val element = event.element

            when (event) {
                is TestElementEvent.Starting -> {
                    check(element.recentEvent == null) {
                        "starting event for $element preceded by ${element.recentEvent}"
                    }
                    element.recentEvent = event

                    // Forward `Starting` events immediately for a test suite with a parent in progress.
                    if (element.testElementParent == elementInProgress && element is TestSuite) {
                        forwardTracking(event)
                    }
                }

                is TestElementEvent.Finished -> {
                    check(element.recentEvent == event.startingEvent) {
                        "finish event for $element preceded by ${element.recentEvent}"
                    }
                    element.recentEvent = event

                    if (element == elementInProgress || element.testElementParent == elementInProgress) {
                        // Forward `Finished` events immediately for an element in progress, or for an element with a
                        // parent in progress. In both cases, events for all children will be forwarded as well.
                        forwardFinish(event)

                        // Forward events for all siblings that have finished in the meantime.
                        element.testElementParent?.forwardEventsForFinishedChildren()
                    }
                }
            }
        }
    }

    /**
     * Forwards a `Finished` event downstream after replaying any non-forwarded events that must precede it.
     */
    private suspend fun forwardFinish(finishedEvent: TestElementEvent.Finished) {
        val element = finishedEvent.element

        // Forward the element's `Starting` event if not already done.
        if (element.forwardingState == TestElement.ForwardingState.NOT_FORWARDED) {
            forwardTracking(finishedEvent.startingEvent)
        }

        // Forward `Starting` and `Finished` events of all children (recursively) if not already done.
        if (element is TestSuite) {
            element.forwardEventsForFinishedChildren()
        }

        // Forward the element's `Finished` event, closing its forwarding tree.
        forwardTracking(finishedEvent)
    }

    /**
     * Forwards events for all finished children that haven't been forwarded yet.
     */
    private suspend fun TestSuite.forwardEventsForFinishedChildren() {
        for (childElement in testElementChildren) {
            val childEvent = childElement.recentEvent
            if (childEvent is TestElementEvent.Finished &&
                childElement.forwardingState != TestElement.ForwardingState.FINISH_FORWARDED
            ) {
                forwardFinish(childEvent)
            }
        }
    }

    /**
     * Forwards an event downstream with state tracking.
     */
    private suspend fun forwardTracking(event: TestElementEvent) {
        val element = event.element

        when (event) {
            is TestElementEvent.Starting -> {
                check(element.forwardingState == TestElement.ForwardingState.NOT_FORWARDED) {
                    "start forwarding for $element preceded by ${element.forwardingState}"
                }
                element.forwardingState = TestElement.ForwardingState.START_FORWARDED
                elementInProgress = element
            }

            is TestElementEvent.Finished -> {
                check(element.forwardingState == TestElement.ForwardingState.START_FORWARDED) {
                    "finish forwarding for $element preceded by ${element.forwardingState}"
                }
                element.forwardingState = TestElement.ForwardingState.FINISH_FORWARDED
                elementInProgress = element.testElementParent
            }
        }

        forward(event)
    }

    /**
     * Forwards an event downstream.
     */
    protected abstract suspend fun forward(event: TestElementEvent)
}
