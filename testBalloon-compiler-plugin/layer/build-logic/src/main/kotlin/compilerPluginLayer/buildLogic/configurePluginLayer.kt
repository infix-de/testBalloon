package compilerPluginLayer.buildLogic

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import tapmoc.Severity
import tapmoc.TapmocExtension

/**
 * Configures the project for a compiler plugin layer compiled with [kotlinVersion].
 *
 * [baseLayer] specifies the project name of the base layer dependency (optional).
 *
 * [kctforkVersion] specifies the version of
 * [ZacSweers/kotlin-compile-testing](https://github.com/ZacSweers/kotlin-compile-testing/releases)
 * to be used for testing.
 */
fun Project.configurePluginLayer(kotlinVersion: String, baseLayer: String? = null, kctforkVersion: String) {
    description = "TestBalloon compiler plugin compatibility layer ($kotlinVersion)"

    val rootGroup = "${project.property("local.PROJECT_GROUP_ID")}"
    group = "$rootGroup.compilerPlugin"

    with(pluginManager) {
        apply("org.jmailen.kotlinter")
    }

    extensions.configure<TapmocExtension>("tapmoc") {
        java(jdkVersion())
        kotlin(kotlinVersion)
        checkDependencies(Severity.ERROR)
    }

    with(dependencies) {
        baseLayer?.let { add("api", "$group:$it") }

        add("compileOnly", "org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")

        add("testImplementation", "$rootGroup:base-test")
        add("testImplementation", "org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")
        add("testImplementation", "dev.zacsweers.kctfork:core:$kctforkVersion")
        add("testImplementation", "org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    }

    tasks.withType(Test::class.java).configureEach {
        useJUnitPlatform()
    }
}
