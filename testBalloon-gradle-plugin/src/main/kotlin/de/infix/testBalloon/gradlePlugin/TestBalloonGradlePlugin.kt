package de.infix.testBalloon.gradlePlugin

import buildConfig.BuildConfig.PROJECT_COMPILER_PLUGIN_ARTIFACT_ID
import buildConfig.BuildConfig.PROJECT_COMPILER_PLUGIN_ID
import buildConfig.BuildConfig.PROJECT_GROUP_ID
import buildConfig.BuildConfig.PROJECT_JUNIT_PLATFORM_LAUNCHER
import buildConfig.BuildConfig.PROJECT_SHARED_ARTIFACT_ID
import buildConfig.BuildConfig.PROJECT_VERSION
import de.infix.testBalloon.framework.shared.internal.DebugLevel
import de.infix.testBalloon.gradlePlugin.shared.TestBalloonGradleExtension
import de.infix.testBalloon.gradlePlugin.shared.TestBalloonGradleProperties
import de.infix.testBalloon.gradlePlugin.shared.compilerPluginOptionValues
import de.infix.testBalloon.gradlePlugin.shared.configureWithTestBalloon
import org.gradle.api.Project
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

@Suppress("unused")
class TestBalloonGradlePlugin : KotlinCompilerPluginSupportPlugin {

    private lateinit var project: Project
    private lateinit var testBalloonProperties: TestBalloonGradleProperties
    private val extension by lazy { project.extensions.getByType(TestBalloonGradleExtension::class.java) }

    override fun apply(target: Project): Unit = with(target) {
        this@TestBalloonGradlePlugin.project = target
        testBalloonProperties = TestBalloonGradleProperties(this)

        try {
            // WORKAROUND https://youtrack.jetbrains.com/issue/KT-53477 – KGP misses transitive compiler plugin
            //     dependencies
            configurations.named(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME) {
                dependencies.add(
                    project.dependencies.create("$PROJECT_GROUP_ID:$PROJECT_SHARED_ARTIFACT_ID:$PROJECT_VERSION")
                )
            }
        } catch (_: UnknownConfigurationException) {
            // The configuration "kotlinNativeCompilerPluginClasspath" is unavailable with AGP9's built-in Kotlin.
        }

        configureWithTestBalloon(
            testBalloonProperties = testBalloonProperties,
            pluginDisplayName = PLUGIN_DISPLAY_NAME,
            junitPlatformLauncher = PROJECT_JUNIT_PLATFORM_LAUNCHER
        )
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        // NOTE: `testBalloonProperties.isTestSourceSet` must be used in lazy task configuration or in `afterEvaluate`.
        // We rely on the documentation of `KotlinCompilerPluginSupportPlugin`, which states:
        // > the Kotlin plugin inspects the project model in an afterEvaluate handler.
        return testBalloonProperties.isTestSourceSet(
            kotlinCompilation.defaultSourceSet.name
        ).also { applies ->
            if (!applies && extension.debugLevel > DebugLevel.NONE) {
                project.logger.warn(
                    "$PLUGIN_DISPLAY_NAME: [DEBUG] compiler plugin is not applicable" +
                        " ('${kotlinCompilation.defaultSourceSet.name}' is not a test source set)."
                )
            }
        }
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> =
        project.provider {
            // NOTE: Before invoking `compilerPluginOptionValues()`, all plugins must have been applied to the
            // project. We rely on the documentation of `KotlinCompilerPluginSupportPlugin`, which states:
            // > the Kotlin plugin inspects the project model in an afterEvaluate handler.
            compilerPluginOptionValues(extension, testBalloonProperties).map { (key, value) ->
                SubpluginOption(key, value)
            }
        }

    override fun getCompilerPluginId(): String = PROJECT_COMPILER_PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = PROJECT_GROUP_ID,
        artifactId = PROJECT_COMPILER_PLUGIN_ARTIFACT_ID,
        version = PROJECT_VERSION
    )
}

private const val PLUGIN_DISPLAY_NAME = "Plugin $PROJECT_COMPILER_PLUGIN_ID"
