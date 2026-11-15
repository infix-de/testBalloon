import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.gradle.BuildScript
import com.autonomousapps.kit.gradle.Imports
import com.autonomousapps.kit.gradle.Plugin
import de.infix.testBalloon.BuildConfig.PROJECT_COMPILER_PLUGIN_ID
import de.infix.testBalloon.BuildConfig.PROJECT_FRAMEWORK_CORE_ARTIFACT_ID
import de.infix.testBalloon.BuildConfig.PROJECT_ROOT_GROUP
import de.infix.testBalloon.BuildConfig.PROJECT_VERSION
import org.gradle.testkit.runner.BuildResult
import org.jetbrains.kotlin.compiler.plugin.devkit.test.AbstractDevKitGradleProject
import org.jetbrains.kotlin.compiler.plugin.devkit.test.KotlinPlugins
import org.jetbrains.kotlin.compiler.plugin.devkit.test.pluginUnderTestVersion
import org.jetbrains.kotlin.compiler.plugin.devkit.test.source

fun withGradleRun(
    testSourceFileContent: String,
    gradleArguments: List<String> = listOf("compileTestKotlin"),
    frameworkDependencyEnabled: Boolean = true,
    debugLevel: String? = null,
    expectSuccess: Boolean = true,
    action: GradleRunResult.() -> Unit
) {
    val gradleProject = GradleProject(
        testSourceFileContent = testSourceFileContent.trimIndent(),
        frameworkDependencyEnabled = frameworkDependencyEnabled,
        debugLevel = debugLevel
    ).gradleProject

    val buildResult: BuildResult = if (expectSuccess) {
        GradleBuilder.build(gradleProject.rootDir, args = gradleArguments.toTypedArray())
    } else {
        GradleBuilder.buildAndFail(gradleProject.rootDir, args = gradleArguments.toTypedArray())
    }

    GradleRunResult(buildResult).action()

    gradleProject.rootDir.deleteRecursively() // clean up after a successful test
}

class GradleRunResult(base: BuildResult) : BuildResult by base {
    /**
     * Gradle output, with normalized (OS-independent) line endings.
     */
    val normalizedOutput: String get() = output.lines().joinToString("\n")
}

private class GradleProject(
    private val testSourceFileContent: String,
    private val frameworkDependencyEnabled: Boolean,
    private val debugLevel: String?
) : AbstractDevKitGradleProject() {
    override val pluginUnderTest = Plugin(PROJECT_COMPILER_PLUGIN_ID, pluginUnderTestVersion)

    override fun sources() = listOf(source(testSourceFileContent, sourceSet = "test"))

    override fun BuildScript.Builder.applyDefaultBuildScript() {
        if (debugLevel != null) {
            imports = Imports.of("de.infix.testBalloon.framework.shared.internal.DebugLevel")
        }

        val kotlinPlugin = KotlinPlugins.jvm(kotlinVersion)

        plugins(kotlinPlugin, pluginUnderTest)

        withKotlin(
            buildString {
                if (multiplatform) append(multiplatformTargetsBlock())
                if (frameworkDependencyEnabled) {
                    val coreDependency = "$PROJECT_ROOT_GROUP:$PROJECT_FRAMEWORK_CORE_ARTIFACT_ID:$PROJECT_VERSION"
                    appendLine("dependencies {")
                    appendLine("""testImplementation("$coreDependency")""")
                    appendLine("}")
                }
                if (debugLevel != null) {
                    appendLine("testBalloon { debugLevel = DebugLevel.$debugLevel }")
                }
                appendLine("tasks.withType<Test>().configureEach {")
                appendLine("testLogging { showStandardStreams = true }")
                appendLine("}")
            }
        )
    }
}
