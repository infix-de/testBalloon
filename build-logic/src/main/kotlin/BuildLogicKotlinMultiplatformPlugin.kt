import buildLogic.addKotlinStdlibDependency
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.BaseNpmExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.LockFileMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.npm.WasmNpmExtension

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

        rootProject.plugins.apply(NpmExtensionSetupRootPlugin::class.java)

        addKotlinStdlibDependency()

        val kotlin = extensions.getByName("kotlin") as KotlinMultiplatformExtension

        kotlin.compilerOptions {
            // WORKAROUND: Disable until KLIB resolver warnings can be suppressed or no longer appear.
            //     See also: https://youtrack.jetbrains.com/issue/KT-78277
            // freeCompilerArgs.addAll("-Werror")
        }
    }
}

private class NpmExtensionSetupRootPlugin : Plugin<Project> {
    // WORKAROUND https://youtrack.jetbrains.com/issue/KT-79811/KJS-kotlinUpgradePackageLock-task-is-unreliable

    override fun apply(target: Project): Unit = with(target) {
        fun BaseNpmExtension.configurePackageLockReports() {
            if (System.getenv("CI") != null) {
                packageLockMismatchReport.set(LockFileMismatchReport.NONE)
                packageLockAutoReplace.set(true)
            }
        }

        plugins.withType(NodeJsRootPlugin::class.java) {
            extensions.findByType(NpmExtension::class.java)?.apply {
                configurePackageLockReports()
            }
        }

        plugins.withType(WasmNodeJsRootPlugin::class.java) {
            extensions.findByType(WasmNpmExtension::class.java)?.apply {
                configurePackageLockReports()
            }
        }
    }
}
