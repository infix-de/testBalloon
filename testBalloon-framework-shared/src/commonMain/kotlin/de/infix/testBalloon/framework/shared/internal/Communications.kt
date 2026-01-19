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
    TESTBALLOON_INCLUDE_PATTERNS,
    TESTBALLOON_EXCLUDE_PATTERNS,
    TESTBALLOON_REPORTING,
    TESTBALLOON_REPORTING_PATH_LIMIT,
    TESTBALLOON_REPORTING_PATH_LIMIT_BELOW_TOP_LEVEL
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

/**
 * Returns the string in a form that is safe to use as a `TestElement.Path.internalId` or a component thereof.
 */
@TestBalloonInternalApi
public fun String.safeAsInternalId(): String = safelyTransformed(internalIdReplacements)

@OptIn(TestBalloonInternalApi::class)
private val internalIdReplacements = mapOf(
    ' ' to Constants.ESCAPED_SPACE, // prevents JS frameworks from replacing a space with a dot
    '/' to '⧸' // prevents crashing Android device tests
)

/**
 * Returns the string in a form that is safe to use (digestible by external components) with additional replacements.
 */
@TestBalloonInternalApi
public fun String.safelyTransformed(replacementCharacters: Map<Char, Char>): String = buildString(length) {
    for (character in this@safelyTransformed) {
        val replacementCharacter = replacementCharacters[character]
        append(
            when {
                replacementCharacter != null -> replacementCharacter
                character.code < 32 -> '�'
                else -> character
            }
        )
    }
}
