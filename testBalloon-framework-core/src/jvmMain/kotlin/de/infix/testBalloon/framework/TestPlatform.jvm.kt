package de.infix.testBalloon.framework

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext

@TestBalloonExperimentalApi
public actual val testPlatform: TestPlatform = TestPlatformJvm

@TestBalloonExperimentalApi
public object TestPlatformJvm : TestPlatform {
    override val type: TestPlatform.Type = TestPlatform.Type.JVM
    override val displayName: String = "JVM"
    override val parallelism: Int = Runtime.getRuntime().availableProcessors()

    @Suppress("DEPRECATION")
    override fun threadId(): ULong = Thread.currentThread().id.toULong()
    override fun threadDisplayName(): String = Thread.currentThread().name ?: "(thread ${threadId()})"
}

public actual fun dispatcherWithParallelism(parallelism: Int): CoroutineDispatcher =
    Dispatchers.IO.limitedParallelism(parallelism)

@TestBalloonExperimentalApi
public actual suspend fun withSingleThreadedDispatcher(action: suspend (dispatcher: CoroutineDispatcher) -> Unit) {
    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    newSingleThreadContext("single-threading").use { dispatcher ->
        action(dispatcher)
    }
}
