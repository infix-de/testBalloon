package com.example

import com.example.testLibrary.statisticsReport
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestInvocation
import de.infix.testBalloon.framework.core.coroutineContext
import de.infix.testBalloon.framework.core.disable
import de.infix.testBalloon.framework.core.dispatcherWithParallelism
import de.infix.testBalloon.framework.core.invocation
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.core.testSuite
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
            if (testPlatform.parallelism < 2) {
                TestConfig.disable()
            } else {
                TestConfig
            }
        )
) {
    for (coroutineId in 1..20) {
        test("test $coroutineId") {
            delay(10.milliseconds)
        }
    }
}
