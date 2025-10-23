package de.infix.testBalloon.integration.kotest.assertions

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.coroutineContext
import io.kotest.assertions.errorCollectorContextElement

public actual fun TestConfig.kotestAssertionsSupport(): TestConfig = coroutineContext(errorCollectorContextElement)
