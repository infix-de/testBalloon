package de.infix.testBalloon.framework.core.internal

import kotlinx.coroutines.await
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.js.Promise
import kotlin.time.Duration

internal actual suspend fun TestScope.runTestAwaitingCompletion(
    timeout: Duration,
    action: suspend TestScope.() -> Unit
) {
    @Suppress("CAST_NEVER_SUCCEEDS")
    (runTest(timeout = timeout) { action() } as Promise<Unit>).await()
}

internal actual fun handleFrameworkLevelError(throwable: Throwable) {
    if (runsInBrowser && !outputTestsFinishedMarkerIfApplicable()) {
        // Throwing here helps with browser tests via Karma. The error message is incomplete, but it seems
        // to be the only way we can get one at all.
        throw throwable
    }
    // With browser tests via Playwright or Node tests, there seems to be nothing we can do to signal an unsuccessful
    // exit status: We'd either lose the original error message (throwing or calling `process.exit(3)` on Node), or
    // not make a difference (throwing on browsers or setting `process.exitCode = 3` on Node).
}

internal actual fun outputTestsFinishedMarkerIfApplicable(): Boolean = withKotlinTestConfigOrNull {
    js("console.log(window.kotlinTestConfig.testsFinishedMarker)")
    true
} ?: false

internal actual fun kotlinTestConfigIncludePatterns(): List<String>? = withKotlinTestConfigOrNull {
    (js("""window.kotlinTestConfig.include || []""") as Array<*>).map { it.toString() }
}

internal actual fun kotlinTestConfigExcludePatterns(): List<String>? = withKotlinTestConfigOrNull {
    (js("""window.kotlinTestConfig.exclude || []""") as Array<*>).map { it.toString() }
}

private fun <Result : Any> withKotlinTestConfigOrNull(value: () -> Result): Result? =
    if (js("""typeof window !== "undefined" && typeof window.kotlinTestConfig !== "undefined"""")) {
        value()
    } else {
        null
    }
