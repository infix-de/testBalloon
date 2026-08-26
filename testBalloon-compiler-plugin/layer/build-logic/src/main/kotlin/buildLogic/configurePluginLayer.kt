package buildLogic

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

/**
 * Configures the project for a compiler plugin layer compiled with [kotlinVersion].
 *
 * [baseLayerVersion] specifies the Kotlin version of the base layer dependency (optional).
 *
 * [kctforkVersion] specifies the version of
 * [ZacSweers/kotlin-compile-testing](https://github.com/ZacSweers/kotlin-compile-testing/releases)
 * to be used for testing.
 */
fun Project.configurePluginLayer(kotlinVersion: String, baseLayerVersion: String? = null, kctforkVersion: String) {
    description = "TestBalloon compiler plugin compatibility layer ($kotlinVersion)"

    group = project.property("local.PROJECT_GROUP_ID")!!

    with(pluginManager) {
        apply("org.jmailen.kotlinter")
    }

    extensions.configure<tapmoc.TapmocExtension>("tapmoc") {
        java(jdkVersion())
        kotlin(kotlinVersion)
        checkDependencies(tapmoc.Severity.ERROR)
    }

    with(dependencies) {
        val layerName = baseLayerVersion?.let { "kotlin-${it.replace(".", "-")}" } ?: "base"
        add("api", "de.infix.testBalloon:$layerName")

        add("compileOnly", "org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")

        add("testImplementation", "de.infix.testBalloon:base-test")
        add("testImplementation", "org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")
        add("testImplementation", "dev.zacsweers.kctfork:core:$kctforkVersion")
        add("testImplementation", "org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    }

    tasks.withType(Test::class.java).configureEach {
        useJUnitPlatform()
    }
}
