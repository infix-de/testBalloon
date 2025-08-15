import buildLogic.withCompatPatrouille
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class BuildLogicAndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        withCompatPatrouille {
            apply("com.android.application")
            apply("org.jetbrains.kotlin.android")
        }

        group = project.property("local.PROJECT_GROUP_ID")!!
    }
}
