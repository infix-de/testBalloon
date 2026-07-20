import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Plugin for Kotlin/JVM which does not configure a default kotlin-stdlib dependency.
 *
 * This works in conjunction with `kotlin.stdlib.default.dependency=false` in the root project's `gradle.properties`.
 */
@Suppress("unused")
class BuildLogicKotlinJvmBasePlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("buildLogic.common")
            apply("org.jetbrains.kotlin.jvm")
        }

        extensions.configure<KotlinJvmProjectExtension>("kotlin") {
            compilerOptions {
                // WORKAROUND: Disable until KLIB resolver warnings can be suppressed or no longer appear.
                //     See also: https://youtrack.jetbrains.com/issue/KT-78277
                // freeCompilerArgs.addAll("-Werror")
            }
        }
    }
}
