@file:OptIn(ExperimentalWasmJsInterop::class)

package de.infix.testBalloon.framework.core.internal

import kotlinx.coroutines.await
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.time.Duration

internal actual suspend fun TestScope.runTestAwaitingCompletion(
    timeout: Duration,
    action: suspend TestScope.() -> Unit
) {
    @Suppress("CAST_NEVER_SUCCEEDS")
    (runTest(timeout = timeout) { action() } as Promise<JsAny>).await<Unit>()
}

internal actual fun handleFrameworkLevelError(throwable: Throwable) {
    if (runsInBrowser) {
        outputTestsFinishedMarkerIfApplicable()
        // With browser tests, there seems to be nothing we can do to signal an unsuccessful exit status.
    } else {
        printStderr(throwable.stackTraceToString())
        exitProcess(3)
    }
}

private fun printStderr(@Suppress("unused") message: String) {
    js("process.stderr.write(message)")
}

private fun exitProcess(@Suppress("unused") status: Int) {
    js("process.exit(status)")
}

internal actual fun outputTestsFinishedMarkerIfApplicable(): Boolean = if (kotlinTestConfigExists()) {
    outputExistingTestsFinishedMarker()
    true
} else {
    false
}

private fun outputExistingTestsFinishedMarker(): Unit = js("console.log(window.kotlinTestConfig.testsFinishedMarker)")

internal actual fun kotlinTestConfigIncludePatterns(): List<String>? = withKotlinTestConfigOrNull {
    existingIncludePatterns().toArray().map { it.toString() }
}

private fun existingIncludePatterns(): JsArray<*> = js("""window.kotlinTestConfig.include || []""")

internal actual fun kotlinTestConfigExcludePatterns(): List<String>? = withKotlinTestConfigOrNull {
    existingExcludePatterns().toArray().map { it.toString() }
}

private fun existingExcludePatterns(): JsArray<*> = js("""window.kotlinTestConfig.exclude || []""")

private fun <Result : Any> withKotlinTestConfigOrNull(value: () -> Result): Result? = if (kotlinTestConfigExists()) {
    value()
} else {
    null
}

private fun kotlinTestConfigExists(): Boolean =
    js("""typeof window !== "undefined" && typeof window.kotlinTestConfig !== "undefined"""")
