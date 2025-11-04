import buildLogic.applyHierarchy
import buildLogic.jsTargets
import buildLogic.nativeTargets
import buildLogic.withCompatPatrouille
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

@Suppress("unused")
class BuildLogicMultiplatformPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        withCompatPatrouille {
            apply("buildLogic.multiplatform-base")
        }

        extensions.configure<KotlinMultiplatformExtension>("kotlin") {
            jvm()
            jsTargets()
            nativeTargets()

            @OptIn(ExperimentalWasmDsl::class)
            wasmWasi {
                nodejs()
            }

            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            applyHierarchy {
                withWasmWasi()
            }
        }
    }
}
