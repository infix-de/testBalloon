import buildLogic.versionFromCatalog
import buildLogic.withTapmoc
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("unused")
class BuildLogicMultiplatformPlusAndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        withTapmoc {
            apply("buildLogic.multiplatform")
            // https://developer.android.com/kotlin/multiplatform/plugin
            apply("com.android.kotlin.multiplatform.library")
        }

        extensions.configure<KotlinMultiplatformExtension>("kotlin") {
            extensions.configure<KotlinMultiplatformAndroidLibraryExtension>("androidLibrary") {
                compileSdk = versionFromCatalog("android-compileSdk").toInt()
            }
        }
    }
}
