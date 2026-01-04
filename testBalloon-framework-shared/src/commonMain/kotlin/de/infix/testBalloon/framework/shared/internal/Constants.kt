package de.infix.testBalloon.framework.shared.internal

/**
 * Framework-internal constants.
 */
@TestBalloonInternalApi
public object Constants {
    public const val CORE_PACKAGE_NAME: String = "de.infix.testBalloon.framework.core"
    public const val CORE_INTERNAL_PACKAGE_NAME: String = "$CORE_PACKAGE_NAME.internal"

    public const val SHARED_PACKAGE_NAME: String = "de.infix.testBalloon.framework.shared"
    public const val SHARED_INTERNAL_PACKAGE_NAME: String = "$SHARED_PACKAGE_NAME.internal"

    public const val ENTRY_POINT_PACKAGE_NAME: String = "$SHARED_INTERNAL_PACKAGE_NAME.entryPoint"
    public const val ENTRY_POINT_ANCHOR_FILE_NAME: String = "EntryPointAnchor.kt"
    public const val ENTRY_POINT_ANCHOR_CLASS_NAME: String = "$ENTRY_POINT_PACKAGE_NAME.EntryPointAnchorKt"

    public const val JVM_DISCOVERY_RESULT_PROPERTY: String = "testFrameworkDiscoveryResult"
    public const val JVM_DISCOVERY_RESULT_PROPERTY_GETTER: String = "getTestFrameworkDiscoveryResult"

    public const val JUNIT4_ENTRY_POINT_SIMPLE_CLASS_NAME: String = "TestBalloonJUnit4"
    public const val JUNIT4_RUNNER_CLASS_NAME: String =
        "$CORE_INTERNAL_PACKAGE_NAME.integration.TestBalloonJUnit4Runner"

    public const val JUNIT_ENGINE_ID: String = "de.infix.testBalloon"

    public const val GRADLE_EXTENSION_NAME: String = "testBalloon"
    public const val GRADLE_PROPERTY_PREFIX: String = "testBalloon"

    /** The internal separator between elements of a test element path. */
    public const val INTERNAL_PATH_ELEMENT_SEPARATOR: Char = '↘'

    /** A replacement for a space character. */
    public const val ESCAPED_SPACE: Char = '\u00a0'

    /** Marks appearing in an element's reporting coordinates. */
    public const val INTERNAL_ELEMENT_REPORTING_COORDINATES_BEGIN_MARK: String = "〈tb〈"
    public const val INTERNAL_ELEMENT_REPORTING_COORDINATES_END_MARK: String = "〉tb〉"
    public const val INTERNAL_ELEMENT_REPORTING_COORDINATES_COMPONENT_SEPARATOR: Char = '⬥'

    /** The internal separator between path patterns. */
    public const val INTERNAL_PATH_PATTERN_SEPARATOR: Char = '⬥'
}
