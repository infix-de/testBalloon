package com.example.testLibrary

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.TestElementName
import de.infix.testBalloon.framework.shared.TestRegistering
import de.infix.testBalloon.framework.shared.TestSuitePropertyName

fun TestSuiteScope.describe(name: String, testConfig: TestConfig = TestConfig, content: TestSuite.() -> Unit) =
    testSuite(name, testConfig = testConfig, content = content)

fun TestSuiteScope.it(
    name: String,
    testConfig: TestConfig = TestConfig,
    action: suspend Test.ExecutionScope.() -> Unit
) = test(name, testConfig = testConfig, action = action)

@TestRegistering
fun describe(
    @TestElementName name: String? = null,
    testConfig: TestConfig = TestConfig,
    @TestSuitePropertyName qualifiedPropertyName: String = "",
    content: TestSuite.() -> Unit
): Lazy<TestSuite> =
    testSuite(name = name, testConfig = testConfig, qualifiedPropertyName = qualifiedPropertyName, content = content)
