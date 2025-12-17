package de.infix.testBalloon.framework.core

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.newSingleThreadContext
import platform.posix.getenv
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.Worker

public actual val testPlatform: TestPlatform = TestPlatformNative

public object TestPlatformNative : TestPlatform {
    override val type: TestPlatform.Type = TestPlatform.Type.Native

    @OptIn(ExperimentalNativeApi::class)
    override val displayName: String = "Native/${Platform.cpuArchitecture.name}/${Platform.osFamily.name}"

    @OptIn(ExperimentalNativeApi::class)
    override val parallelism: Int = Platform.getAvailableProcessors()

    @OptIn(ExperimentalStdlibApi::class, ObsoleteWorkersApi::class)
    @TestBalloonExperimentalApi
    override fun threadId(): ULong = Worker.current.platformThreadId

    @TestBalloonExperimentalApi
    override fun threadDisplayName(): String = threadId().toString()

    @OptIn(ExperimentalForeignApi::class)
    override fun environment(variableName: String): String? = getenv(variableName)?.toKString()
}

@Deprecated("This function has no compelling use case in testing. Scheduled for removal in TestBalloon 0.8.")
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
