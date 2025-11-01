package de.infix.testBalloon.framework.core.internal

import de.infix.testBalloon.framework.core.TestSession
import de.infix.testBalloon.framework.core.internal.integration.TeamCityTestExecutionReport
import de.infix.testBalloon.framework.core.internal.integration.ThrowingTestSetupReport
import de.infix.testBalloon.framework.shared.AbstractTestSuite
import de.infix.testBalloon.framework.shared.internal.InvokedByGeneratedCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.time.Duration

@InvokedByGeneratedCode
internal actual suspend fun setUpAndExecuteTests(suites: Array<AbstractTestSuite>) {
    // `suites` is unused because test suites register themselves with `TestSession`.

    // WORKAROUND Wasm/WASI with kotlinx.coroutines < 1.10.0: calling delay() silently exits wasmWasiNodeRun
    //     https://github.com/Kotlin/kotlinx.coroutines/issues/4239
    withContext(Dispatchers.Default) { }

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
    // We do nothing here and accept that no process failure will be signalled to the invoker, because:
    // – there is no process exit call in Kotlin's stdlib for Wasm/WASI, and
    // – throwing at this point would suppress the error message.
    // TODO: Find a way to signal a framework-level error status (process failure) to the invoker.
}
