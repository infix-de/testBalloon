package de.infix.testBalloon.framework.internal

/**
 * A path segment's name as an external ID, which will not be broken up by platform-specific tooling.
 *
 * Specific characters are replaced with counterparts of similar appearance, because
 * platform tooling would otherwise recognize them as path element separators:
 * - JS/Node: '.' (regular dot)
 * - JS/Browser: '.' (regular dot), ' ' (regular space)
 * - Native: '.' (regular dot)
 */
@TestBalloonInternalApi
public fun String.externalId(): String = replace(' ', NON_BREAKING_SPACE).replace(".", LOW_DOT)

private const val NON_BREAKING_SPACE = '\u00a0'
private const val LOW_DOT = "𛲔"

/**
 * The separator between path segments, human-readable and well visible.
 */
@TestBalloonInternalApi
public const val PATH_SEGMENT_SEPARATOR: String = "$NON_BREAKING_SPACE↘$NON_BREAKING_SPACE"

/**
 * The separator between path patterns, human-readable.
 */
@TestBalloonInternalApi
public const val PATH_PATTERN_SEPARATOR: Char = '⬥'

/**
 * The framework's test reporting mode.
 */
@TestBalloonInternalApi
public enum class TestReportingMode {
    INTELLIJ_IDEA,
    FILES
}

/**
 * A framework environment variable.
 */
@TestBalloonInternalApi
public enum class EnvironmentVariable {
    @Deprecated("To be removed", ReplaceWith("TESTBALLOON_INCLUDE"))
    TEST_INCLUDE,

    TESTBALLOON_INCLUDE,
    TESTBALLOON_EXCLUDE,
    TESTBALLOON_REPORTING
}

/**
 * The unified debug level for Gradle plugin, compiler plugin and framework.
 */
@TestBalloonInternalApi
public enum class DebugLevel {
    NONE,
    BASIC,
    DISCOVERY,
    CODE
}

/**
 * Indicates that the function or constructor is invoked by framework-generated code.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.SOURCE)
@TestBalloonInternalApi
public annotation class InvokedByGeneratedCode
