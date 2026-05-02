package de.infix.testBalloon.framework.core.internal

import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.shared.internal.EnvironmentVariable

internal fun EnvironmentVariable.value(): String? = testPlatform.environment(name)
