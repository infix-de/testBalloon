package buildLogic

import de.infix.testBalloon.gradlePlugin.shared.TestBalloonGradleProperties
import de.infix.testBalloon.gradlePlugin.shared.configureWithTestBalloon
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
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
        add(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME, compilerPluginDependency)
        // WORKAROUND https://youtrack.jetbrains.com/issue/KT-53477 – KGP misses transitive compiler plugin dependencies
        add(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME, sharedDependency)
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

    configureWithTestBalloon(testBalloonProperties)
}
