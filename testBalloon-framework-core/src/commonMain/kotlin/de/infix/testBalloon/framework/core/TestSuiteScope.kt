package de.infix.testBalloon.framework.core

import de.infix.testBalloon.framework.shared.TestDisplayName
import de.infix.testBalloon.framework.shared.TestElementName
import de.infix.testBalloon.framework.shared.TestRegistering

/**
 * A [TestSuite]-based registration scope for tests, test suites and fixtures.
 *
 * The most direct [TestSuiteScope] is the [TestSuite] itself. In addition, there are scopes for fixtures and
 * custom scopes. These are not test suites in their own right, but delegate to one.
 */
public interface TestSuiteScope {
    /**
     * The closest test suite in scope. Use this to refer to the test suite when creating custom tests or test suites.
     */
    public val testSuiteInScope: TestSuite

    /**
     * Registers a [TestSuite] as a child of the [testSuiteInScope].
     */
    // Note: The extra TestSuiteScope extension receiver serves to prioritize overloads from derivative scopes.
    @TestRegistering
    public fun TestSuiteScope.testSuite(
        @TestElementName name: String,
        @TestDisplayName displayName: String = name,
        testConfig: TestConfig = TestConfig,
        content: TestSuite.() -> Unit
    ) {
        TestSuite(testSuiteInScope, name = name, displayName = displayName, testConfig = testConfig, content = content)
    }

    /**
     * Registers a [Test] as a child of the [testSuiteInScope].
     */
    // Note: The extra TestSuiteScope extension receiver serves to prioritize overloads from derivative scopes.
    @TestRegistering
    public fun TestSuiteScope.test(
        @TestElementName name: String,
        @TestDisplayName displayName: String = name,
        testConfig: TestConfig = TestConfig,
        action: suspend Test.ExecutionScope.() -> Unit
    ) {
        Test(testSuiteInScope, name = name, displayName = displayName, testConfig = testConfig, action)
    }

    /**
     * Registers a fixture, a state holder for a lazily initialized [Value] to be used in tests.
     *
     * For details, see [TestFixture].
     */
    public fun <Value : Any> testFixture(value: suspend TestSuite.() -> Value): TestFixture<Value> =
        TestFixture(testSuiteInScope, value)
}
