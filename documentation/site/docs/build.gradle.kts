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

val currentVersion = project.version.toString().replace(Regex("-.*"), "")
val outputDirectory: Directory = layout.projectDirectory.dir("api")
val previousVersionsDirectory: Directory = outputDirectory.dir("previousVersions")

dokka {
    moduleName.set("TestBalloon")
    basePublicationsDirectory.set(outputDirectory)

    pluginsConfiguration {
        versioning {
            version = currentVersion
            olderVersionsDir = previousVersionsDirectory
        }

        html {
            customAssets.from("assets/logo-icon.svg")
            footerMessage.set("Copyright © 2025 infix Software-Systeme GmbH")
        }
    }
}

// WORKAROUND: Cross-module HTML links break with package names containing an uppercase character
//     https://github.com/Kotlin/dokka/issues/4314
tasks.named("dokkaGeneratePublicationHtml") {
    val htmlDirectory = outputDirectory.dir("html")

    fun String.fixed() = replace("de.infix.testBalloon", "de.infix.test-balloon")

    val originalDirectories = listOf(
        "testBalloon-framework-shared/de.infix.testBalloon.framework.shared",
        "testBalloon-framework-core/de.infix.testBalloon.framework.core"
    ).onEach { relativePath ->
        outputs.dir(htmlDirectory.dir(relativePath.fixed()))
    }.map { relativePath -> htmlDirectory.dir(relativePath).toString() }

    doLast {
        for (packageDirectory in originalDirectories) {
            println("Fixing $packageDirectory")
            Runtime.getRuntime().exec(arrayOf("ln", "-s", packageDirectory, packageDirectory.fixed())).waitFor()
        }
    }
}

tasks.register<Exec>("serveSite") {
    group = "documentation"
    workingDir = projectDir.parentFile
    commandLine = listOf("$workingDir/venv/bin/mkdocs", "serve")
}

tasks.register<Exec>("buildSite") {
    group = "documentation"
    workingDir = projectDir.parentFile
    commandLine = listOf("$workingDir/venv/bin/mkdocs", "build")
}
