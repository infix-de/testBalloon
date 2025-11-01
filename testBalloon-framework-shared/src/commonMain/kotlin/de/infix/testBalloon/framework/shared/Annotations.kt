package de.infix.testBalloon.framework.shared

/**
 * Designates a class or function registering a test suite or test.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class TestRegistering

/**
 * Makes a String parameter receive the fully qualified name of its function's or constructor's caller.
 *
 * This annotation requires a top-level `@[TestRegistering]` function or class constructor. A `@[TestElementName]`
 * parameter must be of type String and have a default value (typically, "").
 *
 * If the call site of the function or constructor does not supply an actual value for that `@[TestElementName]`
 * parameter, the compiler will insert the caller's fully qualified class or property name.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
public annotation class TestElementName

/**
 * Makes a String parameter receive the simple name of its function's or constructor's caller.
 *
 * For prerequisites and mechanism, see [TestElementName].
 *
 * The display name generated is the simple name of the caller's class or property.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
public annotation class TestDisplayName

/**
 * Makes the annotated class part of the test element DSL. See the Kotlin docs on [DslMarker] for details.
 */
@DslMarker
public annotation class TestElementDsl
