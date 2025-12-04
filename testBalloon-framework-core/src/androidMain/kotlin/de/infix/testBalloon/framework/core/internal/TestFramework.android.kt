package de.infix.testBalloon.framework.core.internal

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

// Test element paths exceeding a certain length crash Android device-side Tests.
// The exact length depends on the number of elements in the path (hierarchical depth). It is around 242
// characters for a hierarchy of depth 3, reduced by 4 characters per extra hierarchy level.
// Not knowing the hierarchy depth here, we try to be safe and stay well below the maximum possible number.
internal actual val defaultReportingPathLimit: Int? = 200
