@file:Suppress(
    "unused",
    "RedundantSuspendModifier",
    "ktlint:standard:function-literal"
)

package com.example

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals

val TestLevelFixtures by testSuite {
// --8<-- [start:test-level-class]
    class Account(var balance: Double) {
        fun add(amount: Double) {
            balance += amount
        }
    }
// --8<-- [end:test-level-class]

    testSuite("fixture value as a test context") {
// --8<-- [start:test-level-asContextForEach]
        testFixture {
            Account(balance = 42.0)
        } asContextForEach {
            test("add 11.0") {
                add(11.0)
                assertEquals(53.0, balance)
            }
            test("add -11.0") {
                add(-11.0)
                assertEquals(31.0, balance)
            }
        }
// --8<-- [end:test-level-asContextForEach]
    }

    testSuite("fixture value as a test parameter") {
// --8<-- [start:test-level-asParameterForEach]
        testFixture {
            Account(balance = 42.0)
        } asParameterForEach {
            test("add 10.0") { account ->
                account.add(10.0)
                assertEquals(52.0, account.balance)
            }
            test("add -10.0") { account ->
                account.add(-10.0)
                assertEquals(32.0, account.balance)
            }
        }
// --8<-- [end:test-level-asParameterForEach]
    }
}

val SuiteLevelFixtures by testSuite {
// --8<-- [start:suite-level-class]
    class StarRepository {
        suspend fun userStars(user: String): Int = 0
        suspend fun disconnect() {}
    }
// --8<-- [end:suite-level-class]

// --8<-- [start:suite-level-asContextForAll]
    testSuite("fixture value as a test context") {
        val starRepository = testFixture {
            StarRepository()
        } closeWith {
            disconnect()
        } asContextForAll {
            test("alina") {
                assertEquals(4, userStars("alina"))
            }
            test("peter") {
                assertEquals(3, userStars("peter"))
            }
        }
    }
// --8<-- [end:suite-level-asContextForAll]

// --8<-- [start:suite-level-asParameterForAll]
    testSuite("fixture value as a test parameter") {
        val starRepository = testFixture { // (1)!
            StarRepository() // (2)!
        } closeWith {
            disconnect() // (3)!
        } asParameterForAll {
            test("alina") { starRepository ->
                assertEquals(4, starRepository.userStars("alina")) // (4)!
            }
            test("peter") { starRepository ->
                assertEquals(3, starRepository.userStars("peter")) // (5)!
            }
        }
    } // (6)!
// --8<-- [end:suite-level-asParameterForAll]

// --8<-- [start:suite-level-invoke]
    testSuite("fixture value via invoke()") {
        val starRepository = testFixture {
            StarRepository()
        } closeWith {
            disconnect()
        }

        test("alina") {
            assertEquals(4, starRepository().userStars("alina"))
        }
        test("peter") {
            assertEquals(3, starRepository().userStars("peter"))
        }
    }
// --8<-- [end:suite-level-invoke]
}
