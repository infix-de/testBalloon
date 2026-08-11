import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.disable
import de.infix.testBalloon.framework.core.invocation
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.core.testScope
import de.infix.testBalloon.framework.core.testSuite
import kotlin.time.Duration.Companion.minutes

private val latestAgpVersion = projectCatalogVersion("android.gradle.plugin.latest")
private val latestKotlinVersion = projectCatalogVersion("org.jetbrains.kotlin.latest")
private val earliestAgpVersion = projectCatalogVersion("android.gradle.plugin")
private val earliestKotlinVersion = projectCatalogVersion("org.jetbrains.kotlin")

val AndroidTests by testSuite(
    testConfig = TestConfig
        .disableIfAndroidSdkIsUnavailable()
        .invocation(TestConfig.Invocation.Sequential)
        .testScope(isEnabled = true, timeout = 24.minutes)
) {
    testSuite("Android App") {
        for ((agpVersion, kotlinVersion) in listOf(
            latestAgpVersion to null,
            latestAgpVersion to earliestKotlinVersion,
            earliestAgpVersion to latestKotlinVersion,
            earliestAgpVersion to earliestKotlinVersion
        )) {
            test("android-app", kotlinVersion, agpVersion)
        }
    }

    testSuite("KMP with Android Library") {
        for (agpVersion in listOf(latestAgpVersion, earliestAgpVersion)) {
            for (kotlinVersion in listOf(latestKotlinVersion, earliestKotlinVersion)) {
                test("android-kmp-library", kotlinVersion, agpVersion)
            }
        }
    }
}

private fun TestSuite.test(projectBaseName: String, kotlinVersion: String?, agpVersion: String) {
    val project =
        TestProject(
            projectTestSuite = this,
            projectBaseName = projectBaseName,
            projectVariantName = if (kotlinVersion == null) "-A$agpVersion" else "-A$agpVersion-K$kotlinVersion",
            versions = mapOf("org.jetbrains.kotlin" to (kotlinVersion ?: ""), "android.gradle.plugin" to agpVersion)
        )

    test("AGP $agpVersion, Kotlin ${kotlinVersion ?: "default"}") {
        for (taskName in project.testTaskNames()) {
            val taskExecution = project.gradleExecution(":$taskName")

            if (taskExecution.nativeTaskHasFailedExpectedly(taskName)) {
                println("$testElementPath: $taskName – SKIPPED")
                continue
            }

            if (taskName.startsWith("pixel2")) {
                check(taskExecution.checkedStdout().contains("Finished 1 tests on pixel2")) {
                    "$taskName did not produce 'Finished 1 tests on pixel2':\n" +
                        taskExecution.stdoutStderr()
                }
            } else {
                val taskResults = taskExecution.logMessages()
                check(taskResults.size == 1) {
                    "$taskName was expected to produce 1 result, but produced ${taskResults.size}:\n" +
                        "\tactual results:\n${taskResults.asIndentedText(indent = "\t\t")}\n" +
                        taskExecution.stdoutStderr()
                }
            }
            println("$testElementPath: $taskName – OK")
        }
    }
}

private fun TestConfig.disableIfAndroidSdkIsUnavailable() =
    if (testPlatform.environment("ANDROID_HOME") == null) disable() else this
