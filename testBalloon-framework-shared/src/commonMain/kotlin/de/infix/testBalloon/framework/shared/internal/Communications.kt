package de.infix.testBalloon.framework.shared.internal

// This file contains types used to communicate between framework modules.

/**
 * The framework-internal test reporting mode.
 */
@TestBalloonInternalApi
public enum class ReportingMode {
    IntellijIdeaLegacy,
    IntellijIdea,
    Files
}

/**
 * A framework-internal environment variable.
 */
@TestBalloonInternalApi
public enum class EnvironmentVariable {
    @Deprecated("To be removed", ReplaceWith("TESTBALLOON_INCLUDE_PATTERNS"))
    TEST_INCLUDE,

    TESTBALLOON_INCLUDE_PATTERNS,
    TESTBALLOON_EXCLUDE_PATTERNS,
    TESTBALLOON_REPORTING,
    TESTBALLOON_REPORTING_PATH_LIMIT
}

/**
 * The framework-internal debug level.
 */
@TestBalloonInternalApi
public enum class DebugLevel {
    NONE,
    BASIC,
    DISCOVERY,
    CODE
}
