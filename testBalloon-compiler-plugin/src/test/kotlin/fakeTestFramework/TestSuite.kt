package fakeTestFramework

import de.infix.testBalloon.framework.shared.AbstractTestSuite
import de.infix.testBalloon.framework.shared.TestElementName
import de.infix.testBalloon.framework.shared.TestElementPropertyFqn
import de.infix.testBalloon.framework.shared.TestRegistering

@Suppress("unused")
@TestRegistering
fun testSuite(
    @TestElementName name: String? = null,
    @TestElementPropertyFqn propertyFqn: String = "",
    content: TestSuite.() -> Unit
): Lazy<TestSuite> = lazy {
    TestSuite(
        name = name ?: propertyFqn,
        propertyFqn = propertyFqn,
        content = content
    )
}

open class TestSuite(name: String = "", propertyFqn: String? = null, content: TestSuite.() -> Unit = {}) :
    TestElement(name, propertyFqn = propertyFqn),
    AbstractTestSuite {

    override var testElementIsEnabled: Boolean = true

    init {
        content()
    }
}
