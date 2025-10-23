package de.infix.testBalloon.framework.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@TestBalloonExperimentalApi
public interface TestPlatformJsHosted : TestPlatform {
    public enum class Runtime(public val displayName: String) {
        NODE("Node"),
        BROWSER("Browser");

        override fun toString(): String = displayName
    }

    public val runtime: Runtime

    override val parallelism: Int get() = 1
    override fun threadId(): ULong = 0UL
    override fun threadDisplayName(): String = "single"
}

public actual fun dispatcherWithParallelism(parallelism: Int): CoroutineDispatcher =
    Dispatchers.Default // single-threaded on JS

@TestBalloonExperimentalApi
public actual suspend fun withSingleThreadedDispatcher(action: suspend (dispatcher: CoroutineDispatcher) -> Unit) {
    action(Dispatchers.Default)
}
