package de.infix.testBalloon.framework.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

public interface TestPlatformJsHosted : TestPlatform {
    public enum class Runtime(public val displayName: String) {
        NODE("Node"),
        BROWSER("Browser");

        override fun toString(): String = displayName
    }

    public val runtime: Runtime

    override val parallelism: Int get() = 1

    @TestBalloonExperimentalApi
    override fun threadId(): ULong = 0UL

    @TestBalloonExperimentalApi
    override fun threadDisplayName(): String = "single"
}

@Deprecated("This function has no compelling use case in testing. Scheduled for removal in TestBalloon 0.8.")
public actual fun dispatcherWithParallelism(parallelism: Int): CoroutineDispatcher =
    Dispatchers.Default // single-threaded on JS

@TestBalloonExperimentalApi
public actual suspend fun withSingleThreadedDispatcher(action: suspend (dispatcher: CoroutineDispatcher) -> Unit) {
    action(Dispatchers.Default)
}
