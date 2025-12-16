package com.example

import com.example.testLibrary.statisticsReport
import de.infix.testBalloon.framework.core.TestBalloonExperimentalApi
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestInvocation
import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.core.invocation
import de.infix.testBalloon.framework.core.singleThreaded
import de.infix.testBalloon.framework.core.testScope
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

// Disabling the coroutines `TestScope` at the top-level suite makes everything run on real time.
val Concurrency by testSuite(testConfig = TestConfig.testScope(isEnabled = false)) {

    // Compare test runs with different concurrency settings.
    // Use our own custom TestConfig.statisticsReport() to report results for each suite.

    testSuite(
        "sequential",
        testConfig = TestConfig.invocation(TestInvocation.SEQUENTIAL).statisticsReport()
    ) {
        testSeries()
    }

    testSuite(
        "concurrent (default)",
        testConfig = TestConfig.invocation(TestInvocation.CONCURRENT).statisticsReport()
    ) {
        testSeries()
    }

    @OptIn(TestBalloonExperimentalApi::class) // required for singleThreaded()
    testSuite(
        "concurrent (single-threaded)",
        testConfig = TestConfig.invocation(TestInvocation.CONCURRENT).singleThreaded().statisticsReport()
    ) {
        testSeries()
    }

    testSuite(
        "concurrent",
        testConfig = TestConfig.invocation(TestInvocation.CONCURRENT).statisticsReport()
    ) {
        for (suiteId in 1..10) {
            testSuite("suite $suiteId") {
                testSeries()
            }
        }
    }
}

// Define your own test series builder.
private fun TestSuiteScope.testSeries() {
    for (testId in 1..10) {
        test("test $testId") {
            delay(10.milliseconds)
        }
    }
}
