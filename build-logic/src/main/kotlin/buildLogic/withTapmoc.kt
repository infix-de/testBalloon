package buildLogic

import org.gradle.api.Project
import org.gradle.api.plugins.PluginManager
import tapmoc.Severity
import tapmoc.TapmocExtension
import tapmoc.configureJavaCompatibility
import tapmoc.configureKotlinCompatibility
import java.util.concurrent.ConcurrentHashMap

/**
 * Runs [applyPlugins] in a block which is to be guarded by CompatPatrouille.
 *
 * Multiple nested invocations are possible. This function makes sure that CompatPatrouille configures the
 * plugins at the outermost level.
 */
fun Project.withTapmoc(applyPlugins: PluginManager.() -> Unit) {
    @Suppress("newApi")
    val nestingLevel = configurationNestingLevels.compute(this) { _, value ->
        value?.plus(1) ?: 0
    }

    pluginManager.applyPlugins()
    pluginManager.apply("com.gradleup.tapmoc")

    if (nestingLevel == 0) {
        configureWithTapmoc()

        configurationNestingLevels.remove(this)
    }
}

fun Project.configureWithTapmoc() {
    configureJavaCompatibility(versionFromCatalog("jdk").toInt())
    configureKotlinCompatibility(versionFromCatalog("org.jetbrains.kotlin"))

    extensions.configure<TapmocExtension>("tapmoc") {
        checkApiDependencies(Severity.ERROR)
        checkRuntimeDependencies(Severity.ERROR)
    }
}

private val configurationNestingLevels = ConcurrentHashMap<Project, Int>()
