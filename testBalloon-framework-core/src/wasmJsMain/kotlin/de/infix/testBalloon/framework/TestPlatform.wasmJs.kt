package de.infix.testBalloon.framework

@TestBalloonExperimentalApi
public actual val testPlatform: TestPlatform = TestPlatformWasmJs

@TestBalloonExperimentalApi
public object TestPlatformWasmJs : TestPlatformJsHosted {
    override val type: TestPlatform.Type = TestPlatform.Type.WASM_JS

    override val runtime: TestPlatformJsHosted.Runtime =
        if (runtimeIsNodeJs()) TestPlatformJsHosted.Runtime.NODE else TestPlatformJsHosted.Runtime.BROWSER

    override val displayName: String = "Wasm/JS/$runtime"

    override fun environment(variableName: String): String? = wasmJsEnvironment(variableName)
}

// https://stackoverflow.com/a/31090240
private fun runtimeIsNodeJs(): Boolean =
    js("(new Function('try { return this === global; } catch(e) { return false; }'))()")

private fun wasmJsEnvironment(variableName: String): String? = if (wasmProcessEnvExists()) {
    if (wasmProcessEnvEntryExists(variableName)) wasmProcessEnvEntry(variableName) else null
} else {
    if (wasmKarmaEnvExists() && wasmKarmaEnvEntryExists(variableName)) wasmKarmaEnvEntry(variableName) else null
}

private fun wasmProcessEnvExists(): Boolean = js("typeof process !== 'undefined' && typeof process.env !== 'undefined'")

@Suppress("unused")
private fun wasmProcessEnvEntryExists(variableName: String): Boolean =
    js("typeof process.env[variableName] !== 'undefined'")

@Suppress("unused")
private fun wasmProcessEnvEntry(variableName: String): String = js("process.env[variableName]")

private fun wasmKarmaEnvExists(): Boolean = js(
    "typeof window !== 'undefined' && typeof window.__karma__ !== 'undefined' &&" +
        " typeof window.__karma__.config.env !== 'undefined'"
)

@Suppress("unused")
private fun wasmKarmaEnvEntryExists(variableName: String): Boolean =
    js("typeof window.__karma__.config.env[variableName] !== 'undefined'")

@Suppress("unused")
private fun wasmKarmaEnvEntry(variableName: String): String = js("window.__karma__.config.env[variableName]")
