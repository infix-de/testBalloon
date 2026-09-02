@file:Suppress("UnstableApiUsage")

import buildLogic.propagateLifecycleTasksToIncludedBuilds
import kotlin.io.path.div
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

plugins {
    id("buildLogic.kotlin-jvm-base")
    id("buildLogic.publishing")
    alias(libs.plugins.com.github.gmazzo.buildconfig)
}

description = "TestBalloon compiler plugin"

/** Dependencies to be embedded into the compiler plugin artifact. */
val embeddedCompileOnly = configurations.dependencyScope("embeddedCompileOnly")
val embeddedDynamicallyLoaded = configurations.dependencyScope("embeddedRuntimeOnly")

/** The consumable configuration containing the resulting embedded elements. */
val embeddedResult = configurations.resolvable("embeddedResult") {
    extendsFrom(embeddedCompileOnly)
    extendsFrom(embeddedDynamicallyLoaded)
    exclude(module = "kotlin-stdlib")
}

/** Adds a dependency to the `embedded` configuration. */
fun DependencyHandler.embeddedCompileOnly(dependencyNotation: Any) = add(embeddedCompileOnly.name, dependencyNotation)
fun DependencyHandler.embeddedDynamicallyLoaded(dependencyNotation: Any) =
    add(embeddedDynamicallyLoaded.name, dependencyNotation)

@Suppress("AvoidDuplicateDependencies", "RedundantSuppression")
dependencies {
    embeddedCompileOnly("$group.compilerPlugin:base")
    // WORKAROUND https://youtrack.jetbrains.com/issue/KT-53477 – KGP misses transitive compiler plugin dependencies
    embeddedCompileOnly("$group:testBalloon-framework-shared:$version")

    val kotlinVersionLayers = (projectDir.toPath() / "layer").listDirectoryEntries("kotlin-*").map { it.name }
    for (kotlinVersionLayer in kotlinVersionLayers) {
        embeddedDynamicallyLoaded("$group.compilerPlugin:$kotlinVersionLayer")
    }

    project.configurations.named("compileOnly").configure { extendsFrom(embeddedCompileOnly) }
    compileOnly(libs.org.jetbrains.kotlin.stdlib)
    compileOnly(libs.org.jetbrains.kotlin.compiler)
}

buildConfig {
    packageName("buildConfig")
    useKotlinOutput { internalVisibility = true }

    buildConfigField("String", "PROJECT_GROUP_ID", "\"$group\"")
}

val integratedJar = tasks.register("integratedJar", Jar::class.java) {
    group = "build"
    description = "Creates the compiler plugin's integrated JAR, including embedded dependencies."

    // The following is required for CC compatibility (Project.zipTree(Object) cannot be used).
    val archiveOperations = run {
        abstract class InjectionTarget {
            @get:Inject
            abstract val archiveOperations: ArchiveOperations
        }
        project.objects.newInstance(InjectionTarget::class.java).archiveOperations
    }

    // Include the compiler plugin's classes.
    from(java.sourceSets.main.map { it.output })

    // Include the compiler plugin's embedded dependency classes, unzipping JARs.
    from(
        embeddedResult.map { embeddedResult ->
            embeddedResult.elements.map { elements ->
                elements.map { element ->
                    archiveOperations.zipTree(element.asFile).matching {
                        // Strip Kotlin metadata files to avoid compilation errors
                        //     "Module was compiled with an incompatible version of Kotlin"
                        // when an older compiler version encounters newer metadata in the compiler plugin JAR.
                        exclude("META-INF/*.kotlin_module")
                    }
                }
            }
        }
    )

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    archiveClassifier = "integrated"
}

// Publish the integrated JAR instead of the default one.
// See https://github.com/vanniktech/gradle-maven-publish-plugin/issues/1123#issuecomment-3670312723
configurations {
    for (configurationName in listOf("runtimeElements", "apiElements")) {
        named(configurationName) {
            outgoing {
                artifacts.clear()
                artifact(integratedJar)
            }
        }
    }
}

tasks.named("test") {
    (projectDir.toPath() / "layer").listDirectoryEntries().forEach {
        dependsOn(gradle.includedBuild(it.name).task(":test"))
    }
}

propagateLifecycleTasksToIncludedBuilds()
