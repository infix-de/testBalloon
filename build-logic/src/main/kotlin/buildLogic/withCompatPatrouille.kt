package buildLogic

import compat.patrouille.CompatPatrouilleExtension
import compat.patrouille.Severity
import compat.patrouille.configureJavaCompatibility
import compat.patrouille.configureKotlinCompatibility
import org.gradle.api.Project
import org.gradle.api.plugins.PluginManager
import java.util.concurrent.ConcurrentHashMap

/**
 * Runs [applyPlugins] in a block which is to be guarded by CompatPatrouille.
 *
 * Multiple nested invocations are possible. This function makes sure that CompatPatrouille configures the
 * plugins at the outermost level.
 */
fun Project.withCompatPatrouille(applyPlugins: PluginManager.() -> Unit) {
    @Suppress("newApi")
    val nestingLevel = configurationNestingLevels.compute(this) { key, value ->
        value?.plus(1) ?: 0
    }

    pluginManager.applyPlugins()
    pluginManager.apply("com.gradleup.compat.patrouille")

    if (nestingLevel == 0) {
        configureWithCompatPatrouille()

        configurationNestingLevels.remove(this)
    }
}

fun Project.configureWithCompatPatrouille() {
    configureJavaCompatibility(versionFromCatalog("jdk").toInt())
    configureKotlinCompatibility(versionFromCatalog("org.jetbrains.kotlin"))

    extensions.configure<CompatPatrouilleExtension>("compatPatrouille") {
        // The next line is for debugging only
        // project.configurations.forEach { println("$project: configuration '$it'") }
        checkApiDependencies(Severity.ERROR)
        checkRuntimeDependencies(Severity.ERROR)
    }
}

private val configurationNestingLevels = ConcurrentHashMap<Project, Int>()
