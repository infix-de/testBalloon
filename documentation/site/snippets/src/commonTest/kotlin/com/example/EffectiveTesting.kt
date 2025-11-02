package com.example

import de.infix.testBalloon.framework.core.TestBalloonExperimentalApi
import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.aroundEachTest
import de.infix.testBalloon.framework.core.disable
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.TestRegistering
import kotlinx.coroutines.delay
import kotlin.math.round
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

val EffectiveTesting by testSuite {
    testSuite("Use expressive test names") {
        val user = "paul"
// --8<-- [start:expressive-test-names]
        test("User '$user' must be signed in") {
            // ...
        }
// --8<-- [end:expressive-test-names]
    }

// --8<-- [start:multiple-tests-with-fresh-state]
    testSuite("Multiple tests with fresh state") {
        @TestRegistering // (2)!
        fun test(name: String, action: suspend Service.() -> Unit) = // (3)!
            this.test(name) {
// --8<-- [start:multiple-tests-with-fresh-state-init]
                val service = Service().apply {
                    signIn(userName = "siobhan", password = "ask") // (1)!
                }
// --8<-- [end:multiple-tests-with-fresh-state-init]
                service.action() // (4)!
                // cleanup...
            }

        test("deposit") {
            deposit(Amount(20.0)) // (5)!
            assertEquals(Amount(40.99), accountBalance()) // (6)!
        }

        test("withdraw") {
            withdraw(Amount(20.0))
            assertEquals(Amount(0.99), accountBalance())
        }
    }
// --8<-- [end:multiple-tests-with-fresh-state]

// --8<-- [start:multiple-tests-sharing-state]
    testSuite("Multiple tests sharing state") {
        class Context { // (1)!
            val service = Service().apply {
                signIn(userName = "siobhan", password = "ask")
            }
            var expectedTransactionCount = 0 // (2)!
        }

        val context = testFixture { Context() }

        @TestRegistering // (3)!
        fun test(name: String, action: suspend Context.() -> Unit) = // (4)!
            this.test(name) { context().action() } // (5)!

        test("deposit") {
            service.deposit(Amount(20.0))
            assertEquals(Amount(40.99), service.accountBalance())
            assertEquals(++expectedTransactionCount, service.transactionCount())
        }

        test("withdraw") {
            service.withdraw(Amount(20.0))
            assertEquals(Amount(20.99), service.accountBalance())
            assertEquals(++expectedTransactionCount, service.transactionCount())
        }
    }
// --8<-- [end:multiple-tests-sharing-state]

    // TODO: Replace with the real thing
    fun <Value : Any> TestSuite.testScopedFixture(
        value: suspend TestSuite.() -> Value
    ): TestSuite.Fixture<Value> = testFixture(value)

    testSuite("Multiple tests with fresh state") {
        val service = testScopedFixture {
            Service().apply {
                signIn(userName = "siobhan", password = "guesswork")
            }
        }

        test("deposit") {
            service().deposit(Amount(20.0))
            assertEquals(Amount(40.99), service().accountBalance())
        }

        test("withdraw") {
            service().withdraw(Amount(20.0))
            delay(10.seconds)
            assertEquals(Amount(0.99), service().accountBalance())
        }
    }
}

private class Service {
    private var signedIn = false
    private var accountBalance = Amount(0.0)
    private var transactionCount = 0

    @Suppress("unused")
    fun signIn(userName: String, password: String) {
        check(!signedIn)
        signedIn = true
        accountBalance = Amount(20.99)
        transactionCount = 0
    }

    fun accountBalance(): Amount {
        check(signedIn)
        return accountBalance
    }

    fun deposit(amount: Amount) {
        check(signedIn)
        accountBalance += amount
        transactionCount++
    }

    fun withdraw(amount: Amount) {
        check(signedIn)
        require(accountBalance >= amount)
        accountBalance -= amount
        transactionCount++
    }

    fun transactionCount(): Int {
        check(signedIn)
        return transactionCount
    }

    fun signOut() {
        check(signedIn)
        signedIn = false
    }
}

private class Amount(value: Double = 0.0) {
    private val value: Double = round(value * 100) / 100

    operator fun plus(other: Amount) = Amount(value + other.value)
    operator fun minus(other: Amount) = Amount(value - other.value)
    operator fun compareTo(other: Amount) = value.compareTo(other.value)

    override fun toString(): String = "$value"

    override fun equals(other: Any?): Boolean =
        value == (other as? Amount)?.value

    override fun hashCode(): Int = value.hashCode()
}

private fun repeatableRandomAmounts(count: Int): List<Amount> {
    val randomSource = Random(147)
    val maximumValue = 10340.50
    return List(count) { Amount(randomSource.nextDouble() * maximumValue) }
}

