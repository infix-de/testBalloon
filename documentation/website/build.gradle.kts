import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.gradle.util.internal.VersionNumber
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.moveTo
import kotlin.io.path.writeText

plugins {
    kotlin("jvm") apply false
    id("buildLogic.dokka")
}

dependencies {
    dokka(project(":testBalloon-framework-shared"))
    dokka(project(":testBalloon-framework-core"))

    //noinspection UseTomlInstead
    dokkaHtmlPlugin("org.jetbrains.dokka:versioning-plugin")
}

buildscript {
    dependencies {
        classpath(libs.io.ktor.server.netty)
        classpath(libs.io.ktor.client.cio)
    }
}

val devVersion = "dev" // must be synchronized with the 'site_dir' setting in mkdocs.yml
val latestVersionAlias = "latest"
val distributionDirectory: Directory = layout.projectDirectory.dir("distribution")
val apiVersionsDirectory: Directory = distributionDirectory.dir("apiVersions")
val mkdocsVersionsFile: RegularFile = distributionDirectory.file("versions.json")

val previousVersions by lazy {
    if (distributionDirectory.asFile.exists()) {
        distributionDirectory.asFile.listFiles().mapNotNull {
            if (it.isDirectory && it.name.first().isDigit()) VersionNumber.parse(it.name) else null
        }.sortedByDescending { it }.map { "$it" }
    } else {
        listOf()
    }
}

val useProjectVersionPropertyName = "local.documentation.useProjectVersion"
val projectCommonVersion = project.version.toString().replace(Regex("-.*"), "")
val newApiVersion = if (providers.gradleProperty(useProjectVersionPropertyName).orNull == "true") {
    projectCommonVersion
} else {
    devVersion
}
val newApiDirectory: Directory = apiVersionsDirectory.dir(newApiVersion)

dokka {
    moduleName.set("TestBalloon")

    dokkaPublications {
        html {
            outputDirectory = newApiDirectory
        }
    }

    pluginsConfiguration {
        versioning {
            version = newApiVersion
            versionsOrdering = listOf(newApiVersion) + previousVersions
            olderVersionsDir = apiVersionsDirectory
        }

        html {
            customAssets.from("docs/assets/logo-icon.svg")
            footerMessage.set("Copyright © 2025 infix Software-Systeme GmbH")
        }
    }
}

