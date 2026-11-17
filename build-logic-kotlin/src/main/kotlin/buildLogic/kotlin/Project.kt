@file:Suppress("UnstableApiUsage")

package buildLogic.kotlin

import buildLogic.base.kotlinBaseVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin

fun Project.addKotlinStdlibDependency() {
    when (val extension = extensions.getByName("kotlin")) {
        is KotlinJvmExtension -> {
            dependencies.add("api", "org.jetbrains.kotlin:kotlin-stdlib:${kotlinBaseVersion()}")
        }

        is KotlinMultiplatformExtension -> {
            extension.sourceSets.getByName("commonMain").dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib:${kotlinBaseVersion()}")
            }
        }
    }
}

fun Project.applyWebpackVersionsFix() {
    // WORKAROUND: https://youtrack.jetbrains.com/issue/KT-90417
    //     KJS, Gradle: Two webpack versions after kotlinNpmInstall:
    //     "TypeError: The compilation argument must be an instance of Compilation"

    if (this != rootProject) {
        throw IllegalArgumentException("The webpack versions fix must be applied to a root project")
    }

    plugins.withType(NodeJsRootPlugin::class.java) {
        extensions.configure<NodeJsRootExtension>("kotlinNodeJs") {
            versions.webpack.version = "5.112.0"
        }
    }
}
