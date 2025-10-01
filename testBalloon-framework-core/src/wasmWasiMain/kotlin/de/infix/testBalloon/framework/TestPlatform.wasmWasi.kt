package de.infix.testBalloon.framework

import de.infix.testBalloon.framework.internal.wasmWasiEnvironment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@TestBalloonExperimentalApi
public actual val testPlatform: TestPlatform = TestPlatformWasmWasi

@TestBalloonExperimentalApi
public object TestPlatformWasmWasi : TestPlatform {
    override val type: TestPlatform.Type = TestPlatform.Type.WASM_WASI
    override val displayName: String = "Wasm/WASI"
    override val parallelism: Int = 1
    override fun threadId(): ULong = 0UL
    override fun threadDisplayName(): String = "single"
    override fun environment(variableName: String): String? = wasmWasiEnvironment[variableName]
}

public actual fun dispatcherWithParallelism(parallelism: Int): CoroutineDispatcher =
    Dispatchers.Default // single-threaded on Wasm/WASI until shared-everything threads are available

@TestBalloonExperimentalApi
public actual suspend fun withSingleThreadedDispatcher(action: suspend (dispatcher: CoroutineDispatcher) -> Unit) {
    action(Dispatchers.Default)
}
