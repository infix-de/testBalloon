import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.disable
import de.infix.testBalloon.framework.core.testScope
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.AbstractTestElement
import de.infix.testBalloon.framework.shared.TestDiscoverable
import kotlin.io.path.div
import kotlin.io.path.moveTo
import kotlin.time.Duration.Companion.minutes

val IncrementalCompilationTests by testSuite(
    testConfig = TestConfig.testScope(isEnabled = true, timeout = 12.minutes)
) {
    incrementalCompilationTestSuite(
        "incremental-compilation-kotlin-test",
        testConfig = TestConfig.disable() // enable to observe IC with kotlin-test
    ) {
        testSeries("incremental compilation")
    }

    incrementalCompilationTestSuite("incremental-compilation-testBalloon") {
        testSeries(
            name = "full compilation",
            gradleOptions = arrayOf(
                "-Pkotlin.incremental=false",
                "-Pkotlin.incremental.js=false",
                "-Pkotlin.incremental.js.klib=false",
                "-Pkotlin.incremental.js.ir=false"
            ),
            testConfig = TestConfig.disable() // enable to observe behavior with full compilation
        )

        testSeries("incremental compilation")
    }
}

@TestDiscoverable
private fun TestSuite.incrementalCompilationTestSuite(
    projectName: String,
    testConfig: TestConfig = TestConfig,
    action: IncrementalCompilationTestProject.() -> Unit
) = testSuite(projectName, displayName = "project: $projectName", testConfig) {
    IncrementalCompilationTestProject(this, projectName).action()
}

private class IncrementalCompilationTestProject(private val projectTestSuite: TestSuite, projectName: String) :
    TestProject(projectTestSuite, projectName) {

    /**
     * A series of tests which repeatedly executes a Gradle test task for all available targets.
     *
     * The test series starts with a clean project containing a set of source files. The first task execution
     * produces a baseline set of test results. Then, incremental compilation is challenged by
     * - removing the first file,
     * - executing the Gradle test task (one per target),
     * - restoring the first file,
     * - executing the Gradle test task (one per target).
     * Incremental compilation is correct if the test results match the set of files present at each stage.
     */
    @TestDiscoverable
    fun testSeries(name: String, gradleOptions: Array<String> = arrayOf(), testConfig: TestConfig = TestConfig) =
        projectTestSuite.testSuite(name, testConfig = testConfig) {
            suspend fun compileTaskExecution(taskName: String) = gradleExecution(taskName, *gradleOptions)

            val commonTestSourceDirectory = testFixture { projectDirectory() / "src" / "commonTest" }
            val enabledSourcesDirectory = testFixture { commonTestSourceDirectory() / "kotlin" }
            val disabledSourcesDirectory = testFixture { commonTestSourceDirectory() / "disabled" }

            val baselineResults = testFixture {
                val fileCount = 2
                val nativeTargetsThatMayFail = listOf("macosX64", "linuxX64", "mingwX64")

                testTaskNames().mapNotNull { taskName ->
                    val taskExecution = compileTaskExecution(taskName)
                    val targetName = taskName.removeSuffix("Test")

                    if (targetName in nativeTargetsThatMayFail &&
                        (
                            taskExecution.stdout.contains(":$taskName SKIPPED") ||
                                taskExecution.stderr.contains(
                                    "Could not resolve all artifacts for configuration ':$targetName"
                                )
                            )
                    ) {
                        return@mapNotNull null
                    }

                    val taskResults = taskExecution.logMessages()
                    check(taskResults.size == fileCount) {
                        "$taskName was expected to produce $fileCount results, but produced ${taskResults.size}:\n" +
                            "\tactual results:\n${taskResults.asIndentedText(indent = "\t\t")}\n" +
                            taskExecution.stdoutStderr()
                    }

                    taskName to taskResults
                }.toMap()
            }

            test("baseline") {
                check(baselineResults().isNotEmpty()) {
                    "None of the tasks ${testTaskNames()} produced a result."
                }
            }

            suspend fun AbstractTestElement.verifyResults(taskName: String, exceptIndex: Int? = null) {
                val baselineResult = baselineResults()[taskName] ?: return
                val expectedResults = baselineResult.filterIndexed { index, _ ->
                    index != exceptIndex
                }
                val taskExecution = compileTaskExecution(taskName)
                val actualResults = taskExecution.logMessages()
                if (actualResults != expectedResults) {
                    throw AssertionError(
                        "$taskName: Results do not meet expectations.\n" +
                            "\tExpected: $expectedResults\n" +
                            "\tActual: ${actualResults}\n" + taskExecution.stdoutStderr()
                    )
                }
                println("$testElementPath: $taskName – OK")
            }

            val firstFileIndex = 0
            val firstFileName = "File1.kt"

            test("remove $firstFileName") {
                (enabledSourcesDirectory() / firstFileName).moveTo(disabledSourcesDirectory() / firstFileName)
                for (taskName in testTaskNames()) {
                    verifyResults(taskName, firstFileIndex)
                }
            }

            test("restore $firstFileName") {
                (disabledSourcesDirectory() / firstFileName).moveTo(enabledSourcesDirectory() / firstFileName)
                for (taskName in testTaskNames()) {
                    verifyResults(taskName)
                }
            }
        }
}
