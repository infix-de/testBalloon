package com.example.testLibrary

import de.infix.testBalloon.framework.Test
import de.infix.testBalloon.framework.TestConfig
import de.infix.testBalloon.framework.TestElement
import de.infix.testBalloon.framework.TestExecutionTraversal
import de.infix.testBalloon.framework.internal.printlnFixed
import de.infix.testBalloon.framework.testPlatform
import de.infix.testBalloon.framework.traversal
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlin.time.measureTime

/**
 * Returns a test configuration chaining [this] with a statistics report.
 *
 * The statistics cover the test element tree rooted at the configuration's element.
 */
fun TestConfig.statisticsReport() = traversal(StatisticsReport())

private class StatisticsReport : TestExecutionTraversal {
    private val reportStart = atomic<TimeSource.Monotonic.ValueTimeMark?>(null)
    private val lock = reentrantLock()

    // These may be mutated only while `lock` is held.
    private var testCount = 0
    private var testFailureCount = 0
    private var cumulativeTestDuration = 0.seconds
    private var slowestTestDuration: Duration = Duration.ZERO
    private var slowestTestPath = "(none)"
    private val threadIdsUsed = mutableSetOf<ULong>()

    override suspend fun aroundEach(testElement: TestElement, elementAction: suspend TestElement.() -> Unit) {
        val isReportRootElement = reportStart.compareAndSet(null, TimeSource.Monotonic.markNow())

        if (testElement is Test) {
            var testResult: Throwable? = null
            measureTime {
                try {
                    testElement.elementAction()
                } catch (throwable: Throwable) {
                    testResult = throwable
                }
            }.also { duration ->
                lock.withLock {
                    testCount++
                    if (testResult != null) testFailureCount++

                    cumulativeTestDuration += duration
                    if (duration > slowestTestDuration) {
                        slowestTestPath = testElement.testElementPath
                        slowestTestDuration = duration
                    }

                    threadIdsUsed.add(testPlatform.threadId())
                }
            }
            if (testResult != null) throw testResult
        } else {
            testElement.elementAction()
        }

        if (isReportRootElement) {
            val elapsedTime = reportStart.value!!.elapsedNow()
            printlnFixed(
                "${testElement.testElementPath}[${testPlatform.displayName}]: ran $testCount test(s)" +
                    " on ${threadIdsUsed.size} thread(s) in $elapsedTime," +
                    " cumulative test duration: $cumulativeTestDuration"
            )
            if (slowestTestDuration != Duration.ZERO) {
                printlnFixed(
                    "\tThe slowest test '$slowestTestPath' took $slowestTestDuration."
                )
            }
        }
    }
}
