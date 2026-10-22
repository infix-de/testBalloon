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
    // We throw, because this seems to be the only way to
    // - emit an error message when running in a browser,
    // - emit an error message _and_ fail when running on Node.js (exiting with a non-zero status would suppress
    //   the error message).
    throw throwable
}

internal actual fun outputTestsFinishedMarkerIfApplicable() {
    withKotlinTestConfigOrNull {
        js("console.log(window.kotlinTestConfig.testsFinishedMarker)")
    }
}

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
