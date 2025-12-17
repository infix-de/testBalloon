package de.infix.testBalloon.framework.core.internal

import de.infix.testBalloon.framework.core.TestElement

/**
 * A report containing a sequence of test events, each of which will be [add]ed during configuration.
 *
 * During configuration, a report is expected to contain [TestElement.Event.Starting] and [TestElement.Event.Finished]
 * for every element in the test element hierarchy. This includes events for disabled elements.
 */
internal abstract class TestSetupReport {
    abstract fun add(event: TestElement.Event)
}
