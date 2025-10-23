package com.example

import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.TestDiscoverable
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.test.assertEquals

// Multiple tests using the same set of properties.
val ContextualTests by testSuite {
    // Use seeded pseudo-random values for repeatable tests.
    val seededRandomValueSource = Random(13)

    // Option #1 – Recommended
    // Generate 3 test rounds of tests sharing a common context per round.
    // The context is created at execution time, and only if needed (tests may not run due to being disabled or
    // filtered). Use this in most cases, in particular for resource-intensive contexts, and with contexts requiring
    // suspend function invocations.
    for (round in 1..3) {
        // Defining a suite per round is optional.
        testSuite("common context, round $round") {
            class Context {
                val fullPhrase = "This is a very long text"
                val desiredLength = seededRandomValueSource.nextInt(0..15)
                val abbreviatedPhrase = fullPhrase.take(desiredLength)
            }

            val commonContext = testFixture { Context() }

            @TestDiscoverable
            fun testWithContext(name: String, action: suspend Context.() -> Unit) = test(name) {
                commonContext().action()
            }

            testWithContext("abbreviate with substring") {
                assertEquals(abbreviatedPhrase, fullPhrase.substring(0..<desiredLength))
            }

            testWithContext("abbreviate with dropLast") {
                assertEquals(abbreviatedPhrase, fullPhrase.dropLast(fullPhrase.length - desiredLength))
            }
        }
    }

    // Option #2 – Recommended
    // Generate 3 test rounds of tests sharing a common context per round. The context's properties are created at
    // definition time. Use only for lightweight contexts not requiring suspend function invocations, and in cases
    // where test names should display context properties.
    for (round in 1..3) {
        // A set of properties shared by subsequent tests.
        val fullPhrase = "This is a very long text which we intend to abbreviate."
        val desiredLength = seededRandomValueSource.nextInt(0..15)
        val expectedResult = fullPhrase.take(desiredLength)

        // Defining a suite per round is optional.
        testSuite("common context, round $round with length $desiredLength -> '$expectedResult'") {
            test("abbreviate with substring") {
                assertEquals(expectedResult, fullPhrase.substring(0..<desiredLength))
            }

            test("abbreviate with dropLast") {
                assertEquals(expectedResult, fullPhrase.dropLast(fullPhrase.length - desiredLength))
            }
        }
    }

    // Option #2 – Not recommended
    // Generate 3 test rounds of tests, with each test having an individual context.
    // Why is this is not recommended? Because a random context should always be checked by all tests. Otherwise,
    // some context A could succeed on test 1, but might fail on test 2. If test 2 gets a different context, this would
    // not be detected.
    for (round in 1..3) {
        // Defining a suite per round is optional.
        testSuite("individual contexts, round $round") {
            class Context {
                val fullPhrase = "This is a very long text"
                val desiredLength = seededRandomValueSource.nextInt(0..15)
                val abbreviatedPhrase = fullPhrase.take(desiredLength)
            }

            @TestDiscoverable
            fun testWithContext(name: String, action: suspend Context.() -> Unit) = test(name) {
                Context().action()
            }

            testWithContext("abbreviate with substring") {
                assertEquals(abbreviatedPhrase, fullPhrase.substring(0..<desiredLength))
            }

            testWithContext("abbreviate with dropLast") {
                assertEquals(abbreviatedPhrase, fullPhrase.dropLast(fullPhrase.length - desiredLength))
            }
        }
    }
}
