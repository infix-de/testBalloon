package com.example.testLibrary

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestExecutionScope
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.testScope
import de.infix.testBalloon.framework.shared.TestDiscoverable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

/**
 * Registers a test with a [timeout] as defined in kotlinx.coroutines [withTimeout].
 */
fun TestSuite.test(name: String, timeout: Duration, action: suspend TestExecutionScope.() -> Unit) =
    test(name, testConfig = TestConfig.testScope(isEnabled = false)) {
        try {
            withTimeout(timeout) {
                action()
            }
        } catch (timeoutCancellationException: TimeoutCancellationException) {
            throw AssertionError("$timeoutCancellationException", timeoutCancellationException)
        }
    }

/**
 * Registers a test series with a number of [iterations].
 */
fun TestSuite.testSeries(name: String, iterations: Int, action: suspend TestExecutionScope.() -> Unit) {
    for (iteration in 1..iterations) {
        test("$name $iteration") {
            action()
        }
    }
}

/**
 * Registers a test with a non-standard signature for its [action] parameter.
 *
 * The IDE plugin requires a `@TestDiscoverable` annotation to recognize an invocation of this function as registering
 * a test.
 */
@TestDiscoverable
fun TestSuite.databaseTest(name: String, action: suspend Database.() -> Unit) {
    test(name) {
        Database(this).use {
            it.action()
        }
    }
}

class Database(scope: CoroutineScope) : AutoCloseable {
    override fun close() {
        // Do stuff.
    }
}
