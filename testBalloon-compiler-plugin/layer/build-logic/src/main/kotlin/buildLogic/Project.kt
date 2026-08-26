package buildLogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension

fun Project.versionFromCatalog(alias: String): String =
    versionCatalogs.named("libs").findVersion(alias).get().displayName

private val Project.versionCatalogs get() = extensions.getByType(VersionCatalogsExtension::class.java)

fun Project.jdkVersion() = versionFromCatalog("jdk").toInt()
