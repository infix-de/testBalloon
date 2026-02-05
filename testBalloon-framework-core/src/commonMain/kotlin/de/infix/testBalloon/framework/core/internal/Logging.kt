package de.infix.testBalloon.framework.core.internal

import de.infix.testBalloon.framework.core.TestBalloonExperimentalApi
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@TestBalloonExperimentalApi
public enum class LogLevel { DEBUG, INFO, ERROR }

@TestBalloonExperimentalApi
public var testFrameworkLogLevel: LogLevel = LogLevel.INFO

@TestBalloonInternalApi
public fun logDebug(message: () -> String) {
    log(LogLevel.DEBUG) { "DEBUG: ${message()}" }
}

@TestBalloonInternalApi
public fun logInfo(message: () -> String) {
    log(LogLevel.INFO) { "INFO: ${message()}" }
}

@TestBalloonInternalApi
public fun logError(message: () -> String) {
    log(LogLevel.ERROR) { "ERROR: ${message()}" }
}

@TestBalloonInternalApi
public fun log(messageLevel: LogLevel, message: () -> String) {
    if (messageLevel >= testFrameworkLogLevel) {
        @OptIn(ExperimentalTime::class)
        printlnFixed("${Clock.System.now()} [${testPlatform.threadId()}] ${message()}")
    }
}

@TestBalloonInternalApi
public inline fun <Result> withLog(messageLevel: LogLevel, message: String, action: () -> Result): Result {
    try {
        log(messageLevel) { "$message [${testPlatform.displayName}] – start" }
        val result = action()
        log(messageLevel) { "$message [${testPlatform.displayName}] – end" }
        return result
    } catch (throwable: Throwable) {
        log(messageLevel) { "$message [${testPlatform.displayName}] – end with $throwable" }
        throw throwable
    }
}

/**
 * WORKAROUND https://youtrack.jetbrains.com/issue/KT-48292 – KJS / IR: `println` doesn't move to a new line in tests
 */
@TestBalloonInternalApi
public expect fun printlnFixed(message: Any?)

@TestBalloonInternalApi
public fun Throwable.logErrorWithStacktrace(headline: String, includeStacktrace: Boolean = true) {
    logError {
        buildString {
            append(headline)
            message?.let { primaryMessage ->
                append("\n\t$primaryMessage")
                cause?.let { cause ->
                    append("\n")
                    append(cause.toString().prependIndent("\t"))
                }
            }
            if (includeStacktrace) {
                append("\n\tStack trace:\n")
                append(stackTraceToString().prependIndent("\t\t"))
            }
        }
    }
}
