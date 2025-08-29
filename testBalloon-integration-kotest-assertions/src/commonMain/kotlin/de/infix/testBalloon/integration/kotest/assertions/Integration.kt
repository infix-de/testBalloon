package de.infix.testBalloon.integration.kotest.assertions

import de.infix.testBalloon.framework.TestConfig
import de.infix.testBalloon.framework.TestElement

/**
 * Returns a test configuration which enables Kotest assertions for a [TestElement] tree.
 *
 * Some Kotest's assertion library functions like `assertSoftly` and `withClue` require a special setup, which
 * this configuration provides.
 *
 * Child elements inherit this setting's effect.
 */
public expect fun TestConfig.kotestAssertionsSupport(): TestConfig
