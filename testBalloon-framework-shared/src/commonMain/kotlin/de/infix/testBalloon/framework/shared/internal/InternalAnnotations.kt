package de.infix.testBalloon.framework.shared.internal

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

/**
 * Indicates that the function or constructor is invoked by framework-generated code.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.SOURCE)
@TestBalloonInternalApi
public annotation class InvokedByGeneratedCode
