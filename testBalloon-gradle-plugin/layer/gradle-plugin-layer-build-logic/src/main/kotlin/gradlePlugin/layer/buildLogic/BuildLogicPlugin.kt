package gradlePlugin.layer.buildLogic

import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class BuildLogicPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply(buildLogic.base.BuildLogicCommonPlugin::class.java)
            apply("org.jetbrains.kotlin.jvm")
            apply("java-gradle-plugin")
        }
    }
}
