package de.infix.testBalloon.framework

@TestBalloonExperimentalApi
public actual val testPlatform: TestPlatform = TestPlatformJs

@TestBalloonExperimentalApi
public object TestPlatformJs : TestPlatformJsHosted {
    override val type: TestPlatform.Type = TestPlatform.Type.JS

    override val runtime: TestPlatformJsHosted.Runtime =
        if (runtimeIsNodeJs()) TestPlatformJsHosted.Runtime.NODE else TestPlatformJsHosted.Runtime.BROWSER

    override val displayName: String = "JS/$runtime"
}

// https://stackoverflow.com/a/31090240
private fun runtimeIsNodeJs(): Boolean =
    js("(new Function('try { return this === global; } catch(e) { return false; }'))()") as Boolean
