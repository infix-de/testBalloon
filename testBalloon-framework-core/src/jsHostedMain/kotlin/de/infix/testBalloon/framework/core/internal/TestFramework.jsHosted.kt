package de.infix.testBalloon.framework.core.internal

import de.infix.testBalloon.framework.core.TestSession
import de.infix.testBalloon.framework.core.internal.integration.TeamCityTestExecutionReport
import de.infix.testBalloon.framework.core.internal.integration.ThrowingTestSetupReport
import de.infix.testBalloon.framework.core.internal.integration.kotlinJsTestFrameworkAvailable
import de.infix.testBalloon.framework.core.internal.integration.processArguments
import de.infix.testBalloon.framework.core.internal.integration.registerWithKotlinJsTestFramework
import de.infix.testBalloon.framework.shared.AbstractTestSuite
import de.infix.testBalloon.framework.shared.internal.InvokedByGeneratedCode

@InvokedByGeneratedCode
internal actual suspend fun setUpAndExecuteTests(suites: Array<AbstractTestSuite>) {
    // `suites` is unused because test suites register themselves with `TestSession`.

    configureTestsWithExceptionHandling {
        if (argumentsBasedElementSelection == null) {
            argumentsBasedElementSelection =
                processArguments()?.let { ArgumentsBasedElementSelection(it) }
                    ?: testConfigBasedElementSelection()
        }
        TestSession.global.setUp(ThrowingTestSetupReport())
    }.onSuccess {
        executeTestsWithExceptionHandling {
            if (kotlinJsTestFrameworkAvailable()) {
                TestSession.global.registerWithKotlinJsTestFramework()
            } else {
                TestSession.global.execute(TeamCityTestExecutionReport())
                outputTestsFinishedMarkerIfApplicable()
            }
        }
    }
}

internal expect fun outputTestsFinishedMarkerIfApplicable()

internal expect fun kotlinTestConfigIncludePatterns(): List<String>?

internal expect fun kotlinTestConfigExcludePatterns(): List<String>?

private class TestConfigBasedElementSelection(includePatterns: List<String>, excludePatterns: List<String>) :
    ListsBasedElementSelection(includePatterns = includePatterns, excludePatterns = excludePatterns)

private fun testConfigBasedElementSelection(): TestConfigBasedElementSelection? {
    val includePatterns = kotlinTestConfigIncludePatterns() ?: return null
    val excludePatterns = kotlinTestConfigExcludePatterns() ?: return null
    if (includePatterns.isEmpty() && excludePatterns.isEmpty()) return null
    return TestConfigBasedElementSelection(includePatterns, excludePatterns)
}
