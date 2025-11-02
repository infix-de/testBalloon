@file:OptIn(TestBalloonInternalApi::class)

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.testScope
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import java.util.Locale
import kotlin.time.Duration.Companion.minutes

val EnvironmentPropagationTests by testSuite(
    testConfig = TestConfig.testScope(isEnabled = true, timeout = 12.minutes)
) {
    val projectName = "environment-propagation"
    val project = TestProject(this, projectName)

    val nativeTargetsThatMayFail = listOf("macosX64Test", "linuxX64Test", "mingwX64Test")

    val testCases = mapOf(
        mapOf<String, String>() to 1,
        mapOf("FROM_PROPERTY" to "yes") to 2,
        mapOf("FROM_EXTENSION" to "yes") to 2,
        mapOf("FROM_PROPERTY" to "yes", "FROM_EXTENSION" to "yes") to 3
    )
    for ((environment, expectedTestCount) in testCases) {
        test("with environment=$environment expect $expectedTestCount test(s)") {
            for (taskName in project.testTaskNames()) {
                val taskExecution = project.gradleExecution(
                    ":clean${taskName.capitalizedTaskName()}",
                    ":$taskName",
                    "-PtestBalloon.environmentVariables=FROM_PROPERTY",
                    environment = mapOf("FROM_PROPERTY" to "yes", "FROM_EXTENSION" to "yes")
                )
                val taskResults = taskExecution.logMessages()
                if (taskName in nativeTargetsThatMayFail && taskExecution.stdout.contains(":$taskName SKIPPED")) {
                    println("$testElementPath: $taskName – SKIPPED")
                } else {
                    val expectedTestCount = 3
                    check(taskResults.size == expectedTestCount) {
                        "$taskName was expected to produce $expectedTestCount result(s)," +
                            " but produced ${taskResults.size}:\n" +
                            "\tactual results:\n${taskResults.asIndentedText(indent = "\t\t")}\n" +
                            taskExecution.stdoutStderr()
                    }
                    println("$testElementPath: $taskName – OK")
                }
            }
        }
    }
}

private fun String.capitalizedTaskName(): String = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
}
