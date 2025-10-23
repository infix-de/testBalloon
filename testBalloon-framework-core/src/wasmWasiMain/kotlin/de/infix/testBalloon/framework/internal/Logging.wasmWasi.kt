package de.infix.testBalloon.framework.internal

import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi

@TestBalloonInternalApi
public actual fun printlnFixed(message: Any?) {
    println(message)
}
