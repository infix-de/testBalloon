package de.infix.testBalloon.framework.internal

import kotlin.wasm.WasmImport
import kotlin.wasm.unsafe.Pointer
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator

internal val wasmWasiEnvironment: Map<String, String> by lazy { environGet() }

/*
 * The following code originates in KoWasm, and bears the following copyright statement:
 *
 * Copyright 2023 the original author or authors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 *
 * KoWasm license file: https://github.com/kowasm/kowasm/blob/d2db785c714ec5aea5aff7c8acad24834b9a2f3a/LICENSE
 * Original file: https://github.com/kowasm/kowasm/blob/d2db785c714ec5aea5aff7c8acad24834b9a2f3a/wasi/src/wasmJsMain/kotlin/org/kowasm/wasi/internal/Env.kt
 * KoWasm project: https://github.com/kowasm/kowasm
 */

private fun environGet(): Map<String, String> {
    val (numArgs, bufSize) = environSizesGet()
    val tmpByteArray = ByteArray(bufSize)
    @OptIn(UnsafeWasmMemoryApi::class)
    withScopedMemoryAllocator { allocator ->
        val environ = allocator.allocate(numArgs * 4)
        val environBuffer = allocator.allocate(bufSize)
        val ret = wasmEnvironGet(environ.address.toInt(), environBuffer.address.toInt())
        if (ret != 0) {
            throw WasiException(ret)
        }
        val result = mutableMapOf<String, String>()
        repeat(numArgs) { idx ->
            val environPtr = environ + idx * 4
            val ptr = Pointer(environPtr.loadInt().toUInt())
            val endIdx = readZeroTerminatedByteArray(ptr, tmpByteArray)
            val str = tmpByteArray.decodeToString(endIndex = endIdx)
            val (key, value) = str.split("=", limit = 2)
            result[key] = value
        }
        return result
    }
}

private fun environSizesGet(): Pair<Size, Size> {
    @OptIn(UnsafeWasmMemoryApi::class)
    withScopedMemoryAllocator { allocator ->
        val rp0 = allocator.allocate(4)
        val rp1 = allocator.allocate(4)
        val ret = wasmEnvironSizesGet(rp0.address.toInt(), rp1.address.toInt())
        return if (ret == 0) {
            Pair(
                (Pointer(rp0.address.toInt().toUInt())).loadInt(),
                (Pointer(rp1.address.toInt().toUInt())).loadInt()
            )
        } else {
            throw WasiException(ret)
        }
    }
}

@OptIn(UnsafeWasmMemoryApi::class)
private fun readZeroTerminatedByteArray(ptr: Pointer, byteArray: ByteArray): Int {
    for (i in byteArray.indices) {
        val b = (ptr + i).loadByte()
        if (b.toInt() == 0) {
            return i
        }
        byteArray[i] = b
    }
    error("Zero-terminated array is out of bounds")
}

private class WasiException(errorCode: Int) : RuntimeException(message = "WASI call failed with $errorCode")

private typealias Size = Int

@OptIn(ExperimentalWasmInterop::class)
@WasmImport("wasi_snapshot_preview1", "environ_get")
private external fun wasmEnvironGet(arg0: Int, arg1: Int): Int

@OptIn(ExperimentalWasmInterop::class)
@WasmImport("wasi_snapshot_preview1", "environ_sizes_get")
private external fun wasmEnvironSizesGet(arg0: Int, arg1: Int): Int
