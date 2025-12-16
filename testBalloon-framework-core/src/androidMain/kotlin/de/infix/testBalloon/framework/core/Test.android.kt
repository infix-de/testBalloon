package de.infix.testBalloon.framework.core

import de.infix.testBalloon.framework.core.internal.integration.platformDescription
import org.junit.runner.Description

/**
 * The JUnit 4 description for this [Test].
 *
 * This property exists to support integration of JUnit 4-based libraries.
 */
@TestBalloonExperimentalApi
public val Test.jUnit4Description: Description get() = platformDescription
