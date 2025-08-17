package com.example

import de.infix.testBalloon.framework.Test
import de.infix.testBalloon.framework.TestConfig
import de.infix.testBalloon.framework.TestElement
import de.infix.testBalloon.framework.TestExecutionTraversal
import de.infix.testBalloon.framework.TestInvocation
import de.infix.testBalloon.framework.TestSuite
import de.infix.testBalloon.framework.internal.printlnFixed
import de.infix.testBalloon.framework.invocation
import de.infix.testBalloon.framework.singleThreaded
import de.infix.testBalloon.framework.testPlatform
import de.infix.testBalloon.framework.testScope
import de.infix.testBalloon.framework.testSuite
import de.infix.testBalloon.framework.traversal
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

val ConcurrentInstrumentedTestsWithTestBalloon by testSuite(testConfig = TestConfig.testScope(isEnabled = false)) {

    testSuite(
        "sequential",
        testConfig = TestConfig.invocation(TestInvocation.SEQUENTIAL).multithreadingReport()
    ) {
        testSeries()
    }

    testSuite(
        "concurrent, default",
        testConfig = TestConfig.invocation(TestInvocation.CONCURRENT).multithreadingReport()
    ) {
        testSeries()
    }

    testSuite(
        "concurrent, single-threaded",
        testConfig = TestConfig.invocation(TestInvocation.CONCURRENT).singleThreaded().multithreadingReport()
    ) {
        testSeries()
    }

    testSuite(
        "concurrent, nested",
        testConfig = TestConfig.invocation(TestInvocation.CONCURRENT).multithreadingReport()
    ) {
        for (suiteId in 1..10) {
            testSuite("suite #$suiteId") {
                testSeries()
            }
        }
    }
}

// Define your own test series builder.
private fun TestSuite.testSeries() {
    for (testId in 1..10) {
        test("#$testId") {
            delay(10.milliseconds)
        }
    }
}

private fun TestConfig.multithreadingReport() = traversal(MultithreadingReport())

private class MultithreadingReport : TestExecutionTraversal {
    private val lock = reentrantLock()

    // These may be mutated only while `lock` is held.
    private val threadIdsUsed = mutableSetOf<ULong>()
    private var testCount = 0

    private val isReportRoot = atomic(true)

    override suspend fun aroundEach(testElement: TestElement, elementAction: suspend TestElement.() -> Unit) {
        val isReportRoot = this.isReportRoot.getAndSet(false)

        if (testElement is Test) {
            lock.withLock {
                threadIdsUsed.add(testPlatform.threadId())
                testCount++
            }
        }

        testElement.elementAction()

        if (isReportRoot) {
            printlnFixed(
                "${testElement.testElementPath}[${testPlatform.displayName}]: ran $testCount test(s)" +
                    " on ${threadIdsUsed.size} thread(s)"
            )
        }
    }
}
