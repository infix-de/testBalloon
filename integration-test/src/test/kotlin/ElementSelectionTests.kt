@file:OptIn(TestBalloonInternalApi::class)

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.disable
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
        val type: VariantType,
        val execution: suspend (taskName: String, pattern: String) -> TestProject.Execution
    )

    val variants = listOf(
        TestVariant(
            type = VariantType.CLI,
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
            type = VariantType.Environment,
            execution = { taskName, pattern ->
                project.gradleExecution(
                    ":clean${taskName.capitalizedTaskName()}",
                    ":$taskName",
                    environment = mapOf("TESTBALLOON_INCLUDE_PATTERNS" to pattern)
                )
            }
        ),
        TestVariant(
            type = VariantType.Buildscript,
            execution = { taskName, pattern ->
                project.gradleExecution(
                    ":clean${taskName.capitalizedTaskName()}",
                    ":$taskName",
                    "-Plocal.includeTestsMatching=$pattern"
                )
            }
        )
    )

    val nativeTargetsThatMayFail = listOf("macosArm64Test", "linuxX64Test", "mingwX64Test")

    for ((pattern, expectedTestCount) in mapOf(
        "com.example.SimpleSuite${INTERNAL_PATH_ELEMENT_SEPARATOR}test 1" to 1,
        "com.example.SimpleSuite*test 1" to 1,
        "com.example.SimpleSuite|test 1" to 1,
        ";com.example.SimpleSuite;test 1" to 1,
        "NoMatch" to 0
    )) {
        for (variant in variants) {
            test(
                "${variant.type.name}: '$pattern', $expectedTestCount test(s)",
                testConfig = TestConfig.skipConditionally(pattern)
            ) {
                for (taskName in project.testTaskNames()) {
                    val taskExecution = variant.execution(taskName, pattern)

                    if (expectedTestCount == 0 &&
                        taskExecution.exitCode != 0 &&
                        (
                            taskExecution.stderr.contains("No tests found for given includes") ||
                                variant.type == VariantType.Environment &&
                                taskExecution.stderr.contains(
                                    "There are test sources present and no filters are applied"
                                )
                            )
                    ) {
                        println("$testElementPath: $taskName – OK (NO MATCH)")
                        continue
                    }

                    val taskResults = taskExecution.logMessages()
                    if (taskName in nativeTargetsThatMayFail && taskExecution.stdout.contains(":$taskName SKIPPED")) {
                        println("$testElementPath: $taskName – SKIPPED")
                        continue
                    }

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

private enum class VariantType { CLI, Environment, Buildscript }

private val nonAsciiPatternSkippingEnabled = skippingEnabled("non-ASCII patterns")

private fun TestConfig.skipConditionally(pattern: String) =
    if (nonAsciiPatternSkippingEnabled && pattern.any { it.code !in 32..126 }) disable() else this

private fun String.capitalizedTaskName(): String = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
}
