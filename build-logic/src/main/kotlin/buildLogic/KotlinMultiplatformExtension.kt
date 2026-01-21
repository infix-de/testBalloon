package buildLogic

import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalTestingApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyTemplate

fun KotlinMultiplatformExtension.jsTargets() {
    js {
        nodejs()
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        browser()
    }
}

fun KotlinMultiplatformExtension.nativeTargets() {
    // Kotlin/Native target support – see https://kotlinlang.org/docs/native-target-support.html
    // Tier 1
    macosArm64()
    iosArm64()
    iosSimulatorArm64()
    // Tier 2
    linuxX64()
    linuxArm64()
    watchosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosArm64()
    // Tier 3
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    mingwX64()
    watchosDeviceArm64()
    // Tier 3 – scheduled for removal in Kotlin 2.4.0
    macosX64()
    iosX64()
    watchosX64()
    tvosX64()
}

fun KotlinMultiplatformExtension.allTargets(includeWasmWasi: Boolean = true) {
    jvm()
    jsTargets()
    nativeTargets()

    if (includeWasmWasi) {
        @OptIn(ExperimentalWasmDsl::class)
        wasmWasi {
            nodejs()
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyHierarchy {
        if (includeWasmWasi) {
            withWasmWasi()
        }
    }
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
fun KotlinMultiplatformExtension.applyHierarchy(nonJvm: KotlinHierarchyBuilder.() -> Unit = {}) {
    applyHierarchyTemplate(KotlinHierarchyTemplate.default) {
        group("common") {
            // "nonJvm" excludes Android (AGP) targets.
            group("nonJvm") {
                group("jsHosted") {
                    withJs()
                    withWasmJs()
                }
                group("web")
                group("native")
                nonJvm()
            }
        }
    }
}

fun KotlinMultiplatformExtension.enableAbiValidation() {
    // ABI validation is not supported in this backport.
    explicitApi()
}
