import buildLogic.versionFromCatalog
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.internal.VersionNumber

@Suppress("unused")
class BuildLogicAndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("buildLogic.common")
            apply("com.android.application")
            if (VersionNumber.parse(versionFromCatalog("android-gradle-plugin")).major < 9) {
                apply("org.jetbrains.kotlin.android")
            }
        }

        group = project.property("local.PROJECT_GROUP_ID")!!
    }
}
