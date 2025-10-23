package de.infix.testBalloon.framework.shared

/**
 * An element of a test hierarchy. Can be a test or a suite.
 */
@TestElementDsl
public interface AbstractTestElement {
    /**
     * A path uniquely identifying a test element in its test hierarchy.
     */
    public interface Path {
        public override fun toString(): String
    }

    public val testElementPath: Path
    public val testElementIsEnabled: Boolean
}

/**
 * A test containing test logic which raises assertion errors on failure.
 */
public interface AbstractTest : AbstractTestElement

/**
 * A test suite declaring a number of children (tests and/or suites).  A suite may not contain test logic.
 */
public interface AbstractTestSuite : AbstractTestElement

/**
 * A compilation module's root test suite, typically holding the module-wide default configuration.
 *
 * A compilation module may declare at most one test session. It is the root of the test element hierarchy.
 */
public interface AbstractTestSession : AbstractTestSuite
