package buildLogic

import de.infix.testBalloon.framework.internal.TestBalloonInternalApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationMultiplatformExtension
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
    macosX64()
    macosArm64()
    iosSimulatorArm64()
    iosX64()
    iosArm64()
    // Tier 2
    linuxX64()
    linuxArm64()
    watchosSimulatorArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosX64()
    tvosArm64()
    // Tier 3
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    mingwX64()
    watchosDeviceArm64()
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
                group("native")
                nonJvm()
            }
        }
    }
}

fun KotlinMultiplatformExtension.enableAbiValidation() {
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    extensions.configure<AbiValidationMultiplatformExtension>("abiValidation") {
        enabled.set(true)
        filters {
            excluded {
                byNames.add(TestBalloonInternalApi::class.qualifiedName!!)
                annotatedWith.add(TestBalloonInternalApi::class.qualifiedName!!)
            }
        }
    }

    explicitApi()
}
