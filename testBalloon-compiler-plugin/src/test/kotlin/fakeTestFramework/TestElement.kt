package fakeTestFramework

import de.infix.testBalloon.framework.AbstractTestElement
import de.infix.testBalloon.framework.internal.TestBalloonInternalApi
import de.infix.testBalloon.framework.internal.externalId

sealed class TestElement(private val name: String) : AbstractTestElement {
    override val testElementPath: AbstractTestElement.Path = object : AbstractTestElement.Path {
        @OptIn(TestBalloonInternalApi::class)
        override fun toString() = name.externalId()
    }
}
