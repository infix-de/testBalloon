package de.infix.testBalloon.integration.kotest.assertions

import de.infix.testBalloon.framework.TestConfig

public actual fun TestConfig.kotestAssertionsSupport(): TestConfig = TestConfig
// NOTE: Change this once Kotest fully supports an ErrorCollector on multithreaded platforms other than the JVM.
