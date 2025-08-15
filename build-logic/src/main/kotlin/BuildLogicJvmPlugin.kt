import buildLogic.withCompatPatrouille
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class BuildLogicJvmPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        withCompatPatrouille {
            apply("buildLogic.common")
            apply("org.jetbrains.kotlin.jvm")
        }
    }
}
