package de.infix.testBalloon.framework

import kotlinx.coroutines.CoroutineDispatcher

/**
 * The platform executing tests.
 */
@TestBalloonExperimentalApi
public interface TestPlatform {
    public enum class Type { JVM, NATIVE, JS, WASM_JS, WASM_WASI }

    /** The platform's type. */
    public val type: Type

    /** The platform's human-readable name. NOTE: Consider it unstable, use [type] for conditional code. */
    public val displayName: String

    /** The platform's default parallelism. */
    public val parallelism: Int

    /** The ID of the current thread. NOTE: For debugging purposes only, do not make assumptions about its value. */
    public fun threadId(): ULong

    /** The display name of the current thread. NOTE: For debugging purposes only, do not make assumptions about it. */
    public fun threadDisplayName(): String
}

/**
 * The [TestPlatform] currently used to execute tests.
 */
@TestBalloonExperimentalApi
public expect val testPlatform: TestPlatform

/**
 * Returns a [CoroutineDispatcher] providing a maximum amount of [parallelism], limited by the platform's capabilities.
 *
 * Use this utility function for testing special cases.
 */
public expect fun dispatcherWithParallelism(parallelism: Int): CoroutineDispatcher

/**
 * Executes [action], providing it with a single-threaded [CoroutineDispatcher].
 *
 * The dispatcher provided is guaranteed not to leak resources after use. It is not guaranteed to be usable when
 * this function completes.
 *
 * Use this utility function for testing special cases.
 */
@TestBalloonExperimentalApi
public expect suspend fun withSingleThreadedDispatcher(action: suspend (dispatcher: CoroutineDispatcher) -> Unit)
