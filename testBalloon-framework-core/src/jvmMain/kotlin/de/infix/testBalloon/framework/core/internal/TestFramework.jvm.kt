package de.infix.testBalloon.framework.core.internal

import de.infix.testBalloon.framework.core.TestSession
import de.infix.testBalloon.framework.core.internal.integration.TeamCityTestExecutionReport
import de.infix.testBalloon.framework.core.internal.integration.ThrowingTestConfigurationReport
import de.infix.testBalloon.framework.shared.AbstractTestSuite
import de.infix.testBalloon.framework.shared.internal.InvokedByGeneratedCode
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.system.exitProcess
import kotlin.time.Duration

@InvokedByGeneratedCode
internal actual suspend fun configureAndExecuteTests(suites: Array<AbstractTestSuite>) {
    // `suites` is unused because test suites register themselves with `TestSession`.

    // This function is intended for internal framework testing only:
    // On the JVM, tests will be discovered and executed via JUnit Platform, which means that this function
    // will not be used.

    configureTestsWithExceptionHandling {
        TestSession.global.parameterize(ThrowingTestConfigurationReport())
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

internal actual val defaultReportingPathLimit: Int? = null
