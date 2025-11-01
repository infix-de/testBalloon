package de.infix.testBalloon.framework.core

import de.infix.testBalloon.framework.shared.TestRegistering
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.test.runTest
import org.junit.rules.TestRule
import org.junit.runners.model.Statement

/**
 * Registers a [Test] wrapped by a JUnit 4 [TestRule].
 */
@TestRegistering
@TestBalloonExperimentalApi
public fun <Rule : TestRule> TestSuite.testWithJUnit4Rule(name: String, rule: Rule, action: suspend (Rule) -> Unit) {
    test(name) {
        rule.apply(
            object : Statement() {
                override fun evaluate() {
                    runTest(coroutineContext.minusKey(CoroutineExceptionHandler.Key)) {
                        action(rule)
                    }
                }
            },
            jUnit4Description
        ).evaluate()
    }
}
