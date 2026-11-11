package de.infix.testBalloon.framework.core

import org.jetbrains.kotlinx.library.api.watchdog.ExemptionReason
import org.jetbrains.kotlinx.library.api.watchdog.IntentionallyOpen

/**
 * A report containing a sequence of test events, each of which will be [add]ed during execution.
 *
 * During execution, a report is expected to contain [TestElement.Event.Starting] and [TestElement.Event.Finished]
 * for every element in the test element hierarchy. This includes events for disabled elements.
 */
@IntentionallyOpen(reason = ExemptionReason.API_DESIGN)
public abstract class TestExecutionReport {
    public abstract suspend fun add(event: TestElement.Event)
}
