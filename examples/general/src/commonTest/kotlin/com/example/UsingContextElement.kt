package com.example

import com.example.testLibrary.statisticsReport
import de.infix.testBalloon.framework.TestBalloonExperimentalApi
import de.infix.testBalloon.framework.TestConfig
import de.infix.testBalloon.framework.TestInvocation
import de.infix.testBalloon.framework.coroutineContext
import de.infix.testBalloon.framework.disable
import de.infix.testBalloon.framework.dispatcherWithParallelism
import de.infix.testBalloon.framework.invocation
import de.infix.testBalloon.framework.testPlatform
import de.infix.testBalloon.framework.testSuite
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

// Combine concurrent invocation and multithreading by chaining configuration elements.

val UsingContextElement by testSuite(
    testConfig = TestConfig
        .invocation(TestInvocation.CONCURRENT)
        .coroutineContext(dispatcherWithParallelism(4))
        .statisticsReport() // a custom configuration for reporting
        .chainedWith(
            // Conditionally disable the test suite on single-threaded platforms.
            @OptIn(TestBalloonExperimentalApi::class)
            if (testPlatform.parallelism < 2) {
                TestConfig.disable()
            } else {
                TestConfig
            }
        )
) {
    for (coroutineId in 1..20) {
        test("#$coroutineId") {
            delay(10.milliseconds)
        }
    }
}
