import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Build logic for Kotlin Multiplatform targets, excluding Android (AGP) targets.
 */
@Suppress("unused")
class BuildLogicKotlinMultiplatformPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("buildLogic.common")
            apply("org.jetbrains.kotlin.multiplatform")
        }

        val kotlin = extensions.getByName("kotlin") as KotlinMultiplatformExtension

        kotlin.compilerOptions {
            // WORKAROUND: Disable until KLIB resolver warnings can be suppressed or no longer appear.
            //     See also: https://youtrack.jetbrains.com/issue/KT-78277
            // freeCompilerArgs.addAll("-Werror")
        }
    }
}
