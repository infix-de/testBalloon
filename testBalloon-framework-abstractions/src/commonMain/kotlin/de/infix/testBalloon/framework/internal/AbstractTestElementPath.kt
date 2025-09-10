package de.infix.testBalloon.framework.internal

/**
 * A path segment's name as an external ID, which is stable and copyable outside a single TestSession.
 */
@TestBalloonInternalApi
public fun String.externalId(): String = replace(' ', NON_BREAKING_SPACE).replace(".", LOW_DOT)

private const val NON_BREAKING_SPACE = '\u00a0'
private const val LOW_DOT = "𛲔"

/**
 * The copyable separator between path segments.
 */
@TestBalloonInternalApi
public const val PATH_SEGMENT_SEPARATOR: String = "$NON_BREAKING_SPACE↘$NON_BREAKING_SPACE"
