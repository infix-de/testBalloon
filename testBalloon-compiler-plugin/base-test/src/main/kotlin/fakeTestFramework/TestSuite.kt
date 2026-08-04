package fakeTestFramework

import de.infix.testBalloon.framework.shared.AbstractTestSuite
import de.infix.testBalloon.framework.shared.TestElementName
import de.infix.testBalloon.framework.shared.TestRegistering
import de.infix.testBalloon.framework.shared.TestSuitePropertyName

@Suppress("unused")
@TestRegistering
fun testSuite(
    @TestElementName name: String? = null,
    @TestSuitePropertyName qualifiedPropertyName: String = "",
    content: TestSuite.() -> Unit
): Lazy<TestSuite> = lazy {
    TestSuite(
        name = name ?: qualifiedPropertyName,
        qualifiedPropertyName = qualifiedPropertyName,
        content = content
    )
}

open class TestSuite(name: String = "", qualifiedPropertyName: String? = null, content: TestSuite.() -> Unit = {}) :
    TestElement(name, qualifiedPropertyName = qualifiedPropertyName),
    AbstractTestSuite {

    override var testElementIsEnabled: Boolean = true

    init {
        content()
    }
}
