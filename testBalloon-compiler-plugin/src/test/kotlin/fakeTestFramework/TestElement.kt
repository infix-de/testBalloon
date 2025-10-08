package fakeTestFramework

import de.infix.testBalloon.framework.AbstractTestElement
import de.infix.testBalloon.framework.internal.TestBalloonInternalApi

sealed class TestElement(private val name: String) : AbstractTestElement {
    override val testElementPath: AbstractTestElement.Path = object : AbstractTestElement.Path {
        @OptIn(TestBalloonInternalApi::class)
        override fun toString() = name
    }
}
