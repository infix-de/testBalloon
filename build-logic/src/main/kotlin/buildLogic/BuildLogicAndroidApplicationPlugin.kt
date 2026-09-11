package buildLogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.internal.VersionNumber

@Suppress("unused")
class BuildLogicAndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("com.android.application")
            if (VersionNumber.parse(versionFromCatalog("android-gradle-plugin")).major < 9) {
                apply("org.jetbrains.kotlin.android")
            }
            apply("buildLogic.common")
        }

        group = rootGroup
    }
}
