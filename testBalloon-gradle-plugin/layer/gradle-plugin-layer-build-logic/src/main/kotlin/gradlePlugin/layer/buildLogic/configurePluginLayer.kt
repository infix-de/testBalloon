package gradlePlugin.layer.buildLogic

import org.gradle.api.HasImplicitReceiver
import org.gradle.api.Project
import org.gradle.api.SupportsKotlinAssignmentOverloading
import org.gradle.api.tasks.testing.Test
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.jetbrains.kotlin.assignment.plugin.gradle.AssignmentExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension
import tapmoc.Severity
import tapmoc.TapmocExtension

/**
 * Configures the project for a Gradle plugin layer compatible with Kotlin Gradle Plugin [kotlinVersion].
 *
 * [baseLayer] specifies the project name of the base layer dependency (optional).
 */
fun Project.configurePluginLayer(kotlinVersion: String, baseLayer: String? = null) {
    description = "TestBalloon Gradle plugin compatibility layer ($kotlinVersion)"

    group = "${project.property("local.PROJECT_GROUP_ID")}.gradlePlugin"

    val kotlinVersionId = "kotlin${kotlinVersion.replace(Regex("[.-]"), "")}"
    val classVersionId = kotlinVersion.replace(Regex("[.-]"), "_")

    configureGradlePlugin(kotlinVersion)

    extensions.configure<GradlePluginDevelopmentExtension>("gradlePlugin") {
        plugins {
            register("${project.property("local.PROJECT_COMPILER_PLUGIN_ID")}.gradlePlugin.$kotlinVersionId") {
                id = "${project.property("local.PROJECT_COMPILER_PLUGIN_ID")}.gradlePlugin.$kotlinVersionId"
                displayName = "TestBalloon compiler plugin (for Kotlin $kotlinVersion and above)"
                description = displayName
                implementationClass = "$group.layer.$kotlinVersionId.TestBalloonGradlePlugin_$classVersionId"
            }
        }
    }

    with(dependencies) {
        baseLayer?.let { add("api", "$group:$it") }

        add("compileOnly", "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        add("compileOnly", libraryFromCatalog("com.android.gradle.plugin"))
    }

    tasks.withType(Test::class.java).configureEach {
        useJUnitPlatform()
    }
}

/**
 * Configures the project for a Gradle plugin based on [kotlinVersion].
 */
fun Project.configureGradlePlugin(kotlinVersion: String) {
    with(pluginManager) {
        apply("org.jmailen.kotlinter")
        apply("org.jetbrains.kotlin.plugin.sam.with.receiver")
        apply("org.jetbrains.kotlin.plugin.assignment")
    }

    extensions.configure<TapmocExtension>("tapmoc") {
        java(gradleJdkVersion())
        kotlin(kotlinVersion)
        checkKotlinStdlibs(Severity.ERROR)
        checkDependencies(Severity.ERROR)
    }

    extensions.configure<KotlinJvmExtension>("kotlin") {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-opt-in=de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi"
            )
        }
    }

    extensions.configure<SamWithReceiverExtension>("samWithReceiver") {
        annotation(HasImplicitReceiver::class.qualifiedName!!)
    }

    extensions.configure<AssignmentExtension>("assignment") {
        annotation(SupportsKotlinAssignmentOverloading::class.qualifiedName!!)
    }
}
