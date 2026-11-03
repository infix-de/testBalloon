@file:OptIn(TestBalloonInternalApi::class)

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.disable
import de.infix.testBalloon.framework.core.invocation
import de.infix.testBalloon.framework.core.testScope
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.TestRegistering
import de.infix.testBalloon.framework.shared.internal.Constants.INTERNAL_PATH_ELEMENT_SEPARATOR
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import java.util.Locale
import kotlin.time.Duration.Companion.minutes

val ElementSelectionTests by testSuite(
    testConfig = TestConfig
        .invocation(TestConfig.Invocation.Sequential)
        .testScope(isEnabled = true, timeout = 24.minutes)
) {
    projectTestSuite("element-selection", listOf("base-google"), withExtraPatterns = true)
    projectTestSuite(
        "element-selection-new-browser-dsl",
        listOf("base"),
        withExtraPatterns = false
    )
}

@TestRegistering
private fun TestSuite.projectTestSuite(
    projectName: String,
    baseTemplates: List<String>,
    withExtraPatterns: Boolean,
    testConfig: TestConfig = TestConfig
) = testSuite(projectName, testConfig = testConfig) {
    val project =
        TestProject(projectTestSuite = this, projectBaseName = projectName, baseTemplates = baseTemplates)

    if (packageLockFilesUpdateRequested()) {
        test("update package lock files") {
            project.testTaskNames()
        }
        return@testSuite
    }

    class TestVariant(
        val configurationType: VariantConfigurationType,
        val extraName: String? = null,
        val execution: suspend (taskName: String, pattern: String) -> TestProject.Execution,
        val patternMatches: Map<String, Int>
    )

    val commonPatternMatches = mapOf("com.example.SimpleSuite${INTERNAL_PATH_ELEMENT_SEPARATOR}test 1" to 1)
    val extraPatternMatches = if (withExtraPatterns) {
        mapOf(
            "com.example.SimpleSuite*test 1" to 1,
            ";com.example.SimpleSuite;test 1" to 1,
            "NoMatch" to 0,
            "com.example.SpecialNameSuite${INTERNAL_PATH_ELEMENT_SEPARATOR}test 1" to 1
        )
    } else {
        emptyMap()
    }
    val exclusionPatternMatches = mapOf("com.example.SimpleSuite*" to 2)

    val primaryVariants = listOf(
        TestVariant(
            configurationType = VariantConfigurationType.CLI,
            execution = { taskName, pattern ->
                project.gradleExecution(
                    ":clean${taskName.capitalizedTaskName()}",
                    ":$taskName",
                    "--tests",
                    pattern
                )
            },
            patternMatches = commonPatternMatches + extraPatternMatches
        ),
        TestVariant(
            configurationType = VariantConfigurationType.Environment,
            execution = { taskName, pattern ->
                project.gradleExecution(
                    ":clean${taskName.capitalizedTaskName()}",
                    ":$taskName",
                    environment = mapOf("TESTBALLOON_INCLUDE_PATTERNS" to pattern)
                )
            },
            patternMatches = commonPatternMatches
        ),
        TestVariant(
            configurationType = VariantConfigurationType.Buildscript,
            execution = { taskName, pattern ->
                project.gradleExecution(
                    ":clean${taskName.capitalizedTaskName()}",
                    ":$taskName",
                    "-Plocal.includeTestsMatching=$pattern"
                )
            },
            patternMatches = commonPatternMatches
        ),
        TestVariant(
            configurationType = VariantConfigurationType.Environment,
            extraName = "(exclusion)",
            execution = { taskName, pattern ->
                project.gradleExecution(
                    ":clean${taskName.capitalizedTaskName()}",
                    ":$taskName",
                    environment = mapOf("TESTBALLOON_EXCLUDE_PATTERNS" to pattern)
                )
            },
            patternMatches = exclusionPatternMatches
        ),
        TestVariant(
            configurationType = VariantConfigurationType.Buildscript,
            extraName = "(exclusion)",
            execution = { taskName, pattern ->
                project.gradleExecution(
                    ":clean${taskName.capitalizedTaskName()}",
                    ":$taskName",
                    "-Plocal.excludeTestsMatching=$pattern"
                )
            },
            patternMatches = exclusionPatternMatches
        )
    )

    val secondaryVariants = listOf(
        TestVariant(
            configurationType = VariantConfigurationType.CLI,
            execution = { taskName, pattern ->
                project.gradleExecution(
                    ":clean${taskName.capitalizedTaskName()}",
                    ":$taskName",
                    "--tests",
                    pattern
                )
            },
            patternMatches = commonPatternMatches
        )
    )

    val variants = if (secondarySkippingEnabled) secondaryVariants else primaryVariants

    for (variant in variants) {
        for ((pattern, expectedTestCount) in variant.patternMatches) {
            val name = variant.configurationType.name + (variant.extraName?.let { " $it" } ?: "")
            test(
                "$name: '$pattern', $expectedTestCount test(s)",
                testConfig = TestConfig.skipConditionally(pattern)
            ) {
                for (taskName in project.testTaskNames()) {
                    val taskExecution = variant.execution(taskName, pattern)

                    if (expectedTestCount == 0 &&
                        taskExecution.exitCode != 0 &&
                        (
                            taskExecution.stderr.contains("No tests found for given includes") ||
                                variant.configurationType == VariantConfigurationType.Environment &&
                                taskExecution.stderr.contains(
                                    "There are test sources present and no filters are applied"
                                )
                            )
                    ) {
                        println("$testElementPath: $taskName – OK (NO MATCH)")
                        continue
                    }

                    if (taskExecution.nativeTaskHasFailedExpectedly(taskName)) {
                        println("$testElementPath: $taskName – SKIPPED")
                        continue
                    }

                    val taskResults = taskExecution.logMessages()
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

private enum class VariantConfigurationType { CLI, Environment, Buildscript }

private val secondarySkippingEnabled = skippingEnabled("secondary")

private val nonAsciiPatternSkippingEnabled = skippingEnabled("non-ASCII patterns")

private fun TestConfig.skipConditionally(pattern: String) =
    if (nonAsciiPatternSkippingEnabled && pattern.any { it.code !in 32..126 }) disable() else this

private fun String.capitalizedTaskName(): String = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
}
