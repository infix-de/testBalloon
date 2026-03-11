package com.example

import com.example.testLibrary.BehaviorStyle
import com.example.testLibrary.Scenario
import com.example.testLibrary.behaviorStyle
import com.example.testLibrary.withExamples
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.delay
import kotlin.getValue
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

val UsingBehaviorStyle by testSuite {
    Scenario("Too much food syndrome", context = {
        Belly()
    }, testConfig = TestConfig.behaviorStyle(BehaviorStyle.Hierarchical)) {
        Given("I eat 48 cucumbers") {
            eatCucumbers(48)
        }

        When("I wait for an hour") {
            delay(1.hours)
        }

        Then("my belly growls") {
            assertTrue(growls())
        }
    }

    data class Example(val name: String, val initial: Int, val addend: Int, val factor: Int?, val expected: Int)

    withExamples(
        Example("+/+/*", initial = 1, addend = 3, factor = 10, expected = 40),
        Example("-/+/*", initial = -3, addend = 5, factor = 10, expected = 20),
        Example("+/-", initial = 10, addend = -2, factor = null, expected = 8)
    ) {
        Scenario(
            "Calculate $name",
            context = { Calculator() },
            testConfig = TestConfig.behaviorStyle(BehaviorStyle.Hierarchical)
        ) {
            Given("I enter $initial") {
                set(initial)
            }

            When("I add $addend") {
                add(addend)
            }

            if (factor != null) {
                And("I multiply by $factor") {
                    multiply(factor)
                }
            }

            Then("the result should be $expected") {
                assertEquals(expected, result)
            }
        }
    }
}

private class Belly(var cucumberCount: Int = 0) {
    fun eatCucumbers(count: Int) {
        cucumberCount += count
    }

    fun growls() = cucumberCount > 2
}

private class Calculator {
    var result: Int = 0

    fun set(operand: Int) {
        result = operand
    }

    fun add(operand: Int) {
        result += operand
    }

    fun multiply(operand: Int) {
        result *= operand
    }
}
