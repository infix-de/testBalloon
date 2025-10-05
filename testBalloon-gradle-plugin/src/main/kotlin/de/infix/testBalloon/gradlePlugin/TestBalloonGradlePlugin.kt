@file:OptIn(TestBalloonInternalApi::class)

package de.infix.testBalloon.gradlePlugin

import buildConfig.BuildConfig.PROJECT_ABSTRACTIONS_ARTIFACT_ID
import buildConfig.BuildConfig.PROJECT_COMPILER_PLUGIN_ARTIFACT_ID
import buildConfig.BuildConfig.PROJECT_COMPILER_PLUGIN_ID
import buildConfig.BuildConfig.PROJECT_GROUP_ID
import buildConfig.BuildConfig.PROJECT_JUNIT_PLATFORM_LAUNCHER
import buildConfig.BuildConfig.PROJECT_VERSION
import de.infix.testBalloon.framework.internal.DebugLevel
import de.infix.testBalloon.framework.internal.TestBalloonInternalApi
import de.infix.testBalloon.gradlePlugin.shared.TestBalloonGradleProperties
import de.infix.testBalloon.gradlePlugin.shared.configureWithTestBalloon
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

@Suppress("unused")
class TestBalloonGradlePlugin : KotlinCompilerPluginSupportPlugin {

    private lateinit var testBalloonProperties: TestBalloonGradleProperties

    override fun apply(target: Project): Unit = with(target) {
        testBalloonProperties = TestBalloonGradleProperties(this)

        // WORKAROUND https://youtrack.jetbrains.com/issue/KT-53477 – KGP misses transitive compiler plugin dependencies
        configurations.named(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME) {
            dependencies.add(
                project.dependencies.create("$PROJECT_GROUP_ID:$PROJECT_ABSTRACTIONS_ARTIFACT_ID:$PROJECT_VERSION")
            )
        }

        val testRuntimeOnlyConfigurationRegex = testBalloonProperties.testRuntimeOnlyConfigurationRegex

        configurations.configureEach {
            if (testRuntimeOnlyConfigurationRegex.containsMatchIn(name)) {
                dependencies.add(project.dependencies.create(PROJECT_JUNIT_PLATFORM_LAUNCHER))
            }
        }

        extensions.create("testBalloon", TestBalloonGradleExtension::class.java)

        configureWithTestBalloon(testBalloonProperties)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(TestBalloonGradleExtension::class.java)
        return testBalloonProperties.testCompilationRegex.containsMatchIn(kotlinCompilation.name).also { applies ->
            if (extension.debugLevel > DebugLevel.NONE) {
                project.logger.warn(
                    "[DEBUG] $PLUGIN_DISPLAY_NAME is ${if (applies) "" else "not "}applicable" +
                        " for Kotlin compilation '${kotlinCompilation.name}'"
                )
            }
        }
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(TestBalloonGradleExtension::class.java)

        return project.provider {
            listOf(
                SubpluginOption(key = "debugLevel", value = extension.debugLevel.toString()),
                SubpluginOption(key = "jvmStandalone", value = extension.jvmStandalone.toString()),
                SubpluginOption(key = "testModuleRegex", value = testBalloonProperties.testModuleRegex)
            )
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
