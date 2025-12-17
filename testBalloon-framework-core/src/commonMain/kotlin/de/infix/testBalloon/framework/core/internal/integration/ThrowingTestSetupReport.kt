package de.infix.testBalloon.framework.core.internal.integration

import de.infix.testBalloon.framework.core.TestElement
import de.infix.testBalloon.framework.core.internal.TestSetupReport

/**
 * A [TestSetupReport] which relays setup errors to the standard exception handling mechanism.
 */
internal class ThrowingTestSetupReport : TestSetupReport() {
    override fun add(event: TestElement.Event) {
        if (event is TestElement.Event.Finished && event.throwable != null) {
            if (event.throwable is TestConfigurationError) {
                throw event.throwable
            } else {
                throw TestConfigurationError("Could not configure ${event.element}", event.throwable)
            }
        }
    }
}

internal class TestConfigurationError(message: String, cause: Throwable) : Error(message, cause)
