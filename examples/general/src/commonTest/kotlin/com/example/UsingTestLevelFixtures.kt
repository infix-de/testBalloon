package com.example

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.invocation
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals

// Use suspend-capable test-level fixtures with values instantiated per test.

val UsingTestLevelFixtures by testSuite(testConfig = TestConfig.invocation(TestConfig.Invocation.Concurrent)) {
    testSuite("fixture value as a test context") {
        testFixture {
            object {
                var balance = 42.0
                fun add(value: Double) {
                    balance += value
                }
            }
        } asContextForEach {
            test("add 11.0") {
                add(11.0)
                assertEquals(53.0, balance)
            }
            test("add -11.0") {
                add(-11.0)
                assertEquals(31.0, balance)
            }
            testSuite("sub-suite") {
                test("add 12.0") {
                    add(12.0)
                    assertEquals(54.0, balance)
                }
                testFixture {
                    object {
                        var balance = 27.0
                        fun add(value: Double) {
                            balance += value
                        }
                    }
                } asContextForEach {
                    test("add -11.0") {
                        add(-11.0)
                        assertEquals(16.0, balance)
                    }
                }
            }
        }
    }

    testSuite("fixture value as a test parameter") {
        testFixture {
            Account().apply { setBalance(42.0) }
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
    }
}

private class Account {
    var balance = 0.0
        private set

    fun setBalance(value: Double) {
        balance = value
    }

    fun add(amount: Double) {
        balance += amount
    }
}
