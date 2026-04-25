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
 * The IDE plugin uses this annotation to detect the function parameter containing the element name.
 * The optional [prefix] and [postfix] annotation parameters make the IDE plugin aware of modifications to the element
 * name, so that a valid test element path can be produced by static analysis. Example for a Behavior/Gherkin-style
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
 * Designates the String parameter of a top-level test suite function which receives the FQN of the property.
 *
 * A top-level [TestRegistering] test suite function must have exactly one `String?` parameter annotated with
 * `@TestElementPropertyFqn` with a default value of `""`. At the call site of such function, the compiler plugin will
 * insert the fully qualified name of the corresponding top-level property.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
public annotation class TestElementPropertyFqn

/**
 * Makes the annotated class part of the test element DSL. See the Kotlin docs on [DslMarker] for details.
 */
@DslMarker
public annotation class TestElementDsl
