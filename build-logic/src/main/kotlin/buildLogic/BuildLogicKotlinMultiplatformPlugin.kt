package buildLogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlinx.library.api.watchdog.WatchdogGradleExtension
import org.jetbrains.kotlinx.library.api.watchdog.WatchdogSeverity

/**
 * Build logic for Kotlin Multiplatform targets, excluding Android (AGP) targets.
 */
@Suppress("unused")
class BuildLogicKotlinMultiplatformPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("buildLogic.common")
            apply("org.jetbrains.kotlin.multiplatform")
            apply("org.jetbrains.kotlin.library.api-watchdog")
        }

        addKotlinStdlibDependency()

        val kotlin = extensions.getByName("kotlin") as KotlinMultiplatformExtension

        kotlin.compilerOptions {
            // WORKAROUND: Disable until KLIB resolver warnings can be suppressed or no longer appear.
            //     See also: https://youtrack.jetbrains.com/issue/KT-78277
            // freeCompilerArgs.addAll("-Werror")
        }

        extensions.configure<WatchdogGradleExtension>("apiWatchdog") {
            // Avoid annotation boilerplate to justify frequently occurring deliberate design decisions:
            listOf(
                undocumentedPublicApi, // trivial comments
                exhaustivePublicApi, // safe use > breaking change
                statefulClassWithoutEquals, // state != comparable
                statefulClassWithoutHashCode // state != comparable
            ).forEach { it.set(WatchdogSeverity.NONE) }

            javaInterop.enabled.set(false)

            internalApiAnnotations.addAll(annotationsIndicatingInternalUse)
            internalApiAnnotations.add("kotlin.Deprecated")
        }
    }
}
