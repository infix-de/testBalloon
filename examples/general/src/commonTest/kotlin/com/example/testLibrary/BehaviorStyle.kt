package com.example.testLibrary

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestElement
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.core.parameter
import de.infix.testBalloon.framework.shared.TestElementName
import de.infix.testBalloon.framework.shared.TestRegistering

/**
 * Registers a BDD/Gherkin-style scenario, initiating a step definition (Given, When, And, Then).
 *
 * This function provides an initial context initialized by [context] to subsequent steps in [content].
 * Subsequent steps are a variable number of [StepDefinition.Given], [StepDefinition.When], [StepDefinition.And],
 * and [StepDefinition.Then] steps.
 *
 * To create a scenario for a number of examples, enclose it in [withExamples].
 */
@Suppress("TestFunctionName")
@TestRegistering
fun <Context : Any> TestSuiteScope.Scenario(
    @TestElementName(prefix = "Scenario: ") description: String,
    context: suspend () -> Context,
    testConfig: TestConfig = TestConfig,
    content: StepDefinition<Context>.() -> Unit
) {
    testSuite("Scenario: $description", testConfig = testConfig) {
        StepDefinition(testSuiteInScope, context).apply {
            content()
            register()
        }
    }
}

@Suppress("TestFunctionName")
class StepDefinition<Context : Any>(
    override val testSuiteInScope: TestSuite,
    private val context: suspend () -> Context
) : TestSuiteScope {

    private class Step<Value : Any>(val description: String, val action: suspend Value.() -> Unit)

    private val steps = mutableListOf<Step<Context>>()

    /** Adds a "Given" step. */
    fun Given(description: String, action: suspend Context.() -> Unit) {
        steps.add(Step("Given $description", action))
    }

    /** Adds a "When" step. */
    fun When(description: String, action: suspend Context.() -> Unit) {
        steps.add(Step("When $description", action))
    }

    /** Adds an "And" step. */
    fun And(description: String, action: suspend Context.() -> Unit) {
        steps.add(Step("And $description", action))
    }

    /** Adds a "Then" step. */
    fun Then(thenDescription: String, thenAction: suspend Context.() -> Unit) {
        steps.add(Step("Then $thenDescription", thenAction))
    }

    /** Registers test suites and tests for the step definition. */
    internal fun register() {
        when (testSuiteInScope.behaviorStyle()) {
            BehaviorStyle.Linear -> registerLinearSteps()
            BehaviorStyle.Hierarchical -> registerHierarchicalSteps()
        }
    }

    /** Registers a test per step. */
    private fun registerLinearSteps() {
        testFixture { context() }.asParameterForAll {
            for (step in steps) {
                test(step.description) { context ->
                    step.action(context)
                }
            }
        }
    }

    /**
     * Registers a test suite for all steps except the last one, which will become a test.
     * All step actions execute in that test.
     */
    private fun registerHierarchicalSteps() {
        if (steps.isEmpty()) return

        // We need a copy of our step actions, because the steps will be reset for another step definition cycle
        // before our step actions had a chance to execute.
        val stepActions = steps.map { it.action }

        val registerStepDefinitionTestSuite = steps.dropLast(1).foldRight(
            // The innermost step executes all step actions in a single Test.
            fun TestSuiteScope.() {
                test(steps.last().description) {
                    with(context()) {
                        for (action in stepActions) {
                            action()
                        }
                    }
                }
            }
        ) { step, innerContent ->
            // Each step before `Then` is represented by a test suite. It wraps the test suites of subsequent steps.
            // The wrapping must happen inside out (last step first), that's why we're using `foldRight` here.
            fun TestSuiteScope.() {
                testSuite(step.description, content = innerContent)
            }
        }

        registerStepDefinitionTestSuite()
    }
}

/**
 * Performs [action] on [examples], with each example provided as a `this` context.
 */
fun <Example : Any> withExamples(vararg examples: Example, action: Example.() -> Unit) {
    for (example in examples) {
        example.action()
    }
}

fun TestConfig.behaviorStyle(value: BehaviorStyle) = parameter(BehaviorStyleParameter.Key) {
    BehaviorStyleParameter(value)
}

enum class BehaviorStyle {
    Linear,
    Hierarchical
}

internal class BehaviorStyleParameter(val style: BehaviorStyle) : TestElement.KeyedParameter(Key) {
    companion object Key : TestElement.KeyedParameter.Key<BehaviorStyleParameter>
}

private fun TestElement.behaviorStyle() =
    testElementParameter(BehaviorStyleParameter.Key)?.style ?: BehaviorStyle.Linear
