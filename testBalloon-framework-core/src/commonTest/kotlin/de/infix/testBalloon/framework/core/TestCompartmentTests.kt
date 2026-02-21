package de.infix.testBalloon.framework.core

import de.infix.testBalloon.framework.core.internal.FrameworkTestUtilities
import de.infix.testBalloon.framework.core.internal.assertElementPathsContainInOrder
import de.infix.testBalloon.framework.core.internal.assertMessageStartsWith
import de.infix.testBalloon.framework.shared.internal.Constants
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TestCompartmentTests {
    private val iSep = Constants.INTERNAL_PATH_ELEMENT_SEPARATOR

    @Test
    fun defaultCompartment() = FrameworkTestUtilities.assertSuccessfulSuite {
        val compartment = testElementParent as? TestCompartment
        test("test1") {
            assertEquals(TestCompartment.Default, compartment)
        }
        test("test2") {
            assertEquals(TestCompartment.Default, compartment)
        }
    }

    @Test
    fun twoCompartments() = FrameworkTestUtilities.withTestFramework {
        val suite1 by testSuite("suite1") {
            val compartment = testElementParent as? TestCompartment
            test("test1") {
                assertEquals(TestCompartment.Default, compartment)
                assertNull(currentCoroutineContext()[CoroutineName.Key])
            }
            test("test2") {
                assertEquals(TestCompartment.Default, compartment)
                assertNull(currentCoroutineContext()[CoroutineName.Key])
            }
        }

        val coroutineName2 = CoroutineName("#2")
        val compartment2 =
            TestCompartment("Compartment 2", testConfig = TestConfig.coroutineContext(coroutineName2))
        val suite2 by testSuite("suite2", compartment = { compartment2 }) {
            val compartment = testElementParent as? TestCompartment
            test("test1") {
                assertEquals(compartment2, compartment)
                assertEquals(coroutineName2, currentCoroutineContext()[CoroutineName.Key])
            }
            test("test2") {
                assertEquals(compartment2, compartment)
                assertEquals(coroutineName2, currentCoroutineContext()[CoroutineName.Key])
            }
        }

        val suite3 by testSuite("suite3") {
            val compartment = testElementParent as? TestCompartment
            test("test1") {
                assertEquals(TestCompartment.Default, compartment)
                assertNull(currentCoroutineContext()[CoroutineName.Key])
            }
            test("test2") {
                assertEquals(TestCompartment.Default, compartment)
                assertNull(currentCoroutineContext()[CoroutineName.Key])
            }
        }

        val suite4 by testSuite("suite4", compartment = { compartment2 }) {
            val compartment = testElementParent as? TestCompartment
            test("test1") {
                assertEquals(compartment, compartment2)
                assertEquals(coroutineName2, currentCoroutineContext()[CoroutineName.Key])
            }
            test("test2") {
                assertEquals(compartment, compartment2)
                assertEquals(coroutineName2, currentCoroutineContext()[CoroutineName.Key])
            }
        }

        FrameworkTestUtilities.withTestReport(suite1, suite2, suite3, suite4) {
            with(finishedTestEvents()) {
                assertTrue(isNotEmpty())
                assertAllSucceeded()

                // Tests in each compartment must be processed consecutively.
                assertElementPathsContainInOrder(
                    map {
                        it.element.testElementPath.internalId
                    }.filter { it.startsWith("suite1$iSep") || it.startsWith("suite3$iSep") }
                )
                assertElementPathsContainInOrder(
                    map {
                        it.element.testElementPath.internalId
                    }.filter { it.startsWith("suite2$iSep") || it.startsWith("suite4$iSep") }
                )
            }
        }
    }

    @Test
    fun concurrency() = FrameworkTestUtilities.withTestFramework {
        val suiteCount = 8
        val testCount = 8

        val concurrentThreadIds = ConcurrentSet<ULong>()

        val suite1 by testSuite("topSuite1", compartment = { TestCompartment.Concurrent }) {
            val outerSuiteThreadId = testPlatform.threadId()

            for (suiteNumber in 1..suiteCount) {
                testSuite("subSuite$suiteNumber") {
                    assertEquals(outerSuiteThreadId, testPlatform.threadId())

                    for (testNumber in 1..testCount) {
                        test("test$testNumber") {
                            concurrentThreadIds.add(testPlatform.threadId())
                            delay(10.milliseconds)
                        }
                    }
                }
            }
        }

        val sequentialThreadIds = ConcurrentSet<ULong>()

        val suite2 by testSuite("topSuite2") {
            for (suiteNumber in 1..suiteCount) {
                testSuite("subSuite$suiteNumber") {
                    for (testNumber in 1..testCount) {
                        test("test$testNumber") {
                            sequentialThreadIds.add(testPlatform.threadId())
                            delay(10.milliseconds)
                        }
                    }
                }
            }
        }

        class MissedParallelismExpectation(actualThreadCount: Int) :
            Throwable("Expected a concurrent thread count > 1 but was $actualThreadCount")

        var missedParallelismExpectation: MissedParallelismExpectation? = null

        // This test has been flaky on GitHub macOS and Windows runners. Tests occasionally run on one thread only,
        // on systems where multiple cores are present. We try repeatedly before giving up.
        for (attempt in 1..10) {
            concurrentThreadIds.clear()
            sequentialThreadIds.clear()

            try {
                FrameworkTestUtilities.withTestReport(
                    suite1,
                    suite2,
                    invokeSetup = missedParallelismExpectation == null
                ) {
                    with(finishedTestEvents()) {
                        assertTrue(isNotEmpty())
                        assertAllSucceeded()

                        val concurrentThreadCount = concurrentThreadIds.elements().size
                        if (testPlatform.parallelism > 1) {
                            if (concurrentThreadCount <= 1) {
                                throw MissedParallelismExpectation(concurrentThreadCount)
                            }
                        } else {
                            assertEquals(
                                1,
                                concurrentThreadCount,
                                "Expected concurrent execution on 1 thread, actually: $concurrentThreadCount threads"
                            )
                        }

                        val sequentialThreadCount = sequentialThreadIds.elements().size
                        assertEquals(
                            1,
                            sequentialThreadCount,
                            "Expected sequential execution on 1 thread, actually: $sequentialThreadCount threads"
                        )

                        // Tests in each compartment must be processed consecutively.
                        assertElementPathsContainInOrder(
                            map {
                                it.element.testElementPath.internalId
                            }.filter { it.startsWith("topSuite1$iSep") }
                        )
                        assertElementPathsContainInOrder(
                            map {
                                it.element.testElementPath.internalId
                            }.filter { it.startsWith("topSuite2$iSep") }
                        )
                    }
                }
                break
            } catch (exception: MissedParallelismExpectation) {
                missedParallelismExpectation = exception
                delay(1.seconds)
                println(">>> Repeating concurrency test after $attempt failed attempt(s)")
            }
        }

        if (missedParallelismExpectation != null) {
            // Test runners on GitHub can be really stubborn, insisting on a single thread. We ignore them.
            if (testPlatform.environment("CI") != null) {
                println(">>> [CI] ignoring $missedParallelismExpectation")
            } else {
                fail(missedParallelismExpectation.message)
            }
        }
    }

    @Test
    fun ui() = FrameworkTestUtilities.withTestFramework {
        val uiThreadIds = ConcurrentSet<ULong>()

        val suite by testSuite("topSuite", compartment = { TestCompartment.MainDispatcher(Dispatchers.Unconfined) }) {
            for (suiteNumber in 1..3) {
                testSuite("subSuite$suiteNumber") {
                    test("test1") {
                        uiThreadIds.add(testPlatform.threadId())
                        delay(1.milliseconds)
                        assertFails {
                            withMainDispatcher(Dispatchers.Default) {}
                        }.assertMessageStartsWith("Another invocation of withMainDispatcher() is still active.")
                    }

                    for (testNumber in 2..4) {
                        test("test$testNumber") {
                            uiThreadIds.add(testPlatform.threadId())
                            delay(1.milliseconds)
                        }
                    }
                }
            }
        }

        FrameworkTestUtilities.withTestReport(suite) {
            with(finishedTestEvents()) {
                assertTrue(isNotEmpty(), "Missing finished test events.")
                assertAllSucceeded()
                val threadCount = uiThreadIds.elements().size
                assertEquals(1, threadCount, "Expected 1 thread, but got $threadCount.")
            }
        }
    }
}

private class ConcurrentSet<Element> : SynchronizedObject() {
    private val elements = mutableSetOf<Element>()

    fun clear() = synchronized(this) { elements.clear() }
    fun add(element: Element) = synchronized(this) { elements.add(element) }
    fun elements() = synchronized(this) { elements.toSet() }
}
