package de.infix.testBalloon.framework.internal

/**
 * This file contains symbols shared across framework modules.
 */

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

    /** The internal separator between elements of a test element path. */
    public const val INTERNAL_PATH_ELEMENT_SEPARATOR: Char = '↘'

    /** The internal separator between path patterns. */
    public const val INTERNAL_PATH_PATTERN_SEPARATOR: Char = '⬥'
}
