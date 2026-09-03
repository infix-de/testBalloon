package buildLogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun Project.versionFromCatalog(alias: String): String =
    versionCatalogs.named("libs").findVersion(alias).get().displayName

private val Project.versionCatalogs get() = extensions.getByType(VersionCatalogsExtension::class.java)

fun Project.baseJdkVersion() = versionFromCatalog("base.jdk").toInt()
fun Project.gradleJdkVersion() = versionFromCatalog("gradle.jdk").toInt()
fun Project.kotlinVersion() = versionFromCatalog("org.jetbrains.kotlin")
fun Project.robolectricJdkVersion() = baseJdkVersion().coerceAtLeast(versionFromCatalog("org-robolectric-jdk").toInt())
fun Project.junitJupiterJdkVersion() =
    baseJdkVersion().coerceAtLeast(versionFromCatalog("org-junit-jupiter-jdk").toInt())

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
        val excludingRegex = Regex("""(^build-)|(-build-)""")
        for (taskName in listOf("clean", "lintKotlin")) {
            tasks.named(taskName) {
                dependsOn(
                    gradle.includedBuilds
                        .filter { !excludingRegex.containsMatchIn(it.name) }
                        .map { it.task(":$taskName") }
                )
            }
        }
    }
}
