package de.infix.testBalloon.framework

@TestBalloonExperimentalApi
public actual val testPlatform: TestPlatform = TestPlatformWasmJs

@TestBalloonExperimentalApi
public object TestPlatformWasmJs : TestPlatformJsHosted {
    override val type: TestPlatform.Type = TestPlatform.Type.WASM_JS

    override val runtime: TestPlatformJsHosted.Runtime =
        if (runtimeIsNodeJs()) TestPlatformJsHosted.Runtime.NODE else TestPlatformJsHosted.Runtime.BROWSER

    override val displayName: String = "Wasm/JS/$runtime"
}

// https://stackoverflow.com/a/31090240
private fun runtimeIsNodeJs(): Boolean =
    js("(new Function('try { return this === global; } catch(e) { return false; }'))()")
