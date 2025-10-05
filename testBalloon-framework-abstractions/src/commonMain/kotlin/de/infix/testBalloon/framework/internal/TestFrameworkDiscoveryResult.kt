package de.infix.testBalloon.framework.internal

import de.infix.testBalloon.framework.AbstractTestSuite

/**
 * The result of a compilation module's test discovery, used internally by framework-generated code.
 */
@TestBalloonInternalApi
public class TestFrameworkDiscoveryResult @InvokedByGeneratedCode constructor(
    public val topLevelTestSuites: Array<AbstractTestSuite>
)
