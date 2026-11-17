@file:Suppress("UnstableApiUsage")

package buildLogic.base

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.invocation.Gradle

fun Project.versionFromCatalog(alias: String): String =
    @Suppress("NewApi")
    versionCatalogs.named("libs").findVersion(alias).get().displayName

fun Project.libraryFromCatalog(alias: String): String =
    versionCatalogs.named("libs").findLibrary(alias).get().get().toString()

private val Project.versionCatalogs get() = extensions.getByType(VersionCatalogsExtension::class.java)

fun Project.baseJdkVersion() = versionFromCatalog("base.jdk").toInt()
fun Project.gradleJdkVersion() = versionFromCatalog("gradle.jdk").toInt()
fun Project.junitJupiterJdkVersion() =
    baseJdkVersion().coerceAtLeast(versionFromCatalog("org-junit-jupiter-jdk").toInt())
fun Project.junitPlatformJdkVersion() =
    baseJdkVersion().coerceAtLeast(versionFromCatalog("org-junit-platform-jdk").toInt())
fun Project.kotestJdkVersion() = baseJdkVersion().coerceAtLeast(versionFromCatalog("io-kotest-jdk").toInt())
fun Project.kotlinBaseVersion() = versionFromCatalog("org.jetbrains.kotlin.base")
fun Project.kotlinLatestReleaseVersion() = versionFromCatalog("org.jetbrains.kotlin.latest.release")
fun Project.robolectricJdkVersion() = baseJdkVersion().coerceAtLeast(versionFromCatalog("org-robolectric-jdk").toInt())

fun Project.integrationTestRepository() =
    provider { gradle.rootBuild.rootProject.isolated.projectDirectory.dir("build/integration-test-repository") }

fun Project.aggregationStagingRepository() =
    provider { gradle.rootBuild.rootProject.isolated.projectDirectory.dir("build/aggregation-staging-repository") }

private val Gradle.rootBuild: Gradle get() = parent.let { it?.rootBuild ?: this }

val Project.rootGroup: String get() = "${project.property("local.PROJECT_ROOT_GROUP")}"

val Project.compilerPluginId: String get() = "${project.property("local.PROJECT_COMPILER_PLUGIN_ID")}"
val Project.gradlePluginId: String get() = "${project.property("local.PROJECT_GRADLE_PLUGIN_ID")}"
val Project.gradlePluginArtifactId: String get() = "${project.property("local.PROJECT_GRADLE_PLUGIN_ARTIFACT_ID")}"
val Project.frameworkCoreArtifactId: String get() = "${project.property("local.PROJECT_FRAMEWORK_CORE_ARTIFACT_ID")}"

fun Project.propagateLifecycleTasksToIncludedBuilds() {
    afterEvaluate {
        val excludingRegex = Regex("""(^build-)|(-build-)""")
        for (taskName in listOf("clean", "lintKotlin", "check")) {
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
