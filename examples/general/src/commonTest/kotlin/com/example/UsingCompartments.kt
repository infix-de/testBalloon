package com.example

import com.example.testLibrary.statisticsReport
import com.example.testLibrary.testSeries
import de.infix.testBalloon.framework.core.TestBalloonExperimentalApi
import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.internal.printlnFixed
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.core.testScope
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// Register a test suite capable of running tests concurrently.
// Compartments ensure that this does not interfere with tests requiring the default sequential execution.

val ConcurrentSuite by testSuite(
    compartment = { TestCompartment.Concurrent },
    testConfig = TestConfig
        .testScope(isEnabled = false)
        .statisticsReport()
) {
    testSeries("delay iterations", iterations = 10) {
        delay(10.milliseconds)
    }
}

// Register a test suite for UI tests. This will combine sequential execution with the presence of a Main dispatcher.

@OptIn(TestBalloonExperimentalApi::class) // required for TestCompartment.UI
val UiSuite by testSuite(
    compartment = { TestCompartment.UI() },
    testConfig = TestConfig.statisticsReport()
) {
    test("On UI thread") {
        launch(Dispatchers.Main) {
            delay(10.milliseconds)
            @OptIn(TestBalloonInternalApi::class)
            printlnFixed(testPlatform.threadDisplayName())
        }
    }
}

// Note that we can also make the entire test session run tests concurrently:
//     class MyTestSession : TestSession(defaultCompartment = { TestCompartment.Concurrent })
