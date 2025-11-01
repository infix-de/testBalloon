package fakeTestFramework

import de.infix.testBalloon.framework.shared.AbstractTestSuite
import de.infix.testBalloon.framework.shared.TestElementName
import de.infix.testBalloon.framework.shared.TestRegistering

@Suppress("unused")
@TestRegistering
fun testSuite(@TestElementName name: String = "", content: TestSuite.() -> Unit): Lazy<TestSuite> = lazy {
    TestSuite(
        name = name,
        content = content
    )
}

@TestRegistering
open class TestSuite(@TestElementName name: String = "", content: TestSuite.() -> Unit = {}) :
    TestElement(name),
    AbstractTestSuite {

    override var testElementIsEnabled: Boolean = true

    init {
        content()
    }
}
