import buildLogic.withTapmoc
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Build logic for Kotlin Multiplatform targets, excluding Android (AGP) targets.
 */
@Suppress("unused")
class BuildLogicMultiplatformBasePlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        withTapmoc {
            apply("buildLogic.common")
            apply("org.jetbrains.kotlin.multiplatform")
            apply("org.jetbrains.kotlin.plugin.atomicfu")
        }

        val kotlin = extensions.getByName("kotlin") as KotlinMultiplatformExtension

        kotlin.compilerOptions {
            // WORKAROUND: Disable until KLIB resolver warnings can be suppressed or no longer appear.
            //     See also: https://youtrack.jetbrains.com/issue/KT-78277
            // freeCompilerArgs.addAll("-Werror")
        }
    }
}
