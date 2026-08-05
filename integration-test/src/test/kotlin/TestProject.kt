import buildConfig.BuildConfig
import de.infix.testBalloon.framework.core.TestBalloonExperimentalApi
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.core.testPlatform
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.visitFileTree
import kotlin.io.path.writeText
import kotlin.time.Clock

/**
 * A test project is a Gradle project providing tests listed by a Gradle task `listTests`.
 *
 * The test project is created from files in the `build/projectTemplates` directory, specifically the
 * subdirectories `common` and [projectBaseName]. `{{version:NAME}` placeholders in template files are populated with
 * a corresponding `NAME` entry from [versions], if present, otherwise with the main project's version catalog entry.
 *
 * The test project autoconfigures itself for the available test tasks, preparing a clean build with
 * a fresh set of JS and Wasm package lock files (if JS and/or Wasm tests are available).
 */
@OptIn(ExperimentalPathApi::class)
internal open class TestProject(
    projectTestSuite: TestSuite,
    projectBaseName: String,
    projectVariantName: String = "",
    val versions: Map<String, String> = emptyMap()
) : TestSuiteScope {

    override val testSuiteInScope: TestSuite = projectTestSuite

    val projectName = projectBaseName + projectVariantName
    val templatesBaseDirectory = Path("projectTemplates")
    val templateVariantDirectory = templatesBaseDirectory / projectName

    protected val projectDirectory = testFixture {
        val projectDirectory = Path("build") / "projects" / projectName

        projectDirectory.populate(
            templatesBaseDirectory / "common",
            templatesBaseDirectory / projectBaseName,
            templateVariantDirectory
        )

        projectDirectory
    }

    private fun Path.populate(vararg templateDirectories: Path) {
        val templateDirectories = templateDirectories.filter { it.exists() }
        log("Populating $this from $templateDirectories")

        if (exists()) deleteRecursively()
        createDirectories()

        // Use the root project's Gradle setup as a default.
        val rootDirectory = BuildConfig.PROJECT_ROOT_DIRECTORY.toPath()
        for (rootFile in (listOf("gradlew", "gradlew.bat", "gradle"))) {
            (rootDirectory / rootFile).copyToRecursively(this / rootFile, followLinks = false, overwrite = false)
        }

        for (templateDirectory in templateDirectories) {
            populateFromTemplate(templateDirectory)
        }
    }

    private fun Path.populateFromTemplate(source: Path) {
        val target = this

        source.visitFileTree {
            onPreVisitDirectory { path, _ ->
                (target / path.relativeTo(source)).apply {
                    if (!exists()) createDirectory()
                }
                FileVisitResult.CONTINUE
            }

            onVisitFile { path, _ ->
                val targetContent = path.readText().replace(parameterRegex) { matchResult ->
                    val (protocol, name) = matchResult.groupValues[1].split(':')
                    when (protocol) {
                        "version" -> when (name) {
                            "de.infix.testBalloon" -> BuildConfig.PROJECT_VERSION

                            else -> {
                                versions[name]
                                    ?: projectVersions[name]
                                    ?: throw IllegalArgumentException("Version for '$name' not found")
                            }
                        }

                        "path" -> when (name) {
                            "integration-test-repository" -> BuildConfig.PROJECT_INTEGRATION_TEST_REPOSITORY
                            else -> throw IllegalArgumentException("Unknown path name '$name'")
                        }

                        else -> matchResult.value
                    }
                }
                (target / path.relativeTo(source)).writeText(targetContent)
                FileVisitResult.CONTINUE
            }
        }
    }

    private val parameterRegex = Regex("""\{\{(.*?)\}\}""")
    private val projectVersions = BuildConfig.PROJECT_CATALOG_VERSIONS

    internal val testTaskNames = testFixture {
        val listTestsResultRegex = Regex("""##TEST\((.*?)\)##""")

        val testTaskNames = gradleExecution("listTests").checkedStdout().let { stdout ->
            listTestsResultRegex.findAll(stdout).mapNotNull { it.groups[1]?.value }
        }.toList()

        // Prepare the project for execution.
        gradleExecution("clean").checked()

        val npmPackageLockTasks =
            buildList {
                if (testTaskNames.any { it.startsWith("js") }) add("kotlinUpgradePackageLock")
                if (testTaskNames.any { it.startsWith("wasmJs") }) add("kotlinWasmUpgradePackageLock")
            }.toTypedArray()
        val jsPackageLockFile = projectDirectory() / "kotlin-js-store" / "package-lock.json"
        if (npmPackageLockTasks.isNotEmpty() && (!jsPackageLockFile.exists() || packageLockFilesUpdateRequested())) {
            // Create Npm package lock files and copy them to the respective project template directory.
            gradleExecution(*npmPackageLockTasks).checked()
            templateVariantDirectory.takeIf { it.notExists() }?.createDirectory()
            (templateVariantDirectory / "kotlin-js-store").takeIf { it.notExists() }?.createDirectory()
            (projectDirectory() / "kotlin-js-store").copyToRecursively(
                templateVariantDirectory / "kotlin-js-store",
                followLinks = false,
                overwrite = true
            )
        }

        if (packageLockFilesUpdateRequested()) return@testFixture emptyList()

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

@Suppress("MayBeConstant", "RedundantSuppression")
private val LOG_ENABLED = true
private val logDirectory = (Path("build") / "reports").also { it.toFile().mkdirs() }
private val logFile = (logDirectory / "TestProject.log").toFile()
private val logInitialized = AtomicBoolean(false)

private fun log(message: String) {
    @Suppress("KotlinConstantConditions", "RedundantSuppression")
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

fun packageLockFilesUpdateRequested(): Boolean = testPlatform.environment("PACKAGE_LOCK_FILES_UPDATE_REQUESTED") != null
