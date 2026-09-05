import buildLogic.propagateLifecycleTasksToIncludedBuilds
import gradlePlugin.layer.buildLogic.configureGradlePlugin
import kotlin.io.path.div
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

plugins {
    id("gradlePlugin.layer.buildLogic.common")
    // noinspection NewerVersionAvailable
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
    id("java-gradle-plugin")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.sam.with.receiver)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.assignment)
    id("buildLogic.publishing")
    alias(libs.plugins.com.github.gmazzo.buildconfig)
}

description = "Gradle plugin for the TestBalloon framework"
group = "${project.property("local.PROJECT_GROUP_ID")}"

configureGradlePlugin("2.2.0")

/** Dependencies to be embedded into the Gradle plugin artifact. */
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

dependencies {
    embeddedCompileOnly("$group:testBalloon-framework-shared:$version")

    val kotlinVersionLayers = (projectDir.toPath() / "layer").listDirectoryEntries("kotlin-*").map { it.name }
    for (kotlinVersionLayer in kotlinVersionLayers) {
        embeddedDynamicallyLoaded("$group.gradlePlugin:$kotlinVersionLayer")
    }

    project.configurations.named("compileOnly").configure { extendsFrom(embeddedCompileOnly) }
    compileOnly(libs.org.jetbrains.kotlin.stdlib)
    compileOnly(libs.org.jetbrains.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("${project.property("local.PROJECT_COMPILER_PLUGIN_ID")}") {
            id = "${project.property("local.PROJECT_COMPILER_PLUGIN_ID")}"
            displayName = "TestBalloon Gradle plugin for multiplatform test discovery"
            description = displayName
            implementationClass = "$group.gradlePlugin.TestBalloonGradlePlugin"
        }
    }
}

buildConfig {
    packageName("buildConfig")
    useKotlinOutput { internalVisibility = true }

    buildConfigField(
        "String",
        "PROJECT_COMPILER_PLUGIN_ID",
        "\"${project.property("local.PROJECT_COMPILER_PLUGIN_ID")}\""
    )
    buildConfigField("String", "PROJECT_VERSION", "\"$version\"")
}

val integratedJar = tasks.register("integratedJar", Jar::class.java) {
    group = "build"
    description = "Creates the Gradle plugin's integrated JAR, including embedded dependencies."

    // The following is required for CC compatibility (Project.zipTree(Object) cannot be used).
    val archiveOperations = run {
        abstract class InjectionTarget {
            @get:Inject
            abstract val archiveOperations: ArchiveOperations
        }
        project.objects.newInstance(InjectionTarget::class.java).archiveOperations
    }

    // Include the Gradle plugin's classes.
    from(java.sourceSets.main.map { it.output })

    // Include the Gradle plugin's embedded dependency classes, unzipping JARs.
    from(
        embeddedResult.map { embeddedResult ->
            embeddedResult.elements.map { elements ->
                elements.map { element ->
                    archiveOperations.zipTree(element.asFile)
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

propagateLifecycleTasksToIncludedBuilds()
