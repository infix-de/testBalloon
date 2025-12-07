@file:Suppress("RedundantSuspendModifier")

package com.example

import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

// Use suspend-capable suite-level test fixtures with values shared across tests and (sub-)suites.
// Fixtures lazily initialize on first use and automatically release resources when no longer needed.

val UsingSuiteLevelFixtures by testSuite {
    testSuite("fixture value obtained by invocation") {
        val starRepository = testFixture {
            StarRepository()
        } closeWith {
            disconnect()
        }

        testSuite("actual users") {
            val userRepository = userRepository()

            test("alina") {
                assertEquals(4, starRepository().userStars("alina"))
            }

            test("peter") {
                assertEquals(3, starRepository().userStars("peter"))
            }

            test("stars for all") {
                userRepository().users().collect { user ->
                    assertTrue(starRepository().userStars(user) > 0)
                }
            }
        }

        test("unknown") {
            assertEquals(0, starRepository().userStars("unknown"))
        }
    }

    testSuite("fixture value as context") {
        testFixture {
            StarRepository()
        } closeWith {
            disconnect()
        } asContextForAll {
            testSuite("actual users") {
                val userRepository = userRepository()

                test("alina") {
                    assertEquals(4, userStars("alina"))
                }

                test("peter") {
                    assertEquals(3, userStars("peter"))
                }

                test("stars for all") {
                    userRepository().users().collect { user ->
                        assertTrue(userStars(user) > 0)
                    }
                }
            }

            test("unknown") {
                assertEquals(0, userStars("unknown"))
            }
        }
    }

    testSuite("fixture value as parameter") {
        testFixture {
            StarRepository()
        } closeWith {
            disconnect()
        } asParameterForAll {
            testSuite("actual users") {
                val userRepository = userRepository()

                test("alina") { starRepository ->
                    assertEquals(4, starRepository.userStars("alina"))
                }

                test("peter") { repository ->
                    assertEquals(3, repository.userStars("peter"))
                }

                test("stars for all") { starRepository ->
                    userRepository().users().collect { user ->
                        assertTrue(starRepository.userStars(user) > 0)
                    }
                }
            }

            test("unknown") { starRepository ->
                assertEquals(0, starRepository.userStars("unknown"))
            }
        }
    }
}

private class StarRepository {
    suspend fun userStars(user: String): Int = mapOf("alina" to 4, "peter" to 3)[user] ?: 0

    suspend fun disconnect() {} // Called via closeWith, so it can suspend.
}

// A fixture can be defined outside a particular test suite for re-use.
private fun TestSuite.userRepository() = testFixture { UserRepository(testSuiteScope) }

private class UserRepository(scope: CoroutineScope) : AutoCloseable {
    val clientJob = scope.launch {
        // Could be running infinitely like with
        //     client.webSocket("https://ktor.io/docs/client-websockets.html") { ... }
        delay(3.seconds)
    }

    suspend fun users(): Flow<String> = flowOf("alina", "peter")

    override fun close() { // The standard (non-suspending) close function.
        clientJob.cancel()
    }
}
