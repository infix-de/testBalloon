package de.infix.testBalloon.framework.internal

import de.infix.testBalloon.framework.AbstractTestSuite
import de.infix.testBalloon.framework.InvokedByGeneratedCode
import de.infix.testBalloon.framework.TestSession
import de.infix.testBalloon.framework.internal.integration.TeamCityTestExecutionReport
import de.infix.testBalloon.framework.internal.integration.ThrowingTestConfigurationReport
import de.infix.testBalloon.framework.internal.integration.kotlinJsTestFrameworkAvailable
import de.infix.testBalloon.framework.internal.integration.processArguments
import de.infix.testBalloon.framework.internal.integration.registerWithKotlinJsTestFramework

@InvokedByGeneratedCode
internal actual suspend fun configureAndExecuteTests(suites: Array<AbstractTestSuite>) {
    // `suites` is unused because test suites register themselves with `TestSession`.

    configureTestsWithExceptionHandling {
        if (argumentsBasedElementSelection == null) {
            argumentsBasedElementSelection = processArguments()?.let { ArgumentsBasedElementSelection(it) }
        }
        TestSession.global.parameterize(ThrowingTestConfigurationReport())
    }.onSuccess {
        executeTestsWithExceptionHandling {
            if (kotlinJsTestFrameworkAvailable()) {
                TestSession.global.registerWithKotlinJsTestFramework()
            } else {
                TestSession.global.execute(TeamCityTestExecutionReport())
            }
        }
    }
}
