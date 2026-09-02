import buildLogic.propagateLifecycleTasksToIncludedBuilds
import org.jetbrains.kotlin.assignment.plugin.gradle.AssignmentExtension
import org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension
import kotlin.io.path.div
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

plugins {
    id("buildLogic.kotlin-jvm")
    id("java-gradle-plugin")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.sam.with.receiver)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.assignment)
    id("buildLogic.publishing")
    alias(libs.plugins.com.github.gmazzo.buildconfig)
}

description = "Gradle plugin for the TestBalloon framework"

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
    embeddedCompileOnly("$group.gradlePlugin:base")
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

extensions.configure<SamWithReceiverExtension>("samWithReceiver") {
    annotation(HasImplicitReceiver::class.qualifiedName!!)
}

extensions.configure<AssignmentExtension>("assignment") {
    annotation(SupportsKotlinAssignmentOverloading::class.qualifiedName!!)
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
