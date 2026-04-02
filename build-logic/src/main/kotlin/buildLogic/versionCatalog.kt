package buildLogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension

fun Project.versionFromCatalog(alias: String): String =
    versionCatalogs.named("libs").findVersion(alias).get().displayName

fun Project.libraryFromCatalog(alias: String): String =
    versionCatalogs.named("libs").findLibrary(alias).get().get().toString()

private val Project.versionCatalogs get() = extensions.getByType(VersionCatalogsExtension::class.java)

fun Project.jdkVersion() = versionFromCatalog("jdk").toInt()
fun Project.kotlinVersion() = versionFromCatalog("org.jetbrains.kotlin")
fun Project.robolectricJdkVersion() = jdkVersion().coerceAtLeast(versionFromCatalog("org-robolectric-jdk").toInt())
fun Project.junitJupiterJdkVersion() = jdkVersion().coerceAtLeast(versionFromCatalog("org-junit-jupiter-jdk").toInt())
