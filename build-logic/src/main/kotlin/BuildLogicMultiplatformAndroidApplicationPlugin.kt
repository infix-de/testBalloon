import buildLogic.withTapmoc
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class BuildLogicMultiplatformAndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        withTapmoc {
            apply("buildLogic.multiplatform")
            apply("com.android.application")
        }
    }
}
