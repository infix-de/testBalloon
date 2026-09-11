package de.infix.testBalloon.gradlePlugin

import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import de.infix.testBalloon.framework.shared.internal.asKotlinVersion
import de.infix.testBalloon.gradlePlugin.internal.buildConfig.BuildConfig.PROJECT_GRADLE_PLUGIN_ID
import de.infix.testBalloon.gradlePlugin.internal.buildConfig.BuildConfig.PROJECT_VERSION
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

@OptIn(TestBalloonInternalApi::class)
@Suppress("unused")
class TestBalloonGradlePlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(kotlinLayerPluginId())
        }
    }

    private fun Project.kotlinLayerPluginId(): String {
        val kotlinGradlePluginVersion = getKotlinPluginVersion().asKotlinVersion()

        for (kotlinLayerVersionString in listOf("2.4.20", "2.3.20", "2.2.0")) {
            val kotlinLayerVersion = kotlinLayerVersionString.asKotlinVersion()
            if (kotlinLayerVersion <= kotlinGradlePluginVersion) {
                val kotlinLayerVersionId = "kotlin${kotlinLayerVersionString.replace(Regex("[.-]"), "")}"
                return "$PROJECT_GRADLE_PLUGIN_ID.internal.layer.$kotlinLayerVersionId"
            }
        }

        throw NotImplementedError(
            "Plugin $PROJECT_GRADLE_PLUGIN_ID: Kotlin compiler version '$kotlinGradlePluginVersion'" +
                " is unsupported in TestBalloon $PROJECT_VERSION."
        )
    }
}
