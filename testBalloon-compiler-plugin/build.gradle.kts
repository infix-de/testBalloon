@file:Suppress("UnstableApiUsage")

import buildLogic.base.compilerPluginId
import buildLogic.base.frameworkCoreArtifactId
import buildLogic.base.rootGroup
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

plugins {
    kotlin("multiplatform") version "2.4.21"
    id("com.gradleup.tapmoc") version "0.5.0"
    id("buildLogic.base.publishing")
    alias(libs.plugins.org.jetbrains.kotlin.compiler.plugin.devkit.functional.test.publishing)
    alias(libs.plugins.org.jetbrains.kotlin.compiler.plugin.devkit.compiler.plugin)
}

group = rootGroup

description = "TestBalloon compiler plugin"

tapmoc {
    kotlin("2.4.21")
    java(8)
}

pluginDevKit {
    componentRegistrar = "de.infix.testBalloon.compilerPlugin.TestBalloonComponentRegistrar"
    commandLineProcessor = "de.infix.testBalloon.compilerPlugin.TestBalloonCommandLineProcessor"

    cliVersions(min = "2.2.21")

    versionHierarchy {
        split("2.3.0")
        split("2.3.20")
        split("2.4.0")
        split("2.4.20")
    }

    companionLibrary("$rootGroup:testBalloon-framework-shared:$version")

    generateTestsEnabled = false
}

buildConfig {
    buildConfigField("String", "PROJECT_VERSION", "\"$version\"")
    buildConfigField("String", "PROJECT_ROOT_GROUP", "\"$rootGroup\"")
    buildConfigField("String", "PROJECT_FRAMEWORK_CORE_ARTIFACT_ID", "\"$frameworkCoreArtifactId\"")
    buildConfigField("String", "PROJECT_COMPILER_PLUGIN_ID", "\"$compilerPluginId\"")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("$rootGroup:testBalloon-framework-shared:$version")
            }
        }
    }
}

configureDiagnosticsTask()

@Suppress("NewApi")
private fun Project.configureDiagnosticsTask() {
    val taskName = "sourceSetDiagnostics"

    tasks.register(taskName) {
        group = "help"
        description = "Shows diagnostics for the source set topology"

        val testSourceSetsDiagram = provider {
            project.extensions.findByType(KotlinProjectExtension::class.java)?.let { kotlin ->
                val sourceSets = kotlin.sourceSets.filter { "Main" in it.name }

                buildString {
                    appendLine()
                    appendLine("Diagram for display on https://mermaid.live/")
                    appendLine()
                    appendLine("classDiagram")
                    for (sourceSet in sourceSets) {
                        appendLine("    class ${sourceSet.name}")
                    }
                    for (sourceSet in sourceSets) {
                        for (dependsOnSourceSet in sourceSet.dependsOn.map { it.name }) {
                            appendLine("    $dependsOnSourceSet <|-- ${sourceSet.name}")
                        }
                    }
                }
            }
        }

        doLast {
            testSourceSetsDiagram.orNull?.let { print(it) }
        }
    }
}
