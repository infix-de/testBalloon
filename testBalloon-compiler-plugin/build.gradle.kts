@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

plugins {
    // id("buildLogic.kotlin-multiplatform")
    kotlin("multiplatform") version "2.4.0"
    // id("buildLogic.publishing")
    pluginDevKit("compiler-plugin")
}

description = "TestBalloon compiler plugin"

pluginDevKit {
    componentRegistrar = "de.infix.testBalloon.compilerPlugin.TestBalloonComponentRegistrar"
    commandLineProcessor = "de.infix.testBalloon.compilerPlugin.TestBalloonCommandLineProcessor"

    // cliVersions(min = kotlinVersion())
    cliVersions(min = "2.3.0")

    versionHierarchy {
        split("2.4.0")
        split("2.3.20")
        split("2.3.0")
    }
}

val Project.rootGroup: String get() = "${project.property("local.PROJECT_ROOT_GROUP")}"

buildConfig {
    buildConfigField("String", "PROJECT_VERSION", "\"$version\"")
    buildConfigField("String", "PROJECT_ROOT_GROUP", "\"$rootGroup\"")
    buildConfigField("String", "PROJECT_FRAMEWORK_CORE_ARTIFACT_ID", "\"testBalloon-framework-core\"")

    // TODO: remove
    buildConfigField(
        "String",
        "PROJECT_COMPILER_PLUGIN_ID",
        "\"${project.property("local.PROJECT_COMPILER_PLUGIN_ID")}\""
    )
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }

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
