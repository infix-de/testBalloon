package com.example

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestElement
import de.infix.testBalloon.framework.core.TestElementEvent
import de.infix.testBalloon.framework.core.TestExecutionReport
import de.infix.testBalloon.framework.core.disable
import de.infix.testBalloon.framework.core.executionReport
import de.infix.testBalloon.framework.core.internal.printlnFixed
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

// Use a test report to print information about disabled tests.

val UsingExecutionReport by testSuite(testConfig = TestConfig.executionReport(DisabledTestsExecutionReport())) {
    test("test1") {
        delay(1.seconds)
    }

    testSuite("innerSuite", testConfig = TestConfig.disable()) {
        test("testA") {
            delay(2.seconds)
        }

        test("testB") {
            delay(3.seconds)
        }
    }
}

private class DisabledTestsExecutionReport : TestExecutionReport() {
    private val rootElement = atomic<TestElement?>(null)
    private val lock = reentrantLock()
    private val disabledTestPaths = mutableListOf<TestElement.Path>() // guarded by lock

    override suspend fun add(event: TestElementEvent) {
        rootElement.compareAndSet(null, event.element)

        if (event !is TestElementEvent.Finished) return

        val element = event.element

        if (!element.testElementIsEnabled && element is Test) {
            lock.withLock { disabledTestPaths.add(element.testElementPath) }
        }

        if (element == rootElement.value && disabledTestPaths.isNotEmpty()) {
            @OptIn(TestBalloonInternalApi::class)
            printlnFixed(
                "WARNING: ${disabledTestPaths.size} disabled test(s) in ${rootElement.value?.testElementPath}:\n\t" +
                    disabledTestPaths.joinToString("\n\t") { "$it" }
            )
        }
    }
}
