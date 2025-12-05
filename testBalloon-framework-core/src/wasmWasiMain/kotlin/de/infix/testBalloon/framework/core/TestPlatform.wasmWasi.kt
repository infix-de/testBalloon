package de.infix.testBalloon.framework.core

import de.infix.testBalloon.framework.core.internal.wasmWasiEnvironment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

public actual val testPlatform: TestPlatform = TestPlatformWasmWasi

public object TestPlatformWasmWasi : TestPlatform {
    override val type: TestPlatform.Type = TestPlatform.Type.WASM_WASI
    override val displayName: String = "Wasm/WASI"
    override val parallelism: Int = 1
    override fun threadId(): ULong = 0UL

    @TestBalloonExperimentalApi
    override fun threadDisplayName(): String = "single"

    @TestBalloonExperimentalApi
    override fun environment(variableName: String): String? = wasmWasiEnvironment[variableName]
}

@Deprecated("This function has no compelling use case in testing. Scheduled for removal in TestBalloon 0.8.")
public actual fun dispatcherWithParallelism(parallelism: Int): CoroutineDispatcher =
    Dispatchers.Default // single-threaded on Wasm/WASI until shared-everything threads are available

@TestBalloonExperimentalApi
public actual suspend fun withSingleThreadedDispatcher(action: suspend (dispatcher: CoroutineDispatcher) -> Unit) {
    action(Dispatchers.Default)
}