tasks {
    val dokkaFixPublication by registering {
        // WORKAROUND: Cross-module HTML links break with package names containing an uppercase character
        //     https://github.com/Kotlin/dokka/issues/4314
        // Fix the new API reference, creating symbolic cross-module directory links from "broken name" to
        // "actual name".
        fun String.brokenVariant() = replace("de.infix.testBalloon", "de.infix.test-balloon")

        val originalDirectories = listOf(
            "testBalloon-framework-shared/de.infix.testBalloon.framework.shared",
            "testBalloon-framework-core/de.infix.testBalloon.framework.core"
        ).onEach { relativePath ->
            outputs.dir(newApiDirectory.dir(relativePath.brokenVariant()))
        }.map { relativePath -> newApiDirectory.dir(relativePath).toString() }

        doLast {
            for (originalDirectory in originalDirectories) {
                val linkSource = Path(originalDirectory.brokenVariant())
                val linkTarget = Path(originalDirectory).fileName
                linkSource.deleteIfExists()
                Files.createSymbolicLink(linkSource, linkTarget)
            }
        }

        // Save space in the new API's 'older' versions directory, by deleting its version subdirectories and
        // replacing each with a symbolic link to the corresponding original directory.
        val olderVersionsDirectory = newApiDirectory.asFile.toPath() / "older"
        val regularVersions = previousVersions

        doLast {
            for (version in regularVersions) {
                val linkSource = olderVersionsDirectory / version
                val linkTarget = Path("../../$version")
                @OptIn(ExperimentalPathApi::class)
                if (linkSource.exists()) linkSource.deleteRecursively()
                Files.createSymbolicLink(linkSource, linkTarget)
            }
        }
    }

    val dokkaGeneratePublicationHtml by existing {
        finalizedBy(dokkaFixPublication)
    }

    val pythonVirtualenvDirectory = layout.projectDirectory.dir(".cache/venv").asFile.absolutePath

    val installMkdocs by registering {
        val projectDir = projectDir

        outputs.dir(pythonVirtualenvDirectory)

        doLast {
            fun run(vararg arguments: String) {
                val exitCode = ProcessBuilder(*arguments).directory(projectDir).inheritIO().start().waitFor()
                if (exitCode != 0) throw Error("Process ${arguments.toList()} exited with code $exitCode")
            }

            if (!(Path(pythonVirtualenvDirectory) / "bin").exists()) {
                run("python3", "-m", "venv", pythonVirtualenvDirectory)
                run(
                    "$pythonVirtualenvDirectory/bin/pip3",
                    "install",
                    "mkdocs-material",
                    "mkdocs-material[imaging]",
                    "mkdocs-markdownextradata-plugin"
                )
            }
        }
    }

    val generateDocumentationVariables by registering {
        val generatedVariablesDirectory = layout.buildDirectory.dir("generated/documentationVariables")
        outputs.dir(generatedVariablesDirectory)

        val projectVariables = mapOf("version" to newApiVersion)

        doLast {
            val directory = Path("${generatedVariablesDirectory.get()}")
            check(directory.exists() || directory.toFile().mkdirs()) { "Could not create directory '$directory'" }
            (directory / "project.yaml").writeText(
                buildString {
                    for ((name, value) in projectVariables) {
                        appendLine("$name: \"$value\"")
                    }
                }
            )
        }
    }

    register<Exec>("documentationWebsiteUpdate") {
        group = "documentation"
        description = "Updates 'distribution' with main documentation and the API reference."

        workingDir = projectDir

        inputs.files(installMkdocs)
        inputs.files(generateDocumentationVariables)
        dependsOn(dokkaGeneratePublicationHtml)

        commandLine = listOf("$pythonVirtualenvDirectory/bin/mkdocs", "build", "--clean")

        val distributionDirectory = distributionDirectory.asFile.toPath()
        val mkdocsVersionsFile = mkdocsVersionsFile.asFile
        val devVersion = devVersion
        val latestVersionAlias = latestVersionAlias
        val regularVersions = previousVersions
        val apiVersionsName = apiVersionsDirectory.asFile.name
        val newApiVersion = newApiVersion

        doFirst {
            // Update the mkdocs versions file with all existing versions, plus 'dev'.
            mkdocsVersionsFile.writeText(
                buildString {
                    appendLine("[")
                    appendLine(
                        buildList {
                            add("""{ "version": "$devVersion", "title": "$devVersion 🚧", "aliases": [] }""")
                            regularVersions.forEachIndexed { index, version ->
                                val aliases = if (index == 0) "\"latest\"" else ""
                                add("""{ "version": "$version", "title": "$version", "aliases": [$aliases] }""")
                            }
                        }.joinToString(separator = ",\n")
                    )
                    appendLine("]")
                }
            )

            // Update the 'latest' version link.
            (regularVersions.firstOrNull() ?: devVersion).let { latestVersion ->
                val base = distributionDirectory
                val aliasLink = base / latestVersionAlias
                aliasLink.deleteIfExists()
                Files.createSymbolicLink(aliasLink, Path(latestVersion))
            }

            // Create or update the api link.
            var linkDirectory = distributionDirectory / devVersion
            if (!linkDirectory.exists()) linkDirectory.createDirectory()
            var linkSource = linkDirectory / "api"
            linkSource.deleteIfExists()
            Files.createSymbolicLink(linkSource, Path("../$apiVersionsName") / newApiVersion)

            println("The API reference was created for version '$newApiVersion'.")
        }
    }

    register("documentationWebsiteMigrateVersion") {
        group = "documentation"
        description = "Migrates the 'dev' version to $projectCommonVersion"

        val newApiVersion = newApiVersion
        val devVersion = devVersion
        val useProjectVersionPropertyName = useProjectVersionPropertyName
        val distributionDirectory = distributionDirectory.asFile.toPath()
        val projectCommonVersion = projectCommonVersion

        doLast {
            // Ensure that the next update creates a dev version.
            if (newApiVersion != devVersion) {
                throw Error(
                    "Migration requires that the Gradle property '$useProjectVersionPropertyName' is unset."
                )
            }
            (distributionDirectory / devVersion).moveTo(distributionDirectory / projectCommonVersion)
        }
    }

    register("documentationWebsiteServiceStart") {
        group = "documentation"

        val distributionDirectory = distributionDirectory.asFile.absoluteFile
        val port = 8000

        doLast {
            println("Documentation website is at http://localhost:$port/")
            embeddedServer(Netty, port = port) {
                install(ShutDownUrl.ApplicationCallPlugin) {
                    shutDownUrl = "/shutdown"
                    exitCodeSupplier = { 0 }
                }
                routing {
                    staticFiles("/", distributionDirectory)
                }
            }.start(wait = true)
        }
    }

    register("documentationWebsiteServiceStop") {
        group = "documentation"

        val port = 8000

        doLast {
            runBlocking {
                HttpClient(CIO).request("http://localhost:$port/shutdown")
            }
        }
    }
}
