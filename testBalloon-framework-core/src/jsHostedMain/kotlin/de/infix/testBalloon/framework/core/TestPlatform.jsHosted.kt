package de.infix.testBalloon.framework.core

import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@SubclassOptInRequired(markerClass = arrayOf(TestBalloonInternalApi::class))
public interface TestPlatformJsHosted : TestPlatform {
    public enum class Runtime(public val displayName: String) {
        Node("Node"),
        Browser("Browser");

        override fun toString(): String = displayName
    }

    public val runtime: Runtime

    override val parallelism: Int get() = 1

    @TestBalloonExperimentalApi
    override fun threadId(): ULong = 0UL

    @TestBalloonExperimentalApi
    override fun threadDisplayName(): String = "single"
}

public actual suspend fun withSingleThreadedDispatcher(action: suspend (dispatcher: CoroutineDispatcher) -> Unit) {
    action(Dispatchers.Default)
}
