package de.infix.testBalloon.framework.core.internal

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import de.infix.testBalloon.framework.core.TestSession
import de.infix.testBalloon.framework.core.internal.integration.TeamCityTestExecutionReport
import de.infix.testBalloon.framework.core.internal.integration.ThrowingTestSetupReport
import de.infix.testBalloon.framework.shared.AbstractTestSuite
import de.infix.testBalloon.framework.shared.internal.InvokedByGeneratedCode
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.system.exitProcess
import kotlin.time.Duration

@InvokedByGeneratedCode
internal actual suspend fun setUpAndExecuteTests(suites: Array<AbstractTestSuite>) {
    // `suites` is unused because test suites register themselves with `TestSession`.

    // This function is intended for internal framework testing only:
    // On Android, tests will be discovered and executed via JUnit 4, which means that this function
    // will not be used.

    configureTestsWithExceptionHandling {
        TestSession.global.setUp(ThrowingTestSetupReport())
    }.onSuccess {
        executeTestsWithExceptionHandling {
            TestSession.global.execute(TeamCityTestExecutionReport())
        }
    }
}

internal actual suspend fun TestScope.runTestAwaitingCompletion(
    timeout: Duration,
    action: suspend TestScope.() -> Unit
) {
    runTest(timeout = timeout) { action() }
}

internal actual fun handleFrameworkLevelError(throwable: Throwable) {
    exitProcess(3)
}

// The Android test infrastructure uses `RunListener`s to finish activities. The entire mechanism is thread-based
// and expects test events to appear synchronously.
internal actual val testInfrastructureSupportsConcurrency: Boolean = false

internal val instrumentationArguments: Bundle? by lazy {
    runCatching { InstrumentationRegistry.getArguments() }.getOrNull()
}

internal actual val testInfrastructureIsAndroidDevice: Boolean = instrumentationArguments != null

internal actual val defaultReportingPathLimit: Int? = null

/**
 * Default maximum length of the reporting path, excluding the top-level suite name, supported by the platform, or null.
 *
 * A path exceeding a certain limit crashes the Android device test run. The exact cause has not been determined, but
 * crashes have been observed with
 * - Gradle-managed devices,
 * - below-top-level path lengths exceeding 138 characters,
 * - below-top-level paths not being globally unique.
 *
 * A secondary restriction exists to avoid TraceRunListener warnings "Span name exceeds limits".
 * Android's TraceRunListener tries to get a class name by calling `description.getTestClass().getSimpleName()`
 * instead of `description.getClassName()`. Since there is no test class in TestBalloon, the former fails, and
 * TraceRunListener uses the constant `None` as a "test class" name. "Test class" and "test method" names
 * concatenated by `#` may not exceed 127 characters.
 * See https://github.com/android/android-test/blob/5b83cd99b2f6df8a7ce910f7b34917b30d73f0ad/runner/android_junit_runner/java/androidx/test/internal/runner/listener/TraceRunListener.java#L29
 */
internal actual val defaultReportingPathLimitBelowTopLevel: Int? =
    if (testInfrastructureIsAndroidDevice) {
        127 - "None#".length
    } else {
        null
    }
