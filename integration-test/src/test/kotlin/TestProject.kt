import de.infix.testBalloon.framework.core.TestBalloonExperimentalApi
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.core.testPlatform
import kotlinx.datetime.Clock
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.pathString

/**
 * A test project is a Gradle project providing tests listed by a Gradle task `listTests`.
 *
 * The test project is created from files in the `build/projectTemplates` directory, specifically the
 * subdirectories `common` and [projectName].
 * The test project autoconfigures itself for the available test tasks, preparing a clean build with
 * a fresh set of JS and Wasm package lock files (if JS and/or Wasm tests are available).
 */
internal open class TestProject(projectTestSuite: TestSuite, projectName: String) : TestSuiteScope {

    override val testSuiteInScope: TestSuite = projectTestSuite

    @OptIn(ExperimentalPathApi::class)
    protected val projectDirectory = testFixture {
        val commonTemplateDirectory = Path("build") / "projectTemplates" / "common"
        val projectTemplateDirectory = Path("build") / "projectTemplates" / projectName
        val projectDirectory = Path("build") / "projects" / projectName

        log("Setting up $projectDirectory from $commonTemplateDirectory, $projectTemplateDirectory")
        if (projectDirectory.exists()) projectDirectory.deleteRecursively()
        projectDirectory.createDirectories()
        commonTemplateDirectory.copyToRecursively(projectDirectory, followLinks = false, overwrite = false)
        projectTemplateDirectory.copyToRecursively(projectDirectory, followLinks = false, overwrite = false)

        projectDirectory
    }

    internal val testTaskNames = testFixture {
        val listTestsResultRegex = Regex("""##TEST\((.*?)\)##""")

        val testTaskNames = gradleExecution("listTests").checkedStdout().let { stdout ->
            listTestsResultRegex.findAll(stdout).mapNotNull { it.groups[1]?.value }
        }.toList()

        // Prepare the project for execution.
        val npmPackageLockTasks =
            buildList {
                if (testTaskNames.any { it.startsWith("js") }) add("kotlinUpgradePackageLock")
            }.toTypedArray()
        gradleExecution("clean", *npmPackageLockTasks).checked()

        testTaskNames
    }

    internal suspend fun gradleExecution(
        vararg arguments: String,
        environment: Map<String, String> = emptyMap()
    ): Execution = execution(
        (projectDirectory() / (if (runsOnWindows) "gradlew.bat" else "gradlew")).pathString,
        "-p",
        projectDirectory().pathString,
        *arguments,
        environment = environment
    )

    private val runsOnWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

    private fun execution(vararg arguments: String, environment: Map<String, String> = emptyMap()): Execution {
        val process = ProcessBuilder(*arguments).also { processBuilder ->
            processBuilder.environment().run {
                keys.filter { it.startsWith("TEST") }.forEach {
                    remove(it)
                }
                for ((key, value) in environment) {
                    this[key] = value
                }
            }
        }.start()

        val stdout = process.inputStream.readAllBytes().toString(Charsets.UTF_8).trim()
        val stderr = process.errorStream.readAllBytes().toString(Charsets.UTF_8).trim()
        val exitCode = process.waitFor()

        return Execution(arguments.toList(), exitCode, stdout, stderr).run {
            log("Execution ${this.arguments} returned exit code $exitCode\n${stdoutStderr("\t")}")
            this
        }
    }

    internal data class Execution(
        val arguments: List<String>,
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    ) {
        private val logMessageRegex = Regex("""##LOG\((.*?)\)LOG##""")

        fun logMessages(): List<String> = logMessageRegex.findAll(checkedStdout()).mapNotNull {
            it.groups[1]?.value
        }.toList()

        fun stdoutStderr(indent: String = "\t") = buildString {
            appendLine("$indent--- stdout ---")
            appendLine(stdout.prependIndent("$indent\t"))
            appendLine("$indent--- stderr ---")
            appendLine(stderr.prependIndent("$indent\t"))
        }

        fun checked(): Execution {
            check(exitCode == 0) {
                "Execution $arguments failed with exit code $exitCode\n" + stdoutStderr("\t")
            }
            return this
        }

        fun checkedStdout(): String = checked().stdout
    }
}

private val LOG_ENABLED = true
private val logDirectory = (Path("build") / "reports").also { it.toFile().mkdirs() }
private val logFile = (logDirectory / "TestProject.log").toFile()
private val logInitialized = AtomicBoolean(false)

private fun log(message: String) {
    @Suppress("KotlinConstantConditions")
    if (!LOG_ENABLED) return

    if (!logInitialized.getAndSet(true)) {
        logFile.appendText("\n––– Session Starting –––\n")
    }

    @OptIn(TestBalloonExperimentalApi::class)
    logFile.appendText("${Clock.System.now()} [${testPlatform.threadId()}] $message\n")
}

internal fun List<String>.asIndentedText(indent: String = "\t") = joinToString(prefix = indent, separator = "\n$indent")

internal fun skippingEnabled(key: String) =
    testPlatform.environment("TEST_SKIP")?.split(',')?.any { it.trim().contains(key) } == true
