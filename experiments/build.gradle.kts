import buildLogic.addTestBalloonPluginFromProject
import buildLogic.applyHierarchy
import buildLogic.jsTargets
import buildLogic.nativeTargets
import buildLogic.versionFromCatalog
import org.gradle.api.internal.artifacts.dependencies.DefaultFileCollectionDependency
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.gradleup.compat.patrouille")
}

addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkShared)

compatPatrouille {
    java(versionFromCatalog("jdk").toInt())
    kotlin(versionFromCatalog("org.jetbrains.kotlin"))

    checkApiDependencies(compat.patrouille.Severity.ERROR)
    checkRuntimeDependencies(compat.patrouille.Severity.ERROR)
}

kotlin {
    compilerOptions {
        // freeCompilerArgs.addAll("-P", "plugin:de.infix.testBalloon:debugLevel=DISCOVERY")
    }

    jvm()
    jsTargets()
    nativeTargets()

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyHierarchy {
        withWasmWasi()
    }

    androidLibrary {
        namespace = "org.example.android.multiplatform.library"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {}.configure {
            // isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            managedDevices {
                localDevices {
                    @Suppress("UnstableApiUsage")
                    create("pixel2api30") {
                        device = "Pixel 2"
                        apiLevel = 30
                        systemImageSource = "aosp"
                    }
                }
            }
        }
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(projects.testBalloonFrameworkCore)
                implementation(kotlin("test")) // for assertions only
            }
        }

        named("androidHostTest") {
            dependencies {
                // required for local tests with TestBalloon outside this project:
                //     implementation("de.infix.testBalloon:testBalloon-framework-core:${testBalloonVersion}")
                // instead of this project-internal dependency:
                implementation(projects.testBalloonFrameworkCore)
            }
        }

        named("androidDeviceTest") {
            dependencies {
                // required for instrumented tests with TestBalloon outside this project:
                //     implementation("de.infix.testBalloon:testBalloon-framework-core:${testBalloonVersion}")
                // instead of this project-internal dependency:
                implementation(projects.testBalloonFrameworkCore)
                implementation(libs.androidx.test.runner)
            }
        }
    }
}

tasks {
    register("listConfigurations") {
        group = "help"
        notCompatibleWithConfigurationCache("Not important.")
        dependsOn("commonizeNativeDistribution")

        doLast {
            project.configurations.forEach { configuration ->
                if (configuration.dependencies.isNotEmpty()) {
                    println("${configuration.name} – ${configuration.description}")
                    configuration.dependencies.forEach { dependency ->
                        if (dependency is DefaultFileCollectionDependency && !dependency.files.isEmpty) {
                            println("\tFiles:")
                            dependency.files.forEach {
                                println("\t\t$it")
                            }
                        } else {
                            println("\t$dependency")
                        }
                    }
                }
            }
        }
    }
}
