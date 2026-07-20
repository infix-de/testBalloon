package buildLogic

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.addKotlinStdlibDependency() {
    when (val extension = extensions.getByName("kotlin")) {
        is KotlinJvmExtension -> {
            dependencies.add("api", "org.jetbrains.kotlin:kotlin-stdlib:${kotlinVersion()}")
        }

        is KotlinMultiplatformExtension -> {
            extension.sourceSets.getByName("commonMain").dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib:${kotlinVersion()}")
            }
        }
    }
}
