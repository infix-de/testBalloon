package buildLogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun Project.versionFromCatalog(alias: String): String =
    versionCatalogs.named("libs").findVersion(alias).get().displayName

fun Project.libraryFromCatalog(alias: String): String =
    versionCatalogs.named("libs").findLibrary(alias).get().get().toString()

private val Project.versionCatalogs get() = extensions.getByType(VersionCatalogsExtension::class.java)

fun Project.jdkVersion() = versionFromCatalog("jdk").toInt()
fun Project.kotlinVersion() = versionFromCatalog("org.jetbrains.kotlin")
fun Project.robolectricJdkVersion() = jdkVersion().coerceAtLeast(versionFromCatalog("org-robolectric-jdk").toInt())
fun Project.junitJupiterJdkVersion() = jdkVersion().coerceAtLeast(versionFromCatalog("org-junit-jupiter-jdk").toInt())

fun Project.addKotlinStdlibDependency() {
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

fun Project.propagateLifecycleTasksToIncludedBuilds() {
    afterEvaluate {
        for (taskName in listOf("clean", "lintKotlin")) {
            tasks.named(taskName) {
                dependsOn(gradle.includedBuilds.filter { !it.name.startsWith("build-") }.map { it.task(":$taskName") })
            }
        }
    }
}
