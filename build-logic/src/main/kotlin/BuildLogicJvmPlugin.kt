import buildLogic.withCompatPatrouille
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

@Suppress("unused")
class BuildLogicJvmPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        withCompatPatrouille {
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
