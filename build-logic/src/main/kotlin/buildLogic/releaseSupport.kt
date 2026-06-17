package buildLogic

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationException
import org.gradle.api.tasks.options.Option

abstract class TagReleaseSet : DefaultTask() {
    @Option(option = "release", description = "Release set version number")
    @get:Input
    var release: String = ""

    @TaskAction
    fun run() {
        if (release.isEmpty()) throw VerificationException("Please specify a release set version number")

        val releaseComponentVersions = releaseComponentVersions(release) ?: return

        for (releaseComponentVersion in releaseComponentVersions) {
            logger.info("Tagging '$RELEASES_PREFIX$releaseComponentVersion' with '$releaseComponentVersion'")
            verifyReleaseComponentVersion(releaseComponentVersion)
            execute("git", "tag", releaseComponentVersion, "$RELEASES_PREFIX$releaseComponentVersion")
        }
    }
}

abstract class PushReleaseSetTags : DefaultTask() {
    @Option(option = "release", description = "Release set version number")
    @get:Input
    var release: String = ""

    @TaskAction
    fun run() {
        if (release.isEmpty()) throw VerificationException("Please specify a release set version number")

        val releaseComponentVersions = releaseComponentVersions(release) ?: return
        val preReleaseComponentVersions = releaseComponentVersions
            .filter { releaseComponentVersion -> releaseComponentVersion.count { it == '-' } > 1 }.toSet()
        val lastRegularReleaseComponentVersion = releaseComponentVersions.last { it !in preReleaseComponentVersions }

        for (releaseComponentVersion in releaseComponentVersions) {
            // Skip the last regular version. Its tag should be pushed late for pickup by klibs.io.
            if (releaseComponentVersion == lastRegularReleaseComponentVersion) continue
            verifyReleaseComponentVersion(releaseComponentVersion)
            logger.info("Pushing tag '$releaseComponentVersion'")
            execute("git", "push", "origin", "tag", releaseComponentVersion)
            Thread.sleep(2000)
        }
    }
}

private const val RELEASES_PREFIX = "releases/"

@Suppress("NewApi")
private fun releaseComponentVersions(release: String): List<String>? {
    val process = ProcessBuilder("git", "branch", "--list", "$RELEASES_PREFIX$release*").start()

    val result = process.inputStream.readAllBytes().toString(Charsets.UTF_8).trim().lines().map {
        it.trim().substringAfter(RELEASES_PREFIX)
    }

    return if (process.waitFor() == 0) result else null
}

@Suppress("NewApi")
private fun verifyReleaseComponentVersion(releaseComponentVersion: String) {
    val process = ProcessBuilder(
        "git",
        "show",
        "$RELEASES_PREFIX$releaseComponentVersion:gradle.properties"
    ).start()

    val projectVersion = process.inputStream.readAllBytes().toString(Charsets.UTF_8).trim().lines().mapNotNull { line ->
        line.trim().takeIf { it.startsWith("version=") }?.substringAfter("version=")?.substringBeforeLast("-SNAPSHOT")
    }.singleOrNull()
        ?: throw VerificationException("Could not determine project version for '$releaseComponentVersion'")

    check(projectVersion == releaseComponentVersion) {
        "Project version '$projectVersion' does not match release version '$releaseComponentVersion'"
    }
}

@Suppress("NewApi")
private fun Task.execute(vararg arguments: String) {
    logger.warn(arguments.joinToString(" "))

    val process = ProcessBuilder(*arguments).start()

    process.inputStream.readAllBytes().toString(Charsets.UTF_8).trim().let {
        if (it.isNotEmpty()) logger.warn(it)
    }

    process.errorStream.readAllBytes().toString(Charsets.UTF_8).trim().let {
        if (it.isNotEmpty()) logger.error(it)
    }

    check(process.waitFor() == 0)
}
