package de.infix.testBalloon.framework.internal

/**
 * This file contains parameters shared across framework components.
 */

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
 * The framework-internal test reporting mode.
 */
@TestBalloonInternalApi
public enum class ReportingMode {
    INTELLIJ_IDEA,
    FILES
}

/**
 * A framework-internal environment variable.
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
 * Indicates that the function or constructor is invoked by framework-generated code.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.SOURCE)
@TestBalloonInternalApi
public annotation class InvokedByGeneratedCode

/**
 * Framework-internal constants.
 */
@TestBalloonInternalApi
public object Constants {
    public const val ABSTRACTIONS_PACKAGE_NAME: String = "de.infix.testBalloon.framework"

    public const val CORE_PACKAGE_NAME: String = "de.infix.testBalloon.framework"
    public const val CORE_INTERNAL_PACKAGE_NAME: String = "$CORE_PACKAGE_NAME.internal"

    public const val ENTRY_POINT_PACKAGE_NAME: String = "$CORE_INTERNAL_PACKAGE_NAME.entryPoint"
    public const val ENTRY_POINT_ANCHOR_FILE_NAME: String = "EntryPointAnchor.kt"
    public const val ENTRY_POINT_ANCHOR_CLASS_NAME: String = "$ENTRY_POINT_PACKAGE_NAME.EntryPointAnchorKt"

    public const val JVM_DISCOVERY_RESULT_PROPERTY: String = "testFrameworkDiscoveryResult"
    public const val JVM_DISCOVERY_RESULT_PROPERTY_GETTER: String = "getTestFrameworkDiscoveryResult"

    public const val JUNIT_ENGINE_NAME: String = "de.infix.testBalloon"

    public const val GRADLE_EXTENSION_NAME: String = "testBalloon"
    public const val GRADLE_PROPERTY_PREFIX: String = "testBalloon"
}
