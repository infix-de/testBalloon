package buildLogic

import de.infix.testBalloon.gradlePlugin.shared.TestBalloonGradleProperties
import de.infix.testBalloon.gradlePlugin.shared.configureWithTestBalloon
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.UnknownConfigurationException
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME

/**
 * Adds the configuration normally supplied by the project's own Gradle plugin.
 *
 * This enables the project's compiler plugin without loading it from a repository.
 */
fun Project.addTestBalloonPluginFromProject(compilerPluginDependency: Dependency, sharedDependency: Dependency) {
    val testBalloonProperties = TestBalloonGradleProperties(this)

    with(dependencies) {
        add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, compilerPluginDependency)
        try {
            add(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME, compilerPluginDependency)
            // WORKAROUND https://youtrack.jetbrains.com/issue/KT-53477 – KGP misses transitive compiler plugin
            //     dependencies
            add(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME, sharedDependency)
        } catch (_: UnknownConfigurationException) {
            // The configuration "kotlinNativeCompilerPluginClasspath" is unavailable with AGP9's built-in Kotlin.
        }
    }

    val junitPlatformLauncherDependentConfigurationRegex =
        testBalloonProperties.junitPlatformLauncherDependentConfigurationRegex

    configurations.configureEach {
        if (junitPlatformLauncherDependentConfigurationRegex.containsMatchIn(name)) {
            dependencies.add(
                project.dependencies.create(libraryFromCatalog("org.junit.platform.launcher"))
            )
        }
    }

    extensions.configure<HasConfigurableKotlinCompilerOptions<*>>("kotlin") {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-P",
                "plugin:de.infix.testBalloon:testModuleRegex=${testBalloonProperties.testModuleRegex}"
            )
        }
    }

    configureWithTestBalloon(testBalloonProperties)
}
