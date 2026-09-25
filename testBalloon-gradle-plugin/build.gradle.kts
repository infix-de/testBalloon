import buildLogic.propagateLifecycleTasksToIncludedBuilds
import buildLogic.rootGroup
import gradlePlugin.layer.buildLogic.configureGradlePlugin
import gradlePlugin.layer.buildLogic.gradlePluginId

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

description = "TestBalloon Gradle plugin"
group = rootGroup

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
    embeddedCompileOnly("$rootGroup:testBalloon-framework-shared")

    gradle.includedBuilds.filter { it.name.startsWith("kotlin-") }.forEach {
        embeddedDynamicallyLoaded("$group.gradlePlugin:${it.name}")
    }

    project.configurations.named("compileOnly").configure { extendsFrom(embeddedCompileOnly) }
    compileOnly(libs.org.jetbrains.kotlin.stdlib)
    compileOnly(libs.org.jetbrains.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        val pluginId = rootGroup
        register(pluginId) {
            id = pluginId
            displayName = "TestBalloon Gradle plugin for multiplatform test discovery"
            description = displayName
            implementationClass = "$rootGroup.gradlePlugin.TestBalloonGradlePlugin"
        }
    }
}

buildConfig {
    packageName("$group.gradlePlugin.internal.buildConfig")
    useKotlinOutput { internalVisibility = true }

    buildConfigField("String", "PROJECT_GRADLE_PLUGIN_ID", "\"$gradlePluginId\"")
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
