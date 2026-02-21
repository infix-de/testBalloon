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
 * Marker for an internal testing API, intended to support testing TestBalloon add-ons.
 */
@RequiresOptIn(
    message = "This internal API is intended to support testing TestBalloon add-ons," +
        " where tests require access to the framework's setup. Please do not use it for any other purpose." +
        " No stability guarantees are provided for this API.",
    level = RequiresOptIn.Level.ERROR
)
public annotation class TestBalloonInternalTestingApi

/**
 * Indicates that the function or constructor is invoked by framework-generated code.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.SOURCE)
@TestBalloonInternalApi
public annotation class InvokedByGeneratedCode
