package de.infix.testBalloon.framework

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.newSingleThreadContext
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.Worker

@TestBalloonExperimentalApi
public actual val testPlatform: TestPlatform = TestPlatformNative

@TestBalloonExperimentalApi
public object TestPlatformNative : TestPlatform {
    override val type: TestPlatform.Type = TestPlatform.Type.NATIVE

    @OptIn(ExperimentalNativeApi::class)
    override val displayName: String = "Native/${Platform.cpuArchitecture.name}/${Platform.osFamily.name}"

    @OptIn(ExperimentalNativeApi::class)
    override val parallelism: Int = Platform.getAvailableProcessors()

    @OptIn(ExperimentalStdlibApi::class, ObsoleteWorkersApi::class)
    override fun threadId(): ULong = Worker.current.platformThreadId
    override fun threadDisplayName(): String = threadId().toString()
}

public actual fun dispatcherWithParallelism(parallelism: Int): CoroutineDispatcher =
    Dispatchers.IO.limitedParallelism(parallelism)

@TestBalloonExperimentalApi
public actual suspend fun withSingleThreadedDispatcher(action: suspend (dispatcher: CoroutineDispatcher) -> Unit) {
    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    val dispatcher = newSingleThreadContext("single-threading")
    AutoCloseable { dispatcher.close() }.use {
        action(dispatcher)
    }
}
