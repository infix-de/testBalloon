@file:Suppress("unused", "ktlint:standard:function-literal")

package com.example

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestExecutionScope
import de.infix.testBalloon.framework.core.TestInvocation
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.aroundEachTest
import de.infix.testBalloon.framework.core.coroutineContext
import de.infix.testBalloon.framework.core.dispatcherWithParallelism
import de.infix.testBalloon.framework.core.invocation
import de.infix.testBalloon.framework.core.testScope
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.TestDiscoverable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

// --8<-- [start:test-with-timeout]
fun TestSuite.test(
    name: String,
    timeout: Duration,
    action: suspend TestExecutionScope.() -> Unit
) = test(
    "$name (timeout: $timeout)",
    testConfig = TestConfig.testScope(false)
) {
    try {
        withTimeout(timeout) {
            action()
        }
    } catch (cancellation: TimeoutCancellationException) {
        throw AssertionError("$cancellation", cancellation)
    }
}
// --8<-- [end:test-with-timeout]

// --8<-- [start:test-series]
fun TestSuite.testSeries(
    name: String,
    iterations: Int,
    action: suspend TestExecutionScope.() -> Unit
) {
    for (iteration in 1..iterations) {
        test("$name $iteration") {
            action()
        }
    }
}
// --8<-- [end:test-series]

// --8<-- [start:test-with-database-context]
@TestDiscoverable // (1)!
fun TestSuite.databaseTest(name: String, action: suspend Database.() -> Unit) {
    test(name) {
        Database(this).use { // (2)!
            it.action() // (3)!
        }
    }
}
// --8<-- [end:test-with-database-context]

class Database(coroutineScope: CoroutineScope) : AutoCloseable {
    override fun close() {}
}

fun TestConfig.statisticsReport(): TestConfig = this

val Other by testSuite {
// --8<-- [start:concurrency]
    testSuite(
        "let's test concurrency",
        testConfig = TestConfig
            .invocation(TestInvocation.CONCURRENT) // (1)!
            .coroutineContext(dispatcherWithParallelism(4)) // (2)!
            .statisticsReport() // (3)!
    ) {
        // ...
    }
// --8<-- [end:concurrency]

    // --8<-- [start:custom-test-config-function]
    fun TestConfig.onFourThreadsWithStatistics() = this // (1)!
        .invocation(TestInvocation.CONCURRENT)
        .coroutineContext(dispatcherWithParallelism(4))
        .statisticsReport()
    // --8<-- [end:custom-test-config-function]

    // --8<-- [start:reuse-custom-test-config-function]
    testSuite(
        "let's test concurrency",
        testConfig = TestConfig.onFourThreadsWithStatistics()
    ) {
        // ...
    }
    // --8<-- [end:reuse-custom-test-config-function]

    // --8<-- [start:custom-test-config-timeout-function]
    fun TestConfig.withTestTimeout(timeout: Duration) = this // (1)!
        .testScope(isEnabled = false) // (2)!
        .aroundEachTest { action -> // (3)!
            try {
                withTimeout(timeout) {
                    action()
                }
            } catch (cancellation: TimeoutCancellationException) {
                throw AssertionError("$cancellation", cancellation)
            }
        }
    // --8<-- [end:custom-test-config-timeout-function]
}

/*
@Suppress("ktlint:standard:property-naming", "LocalVariableName")
private val guardAgainstMultiplyDefinedSessions = {
    // --8<-- [start:test-suite-with-ui-compartment]
    val RealTimeTests by testSuite(
        compartment = { TestCompartment.RealTime } // (1)!
    ) {
        // ...
    }
    // --8<-- [end:test-suite-with-ui-compartment]

    // --8<-- [start:custom-test-session]
    class ModuleTestSession :
        TestSession(testConfig = DefaultConfiguration.statisticsReport())
    // --8<-- [end:custom-test-session]

    // --8<-- [start:concurrent-test-session]
    class ConcurrentTestSession :
        TestSession(
            defaultCompartment = { TestCompartment.Concurrent } // (1)!
        )
    // --8<-- [end:concurrent-test-session]
}
*/
