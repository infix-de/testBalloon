package de.infix.testBalloon.framework.core

import org.junit.rules.TestRule
import org.junit.runners.model.Statement

/**
 * A context for a test-level fixture handling JUnit 4 rules.
 *
 * Each JUnit 4 TestRule registered in this context via the [rule] method will be applied to a test.
 *
 * Usage:
 * ```
 * testFixture {
 *         object : JUnit4RulesContext() {
 *             val composeTestRule = rule(createComposeRule())
 *             // other rules or regular properties...
 *         }
 *     } asContextForEach {
 *         test("setup") {
 *             composeTestRule.setContent {
 *                 ComposableUnderTest()
 *             }
 *     }
 * }
 * ```
 */
@TestBalloonExperimentalApi
public open class JUnit4RulesContext : TestFixture.BlockingEnvelope {
    /** Rules in reverse order of registration (LIFO). */
    private val rules = mutableListOf<TestRule>()

    /**
     * Registers a JUnit4 rule.
     *
     * Rules wrap around the test execution outside-in, with the rule registered last being closest to the test
     * action.
     */
    public fun <Rule : TestRule> rule(rule: Rule): Rule = rule.also { rules.add(0, it) }

    override fun execute(test: Test, elementAction: () -> Unit) {
        if (rules.isEmpty()) return elementAction() // fast path

        val elementActionStatement: Statement = object : Statement() {
            override fun evaluate() {
                elementAction()
            }
        }

        // Create a composite statement by wrapping (apply) each rule statement around the preceding one,
        // then execute (evaluate) the composite statement.
        rules.fold(elementActionStatement) { compositeStatement, rule ->
            rule.apply(compositeStatement, test.jUnit4Description)
        }.evaluate()
    }
}
