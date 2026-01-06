package de.infix.testBalloon.framework.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext

public actual val testPlatform: TestPlatform = TestPlatformJvm

public object TestPlatformJvm : TestPlatform {
    override val type: TestPlatform.Type = TestPlatform.Type.Jvm
    override val displayName: String = "JVM"
    override val parallelism: Int = Runtime.getRuntime().availableProcessors()

    @Suppress("DEPRECATION")
    @TestBalloonExperimentalApi
    override fun threadId(): ULong = Thread.currentThread().id.toULong()

    @TestBalloonExperimentalApi
    override fun threadDisplayName(): String = Thread.currentThread().name ?: "(thread ${threadId()})"

    override fun environment(variableName: String): String? = System.getenv(variableName)
}

@TestBalloonExperimentalApi
public actual suspend fun withSingleThreadedDispatcher(action: suspend (dispatcher: CoroutineDispatcher) -> Unit) {
    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    newSingleThreadContext("single-threading").use { dispatcher ->
        action(dispatcher)
    }
}
