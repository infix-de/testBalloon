package de.infix.testBalloon.framework

import de.infix.testBalloon.framework.internal.integration.platformDescription
import org.junit.runner.Description

/**
 * The JUnit 4 description for this [Test].
 *
 * This property exists to support integration of JUnit 4-based libraries.
 */
@TestBalloonExperimentalApi
public val TestExecutionScope.jUnit4Description: Description get() = test.platformDescription
