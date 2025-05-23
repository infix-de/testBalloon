package de.infix.testBalloon.framework

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val testPlatform: TestPlatform = TestPlatformWasmWasi

object TestPlatformWasmWasi : TestPlatform {
    override val type: TestPlatform.Type = TestPlatform.Type.WASM_WASI
    override val displayName = "Wasm/WASI"
    override val parallelism = 1
    override fun threadId() = 0UL
    override fun threadDisplayName() = "single"
}

actual fun dispatcherWithParallelism(parallelism: Int): CoroutineDispatcher =
    Dispatchers.Default // single-threaded on Wasm/WASI until shared-everything threads are available

actual suspend fun withSingleThreadedDispatcher(action: suspend (dispatcher: CoroutineDispatcher) -> Unit) =
    action(Dispatchers.Default)
