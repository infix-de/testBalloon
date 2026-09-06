package de.infix.testBalloon.gradlePlugin

import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import de.infix.testBalloon.framework.shared.internal.asKotlinVersion
import de.infix.testBalloon.gradlePlugin.internal.buildConfig.BuildConfig.PROJECT_COMPILER_PLUGIN_ID
import de.infix.testBalloon.gradlePlugin.internal.buildConfig.BuildConfig.PROJECT_VERSION
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

@OptIn(TestBalloonInternalApi::class)
@Suppress("unused")
class TestBalloonGradlePlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(kotlinSpecificPluginId())
        }
    }

    private fun Project.kotlinSpecificPluginId(): String {
        val kotlinGradlePluginVersion = getKotlinPluginVersion().asKotlinVersion()
        val dotOrDash = Regex("[.-]")

        for (kotlinVersionString in listOf("2.4.20-RC", "2.3.20", "2.2.0")) {
            val adapterVersion = kotlinVersionString.asKotlinVersion()
            if (adapterVersion <= kotlinGradlePluginVersion) {
                return "$PROJECT_COMPILER_PLUGIN_ID.gradlePlugin.kotlin${kotlinVersionString.replace(dotOrDash, "")}"
            }
        }

        throw NotImplementedError(
            "$PROJECT_COMPILER_PLUGIN_ID: Kotlin compiler version '$kotlinGradlePluginVersion' is unsupported" +
                " in TestBalloon $PROJECT_VERSION."
        )
    }
}
