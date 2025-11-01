@file:Suppress("ktlint:standard:function-literal")

package com.example

import de.infix.testBalloon.framework.core.TestExecutionScope
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds

// --8<-- [start:ExampleTests]
val ExampleTests by testSuite { // (1)!
    test("string length") { // (2)!
        assertEquals(8, "Test me!".length) // (3)!
    }

    testSuite("integer operations") { // (4)!
        test("max") {
            assertEquals(5, max(5, 3))
        }

        test("min") {
            delay(10.milliseconds) // (5)!
            assertEquals(3, min(5, 3))
        }
    }
}
// --8<-- [end:ExampleTests]

// --8<-- [start:TestSuite-test]
fun TestSuite.test(
    name: String,
    iterations: Int,
    action: suspend TestExecutionScope.() -> Unit
) = test(name) {
    for (iteration in 1..iterations) {
        action()
    }
}
// --8<-- [end:TestSuite-test]

// --8<-- [start:ParameterizedTests]
val ParameterizedTests by testSuite {
    val testCases = mapOf(
        "one" to 3,
        "two" to 3,
        "three" to 5
    )

    for ((string, expectedLength) in testCases) {
        test("length of '$string' is $expectedLength") {
            assertEquals(expectedLength, string.length)
        }
    }
}
// --8<-- [end:ParameterizedTests]

// --8<-- [start:UserTest]
val UserTest by testSuite {
    for (invalidUserName in listOf("", "a", "+", "+foo")) {
        testSuite("User name '$invalidUserName'") {
            for (role in User.Role.entries) {
                test("is invalid with role '$role'") {
                    assertFailsWith<IllegalArgumentException> {
                        User(invalidUserName, role)
                    }
                }
            }
        }
    }
}
// --8<-- [end:UserTest]

private class User(val name: String, val role: Role) {
    enum class Role {
        FINANCIAL,
        LOGISTICS,
        IT
    }

    init {
        require(name.isEmpty())
        require(name.length >= 3)
        require(name.all { it.isLetter() })
    }
}

val TransactionServiceTests by testSuite {
    class TransactionService {
        fun updateTransactionCount(count: Int) {}
    }

    val service = TransactionService()

// --8<-- [start:TransactionServiceTests-accepted-counts]
    testSuite("Accepted counts") {
        val samples = buildList {
            addAll(listOf(0, 1, Int.MAX_VALUE)) // (1)!
            val randomSampleSource = Random(42) // (2)!
            repeat(20) { add(randomSampleSource.nextInt(0, Int.MAX_VALUE)) }
        }

        for (count in samples) {
            test("count $count is accepted") {
                service.updateTransactionCount(count) // should not throw
            }
        }
    }
// --8<-- [end:TransactionServiceTests-accepted-counts]
}

val CalculatorTests by testSuite {
// --8<-- [start:CalculatorTests-Broken]
    testSuite("Broken calculator test suite") {
        val calculator = Calculator()
        val operands = listOf(7, 23, 15)
        var sum = 0 // (1)

        for (operand in operands) {
            sum += operand // (2)
            test("add $operand, expect $sum") {
                calculator.add(operand)
                assertEquals(sum, calculator.result) // (3)
            }
        }
    }
// --8<-- [end:CalculatorTests-Broken]

// --8<-- [start:CalculatorTests-Healthy]
    testSuite("Healthy calculator test suite") {
        val calculator = Calculator()
        val operandsAndSums = buildList { // (1)
            val operands = listOf(7, 23, 15)
            var sum = 0
            for (operand in operands) {
                sum += operand
                add(operand to sum)
            }
        }

        for ((operand, sum) in operandsAndSums) {
            test("add $operand, expect $sum") {
                calculator.add(operand) // (2)
                assertEquals(sum, calculator.result) // (3)
            }
        }
    }
// --8<-- [end:CalculatorTests-Healthy]
}

private class Calculator {
    var result: Int = 0
    fun add(operand: Int) {
        result += operand
    }
}
