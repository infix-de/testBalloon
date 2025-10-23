package de.infix.testBalloon.framework.core

@TestBalloonExperimentalApi
public actual val testPlatform: TestPlatform = TestPlatformJs

@TestBalloonExperimentalApi
public object TestPlatformJs : TestPlatformJsHosted {
    override val type: TestPlatform.Type = TestPlatform.Type.JS

    override val runtime: TestPlatformJsHosted.Runtime =
        if (runtimeIsNodeJs()) TestPlatformJsHosted.Runtime.NODE else TestPlatformJsHosted.Runtime.BROWSER

    override val displayName: String = "JS/$runtime"

    override fun environment(variableName: String): String? = jsEnvironment(variableName)
}

// https://stackoverflow.com/a/31090240
private fun runtimeIsNodeJs(): Boolean =
    js("(new Function('try { return this === global; } catch(e) { return false; }'))()") as Boolean

@Suppress("unused")
private fun jsEnvironment(variableName: String): String? = js(
    """
    if (typeof process !== "undefined") {
        if (typeof process.env !== "undefined")
            return process.env[variableName];
    } else if (typeof window !== "undefined" && typeof window.__karma__ !== "undefined" &&
                   typeof window.__karma__.config.env !== "undefined") {
        return window.__karma__.config.env[variableName];
    }
    return undefined;
    """
)?.toString()
