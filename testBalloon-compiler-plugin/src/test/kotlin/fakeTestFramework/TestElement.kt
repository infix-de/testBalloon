package fakeTestFramework

import de.infix.testBalloon.framework.shared.AbstractTestElement
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi

// This test element can only represent a top-level test suite.
sealed class TestElement(@Suppress("unused") name: String, private val propertyFqn: String?) : AbstractTestElement {
    override val testElementPath: AbstractTestElement.Path = object : AbstractTestElement.Path {
        @OptIn(TestBalloonInternalApi::class)
        override fun toString() = propertyFqn ?: "?"
    }
}
