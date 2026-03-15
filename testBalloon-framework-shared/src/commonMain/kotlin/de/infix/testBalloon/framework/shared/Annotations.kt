package de.infix.testBalloon.framework.shared

/**
 * Designates a function registering a test suite or test, or a class registering test suite content.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class TestRegistering

/**
 * Designates a test-registering function's String parameter as representing the test element's name.
 *
 * For a top-level [TestRegistering] function, this implies:
 * - A `@TestElementName` parameter must have a default value (typically, "").
 * - If the call site of the function does not supply an actual value for that `@TestElementName` parameter,
 *   the compiler will insert the caller's fully qualified property name.
 *
 * In addition, the IDE plugin uses this annotation to detect the element name position in a parameter list.
 * The optional [prefix] and [postfix] parameters make the IDE plugin aware of modifications to the element name,
 * so that a valid test element path can be produced by static analysis. Example for a Behavior/Gherkin-style
 * `Scenario` function creating a test suite:
 * ```
 * @TestRegistering
 * fun <Context : Any> TestSuiteScope.Scenario(
 *     @TestElementName(prefix = "Scenario: ") description: String,
 *     // ...
 * ) {
 *     testSuite("Scenario: $description", testConfig = testConfig) {
 *         // ...
 *     }
 * }
 * ```
 *
 * Note: A test-registering function below the top level, which uses its first parameter for the test element name,
 * is not required to annotate it with `@TestElementName`.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
public annotation class TestElementName(val prefix: String = "", val postfix: String = "")

/**
 * Makes a String parameter receive the simple name of its call-site top-level property.
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
