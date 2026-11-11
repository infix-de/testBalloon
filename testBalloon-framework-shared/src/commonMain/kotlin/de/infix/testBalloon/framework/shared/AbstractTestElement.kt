@file:OptIn(TestBalloonInternalApi::class)

package de.infix.testBalloon.framework.shared

import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi

/**
 * An element of a test hierarchy. Can be a test or a suite.
 */
@TestElementDsl
@SubclassOptInRequired(markerClass = arrayOf(TestBalloonInternalApi::class))
public interface AbstractTestElement {
    /**
     * A path uniquely identifying a test element in its test hierarchy.
     */
    @SubclassOptInRequired(markerClass = arrayOf(TestBalloonInternalApi::class))
    public interface Path {
        public override fun toString(): String
    }

    public val testElementPath: Path
    public val testElementIsEnabled: Boolean
}

/**
 * A test containing test logic which throws (assertion errors) on failure.
 */
@SubclassOptInRequired(markerClass = arrayOf(TestBalloonInternalApi::class))
public interface AbstractTest : AbstractTestElement

/**
 * A test suite comprised of a number of children (tests and/or suites).  A suite may not contain test logic.
 */
@SubclassOptInRequired(markerClass = arrayOf(TestBalloonInternalApi::class))
public interface AbstractTestSuite : AbstractTestElement

/**
 * A compilation module's root test suite, typically holding the module-wide default configuration.
 *
 * A compilation module may declare at most one test session. It is the root of the test element hierarchy.
 */
@SubclassOptInRequired(markerClass = arrayOf(TestBalloonInternalApi::class))
public interface AbstractTestSession : AbstractTestSuite
