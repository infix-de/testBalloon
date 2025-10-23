package fakeTestFramework

import de.infix.testBalloon.framework.shared.AbstractTestElement
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi

sealed class TestElement(private val name: String) : AbstractTestElement {
    override val testElementPath: AbstractTestElement.Path = object : AbstractTestElement.Path {
        @OptIn(TestBalloonInternalApi::class)
        override fun toString() = name
    }
}
