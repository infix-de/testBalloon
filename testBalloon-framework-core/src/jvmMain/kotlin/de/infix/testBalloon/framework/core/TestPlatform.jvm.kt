package de.infix.testBalloon.framework.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext

public actual val testPlatform: TestPlatform = TestPlatformJvm

public object TestPlatformJvm : TestPlatform {
    override val type: TestPlatform.Type = TestPlatform.Type.JVM
    override val displayName: String = "JVM"
    override val parallelism: Int = Runtime.getRuntime().availableProcessors()

    @Suppress("DEPRECATION")
    @TestBalloonExperimentalApi
    override fun threadId(): ULong = Thread.currentThread().id.toULong()

    @TestBalloonExperimentalApi
    override fun threadDisplayName(): String = Thread.currentThread().name ?: "(thread ${threadId()})"

    override fun environment(variableName: String): String? = System.getenv(variableName)
}

@Deprecated("This function has no compelling use case in testing. Scheduled for removal in TestBalloon 0.8.")
public actual fun dispatcherWithParallelism(parallelism: Int): CoroutineDispatcher =
    Dispatchers.IO.limitedParallelism(parallelism)

@TestBalloonExperimentalApi
public actual suspend fun withSingleThreadedDispatcher(action: suspend (dispatcher: CoroutineDispatcher) -> Unit) {
    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    newSingleThreadContext("single-threading").use { dispatcher ->
        action(dispatcher)
    }
}
