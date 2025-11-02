@file:OptIn(TestBalloonInternalApi::class)

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.testScope
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.internal.Constants.INTERNAL_PATH_ELEMENT_SEPARATOR
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import java.util.Locale
import kotlin.time.Duration.Companion.minutes

val ElementSelectionTests by testSuite(testConfig = TestConfig.testScope(isEnabled = true, timeout = 12.minutes)) {
    val projectName = "element-selection"
    val project = TestProject(this, projectName)

    class TestVariant(
        val type: String,
        val execution: suspend (taskName: String, pattern: String) -> TestProject.Execution
    )

    val variants = listOf(
        TestVariant(
            type = "CLI",
            execution = { taskName, pattern ->
                project.gradleExecution(
                    ":clean${taskName.capitalizedTaskName()}",
                    ":$taskName",
                    "--tests",
                    pattern
                )
            }
        ),
        TestVariant(
            type = "Environment",
            execution = { taskName, pattern ->
                project.gradleExecution(
                    ":clean${taskName.capitalizedTaskName()}",
                    ":$taskName",
                    environment = mapOf("TESTBALLOON_INCLUDE_PATTERNS" to pattern)
                )
            }
        ),
        TestVariant(
            type = "Buildscript",
            execution = { taskName, pattern ->
                project.gradleExecution(
                    ":clean${taskName.capitalizedTaskName()}",
                    ":$taskName",
                    "-Plocal.includeTestsMatching=$pattern"
                )
            }
        )
    )

    val nativeTargetsThatMayFail = listOf("macosX64Test", "linuxX64Test", "mingwX64Test")

    for ((pattern, expectedTestCount) in mapOf(
        "com.example.SimpleSuite${INTERNAL_PATH_ELEMENT_SEPARATOR}test 1" to 1,
        "com.example.SimpleSuite*test 1" to 1,
        "com.example.SimpleSuite|test 1" to 1,
        ";com.example.SimpleSuite;test 1" to 1,
        "NoMatch" to 0
    )) {
        for (variant in variants) {
            test("${variant.type}: '$pattern', $expectedTestCount test(s)") {
                for (taskName in project.testTaskNames()) {
                    val taskExecution = variant.execution(taskName, pattern)
                    val taskResults = taskExecution.logMessages()
                    if (taskName in nativeTargetsThatMayFail && taskExecution.stdout.contains(":$taskName SKIPPED")) {
                        println("$testElementPath: $taskName – SKIPPED")
                    } else {
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
}

private fun String.capitalizedTaskName(): String = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
}
