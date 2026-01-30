@file:OptIn(TestBalloonInternalApi::class)

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.core.testScope
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import java.util.Locale
import kotlin.time.Duration.Companion.minutes

val EnvironmentPropagationTests by testSuite(
    testConfig = TestConfig.testScope(isEnabled = true, timeout = 24.minutes)
) {
    val nativeTasksThatMayFail = setOf("macosArm64Test", "iosSimulatorArm64Test", "linuxX64Test", "mingwX64Test")
    val projectTestSuite = this
    val environment = mapOf("TEST_ONE" to "test one", "CUSTOM_ONE" to "custom one", "CUSTOM_TWO" to "custom two")

    fun TestSuiteScope.projectTest(
        projectName: String,
        variantName: String? = null,
        vararg gradleArguments: String,
        expectedVariables: List<String>
    ) = test(listOfNotNull(projectName, variantName).joinToString()) {
        val project = TestProject(projectTestSuite, "environment-propagation-$projectName")

        for (taskName in project.testTaskNames()) {
            val taskExecution = project.gradleExecution(
                ":clean${taskName.capitalizedTaskName()}",
                ":$taskName",
                *gradleArguments,
                environment = environment
            )
            val taskResults = taskExecution.logMessages()
            if (taskName in nativeTasksThatMayFail && taskExecution.stdout.contains(":$taskName SKIPPED")) {
                println("$testElementPath: $taskName – SKIPPED")
            } else {
                check(taskResults == expectedVariables) {
                    "$taskName did not propagate exactly $expectedVariables:\n" +
                        "\tactual results:\n${taskResults.asIndentedText(indent = "\t\t")}\n" +
                        taskExecution.stdoutStderr()
                }
                println("$testElementPath: $taskName – OK")
            }
        }
    }

    projectTest("unrestricted", expectedVariables = listOf("TEST_ONE", "CUSTOM_ONE", "CUSTOM_TWO"))

    projectTest(
        "browser-and-ios-without-extension",
        "no custom variables declared safe",
        expectedVariables = listOf("TEST_ONE") // matched by default pattern
    )
    projectTest(
        "browser-and-ios-without-extension",
        "CUSTOM_ONE declared safe by property",
        "-PtestBalloon.browserSafeEnvironmentPattern=CUSTOM_ONE",
        "-PtestBalloon.simulatorSafeEnvironmentPattern=CUSTOM_ONE",
        expectedVariables = listOf("CUSTOM_ONE") // matched by property
    )
    projectTest(
        "browser-and-ios-with-extension",
        "CUSTOM_TWO declared safe by extension",
        "-PtestBalloon.browserSafeEnvironmentPattern=CUSTOM_ONE",
        "-PtestBalloon.simulatorSafeEnvironmentPattern=CUSTOM_ONE",
        expectedVariables = listOf("CUSTOM_TWO") // matched by extension, overrides property
    )
}

private fun String.capitalizedTaskName(): String = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
}
