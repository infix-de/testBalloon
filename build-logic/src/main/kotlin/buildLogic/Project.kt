package buildLogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.invocation.Gradle
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun Project.versionFromCatalog(alias: String): String =
    @Suppress("NewApi")
    versionCatalogs.named("libs").findVersion(alias).get().displayName

private val Project.versionCatalogs get() = extensions.getByType(VersionCatalogsExtension::class.java)

fun Project.baseJdkVersion() = versionFromCatalog("base.jdk").toInt()
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

fun Project.integrationTestRepository() =
    gradle.rootBuild.rootProject.layout.buildDirectory.dir("integration-test-repository")

fun Project.aggregationStagingRepository() =
    gradle.rootBuild.rootProject.layout.buildDirectory.dir("aggregation-staging-repository")

private val Gradle.rootBuild: Gradle get() = parent.let { it?.rootBuild ?: this }

val Project.rootGroup: String get() = "${project.property("local.PROJECT_ROOT_GROUP")}"
