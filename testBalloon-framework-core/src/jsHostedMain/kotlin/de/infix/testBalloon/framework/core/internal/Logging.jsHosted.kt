package de.infix.testBalloon.framework.core.internal

import de.infix.testBalloon.framework.core.TestPlatformJsHosted
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi

internal val runsInBrowser: Boolean =
    (testPlatform as? TestPlatformJsHosted)?.runtime == TestPlatformJsHosted.Runtime.BROWSER

@TestBalloonInternalApi
public actual fun printlnFixed(message: Any?) {
    if (runsInBrowser) {
        println(message.toString().replace("\n", "\n\n") + "\n")
    } else {
        println(message)
    }
}