val ServiceTest by testSuite {
    val service = Service()

    test("sign in succeeds") {
        service.signIn("mona", "some-token")
    }

    testSuite("user signed in") {
        test("account balance is 20.99") {
            assertEquals(Amount(20.99), service.accountBalance())
        }
        test("transaction count is 0") {
            assertEquals(0, service.transactionCount())
        }
        test("sign out succeeds") {
            service.signOut()
        }
    }

    testSuite("user signed out") {
        test("account balance access fails") {
            assertFailsWith<IllegalStateException> { service.accountBalance() }
        }
        test("transaction count access fails") {
            assertFailsWith<IllegalStateException> {
                service.transactionCount()
            }
        }
        test("sign out fails") {
            assertFailsWith<IllegalStateException> { service.signOut() }
        }
    }
}

val DepositTestDoneWrong by testSuite {
    val service = Service()

    test("sign in succeeds") {
        service.signIn("mona", "some-token")
    }

    testSuite("account balance") {
        var expectedBalance = Amount(20.99)

        test("starts at $expectedBalance") {
            assertEquals(expectedBalance, service.accountBalance())
        }

        repeatableRandomAmounts(10).forEach { amount ->
            expectedBalance += amount

            test("deposit $amount, expect balance $expectedBalance") {
                service.deposit(amount)
                assertEquals(expectedBalance, service.accountBalance())
            }
        }
    }
}

val DepositTestDoneBetter by testSuite {
    val service = Service()

    test("sign in succeeds") {
        service.signIn("mona", "some-token")
    }

    var registeredBalance = Amount(20.99)

    testSuite("account balance") {
        val expectedBalance = registeredBalance
        test("starts at $expectedBalance") {
            assertEquals(expectedBalance, service.accountBalance())
        }

        repeatableRandomAmounts(10).forEach { amount ->
            registeredBalance += amount
            val expectedBalance = registeredBalance

            test("deposit $amount, expect balance $expectedBalance") {
                service.deposit(amount)
                assertEquals(expectedBalance, service.accountBalance())
            }
        }
    }
}

val DepositTestDoneBest by testSuite {
    val service = Service()

    test("sign in succeeds") {
        service.signIn("mona", "some-token")
    }

    val initialBalance = Amount(20.99)
    var runningBalance = initialBalance
    val amountsAndBalances = repeatableRandomAmounts(10).map { amount ->
        runningBalance += amount
        amount to runningBalance
    }

    testSuite("account balance") {
        test("starts at $initialBalance") {
            assertEquals(initialBalance, service.accountBalance())
        }

        amountsAndBalances.forEach { (amount, expectedBalance) ->
            test("deposit $amount, expect balance $expectedBalance") {
                service.deposit(amount)
                assertEquals(expectedBalance, service.accountBalance())
            }
        }
    }
}

// --8<-- [start:test-suite-with-sequential-compartment]
val TestsSharingMutableState by testSuite(
    compartment = { TestCompartment.Sequential } // (1)!
) {
    // ...
}
// --8<-- [end:test-suite-with-sequential-compartment]

// --8<-- [start:test-suite-with-concurrent-compartment]
val ConcurrentTests by testSuite(
    compartment = { TestCompartment.Concurrent } // (1)!
) {
    // ...
}
// --8<-- [end:test-suite-with-concurrent-compartment]

// --8<-- [start:repeatOnFailure]
fun TestConfig.repeatOnFailure(maxRepetitions: Int) = aroundEachTest { action ->
    var lastException: Throwable? = null
    repeat(maxRepetitions) {
        try {
            action()
            return@aroundEachTest
        } catch (exception: Throwable) {
            lastException = exception
            // suppress as long as we try repeatedly
        }
    }
    throw lastException!!
}
// --8<-- [end:repeatOnFailure]

// --8<-- [start:FlakyTests]
val FlakyTests by testSuite {
    testSuite("not controlled") {
        test("would succeed after 3 failures") {
            doSomethingFlaky()
        }
    }

    testSuite("under control", testConfig = TestConfig.repeatOnFailure(5)) {
        test("succeeds after 3 failures") {
            doSomethingFlaky()
        }

        test("always fails") {
            throw Error("always failing")
        }
    }
}
// --8<-- [end:FlakyTests]

private var doSomethingFlakyInvocationCount = 0
private fun doSomethingFlaky() {
    if (doSomethingFlakyInvocationCount++ < 3) {
        throw Error("failing because flaky")
    }
    doSomethingFlakyInvocationCount = 0 // ready for next time
}

// --8<-- [start:my-tags]
enum class MyTag {
    CI,
    SimulatedCI,
    Release;

    @OptIn(TestBalloonExperimentalApi::class) // required for testPlatform
    fun value() =
        testPlatform.environment("TEST_TAGS")?.split(',')?.last { it == name }

    fun exists() = value() != null
}

fun TestConfig.onlyIfTagged(vararg tags: MyTag) =
    if (tags.any { it.exists() }) this else disable()
// --8<-- [end:my-tags]

// --8<-- [start:tag-based-tests]
val ConditionalTests by testSuite(
    testConfig = TestConfig.onlyIfTagged(MyTag.CI, MyTag.SimulatedCI)
) {
    // ...
}
// --8<-- [end:tag-based-tests]
