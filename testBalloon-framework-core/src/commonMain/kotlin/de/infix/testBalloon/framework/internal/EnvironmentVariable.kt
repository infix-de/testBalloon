package de.infix.testBalloon.framework.internal

import de.infix.testBalloon.framework.testPlatform

internal fun EnvironmentVariable.value(): String? = testPlatform.environment(name)
