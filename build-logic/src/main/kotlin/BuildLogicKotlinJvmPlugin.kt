import buildLogic.kotlinVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("unused")
class BuildLogicKotlinJvmPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("buildLogic.kotlin-jvm-base")
        }

        target.addStdlibDependency()
    }
}

internal fun Project.addStdlibDependency() {
    val extension = extensions.getByName("kotlin")
    when (extension) {
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