package de.infix.testBalloon.integration.blockingDetection

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestElement
import de.infix.testBalloon.framework.core.testScope

public enum class BlockingDetection {
    DISABLED,
    ERROR,
    PRINT
}

/**
 * Returns a test configuration which determines blocking call detection for a [TestElement] hierarchy.
 *
 * NOTES:
 * - Blocking call detection is only available on the JVM. This configuration has no effect on other platforms.
 * - Blocking call detection is only available with [kotlinx.coroutines.Dispatchers.Default], which is used by
 *   default.
 * - Enabling blocking call detection disables [TestConfig.testScope]. This is necessary since kotlinx-coroutines
 *   uses blocking calls in `TestScope`.
 *
 * Child elements inherit this setting's effect.
 */
public expect fun TestConfig.blockingDetection(mode: BlockingDetection = BlockingDetection.ERROR): TestConfig
