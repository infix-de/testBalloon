package de.infix.testBalloon.framework.core

import de.infix.testBalloon.framework.core.internal.instrumentationArguments
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext

public actual val testPlatform: TestPlatform = TestPlatformAndroid

public object TestPlatformAndroid : TestPlatform {
    override val type: TestPlatform.Type = TestPlatform.Type.Jvm
    override val displayName: String = "Android"
    override val parallelism: Int = Runtime.getRuntime().availableProcessors()

    @Suppress("DEPRECATION")
    @TestBalloonExperimentalApi
    override fun threadId(): ULong = Thread.currentThread().id.toULong()

    @TestBalloonExperimentalApi
    override fun threadDisplayName(): String = Thread.currentThread().name ?: "(thread ${threadId()})"

    override fun environment(variableName: String): String? = System.getenv(variableName)
        ?: instrumentationArguments?.getString(variableName)
}

@ExperimentalCoroutinesApi
public actual suspend fun withSingleThreadedDispatcher(action: suspend (dispatcher: CoroutineDispatcher) -> Unit) {
    @OptIn(DelicateCoroutinesApi::class)
    newSingleThreadContext("single-threading").use { dispatcher ->
        action(dispatcher)
    }
}
