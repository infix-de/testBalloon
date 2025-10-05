package de.infix.testBalloon.framework.internal

/**
 * Marker for a TestBalloon-internal API, which must have public visibility for technical reasons.
 *
 * Please do not use this API, as it is not stable and may change without notice at any time.
 */
@RequiresOptIn(
    message = "This internal API is public for technical reasons. Please do not use, it may change any time.",
    level = RequiresOptIn.Level.ERROR
)
public annotation class TestBalloonInternalApi
