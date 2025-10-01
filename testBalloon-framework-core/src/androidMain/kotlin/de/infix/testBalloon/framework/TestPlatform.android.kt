package de.infix.testBalloon.framework

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext

@TestBalloonExperimentalApi
public actual val testPlatform: TestPlatform = TestPlatformAndroid

@TestBalloonExperimentalApi
public object TestPlatformAndroid : TestPlatform {
    override val type: TestPlatform.Type = TestPlatform.Type.JVM
    override val displayName: String = "Android"
    override val parallelism: Int = Runtime.getRuntime().availableProcessors()

    @Suppress("DEPRECATION")
    override fun threadId(): ULong = Thread.currentThread().id.toULong()
    override fun threadDisplayName(): String = Thread.currentThread().name ?: "(thread ${threadId()})"

    override fun environment(variableName: String): String? = System.getenv(variableName)
}

public actual fun dispatcherWithParallelism(parallelism: Int): CoroutineDispatcher =
    Dispatchers.IO.limitedParallelism(parallelism)

@ExperimentalCoroutinesApi
public actual suspend fun withSingleThreadedDispatcher(action: suspend (dispatcher: CoroutineDispatcher) -> Unit) {
    @OptIn(DelicateCoroutinesApi::class)
    newSingleThreadContext("single-threading").use { dispatcher ->
        action(dispatcher)
    }
}
