@file:Suppress("NewApi")
@file:OptIn(TestBalloonInternalApi::class)

package de.infix.testBalloon.gradlePlugin.shared

import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.EnvironmentVariable
import de.infix.testBalloon.framework.shared.internal.ReportingMode
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test
import org.gradle.util.internal.VersionNumber
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestReport
import java.nio.file.DirectoryNotEmptyException
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.writeText

/**
 * Configures the project for TestBalloon, given the precondition that the compiler plugin artifacts are set up.
 */
internal fun Project.configureWithTestBalloon(
    testBalloonProperties: TestBalloonGradleProperties,
    browserSafeEnvironmentPatternFromExtension: () -> String = { "" }
) {
    configureTestTasks(testBalloonProperties, browserSafeEnvironmentPatternFromExtension)
    configureDiagnosticsTask()

    afterEvaluate {
        val nonIncrementalTestCompileTaskRegex = testBalloonProperties.nonIncrementalTestCompileTaskRegex
        tasks.withType(AbstractKotlinCompile::class.java).configureEach {
            if (nonIncrementalTestCompileTaskRegex.containsMatchIn(name)) {
                incremental = false
                if (this is Kotlin2JsCompile) {
                    @Suppress("INVISIBLE_REFERENCE")
                    incrementalJsKlib = false
                }
            }
        }
    }
}

/**
 * Configures the test tasks for TestBalloon.
 */
private fun Project.configureTestTasks(
    testBalloonProperties: TestBalloonGradleProperties,
    browserSafeEnvironmentPatternFromExtension: () -> String
) {
    val reportingMode = when (testBalloonProperties.reportingMode) {
        "intellij-legacy" -> if (providers.systemProperty("idea.active").isPresent) {
            ReportingMode.IntellijIdeaLegacy
        } else {
            ReportingMode.Files
        }

        "intellij" -> ReportingMode.IntellijIdea

        "files" -> ReportingMode.Files

        else -> if (providers.systemProperty("idea.active").isPresent) {
            ReportingMode.IntellijIdea
        } else {
            ReportingMode.Files
        }
    }

    val reportsEnabled = testBalloonProperties.reportsEnabled ?: (reportingMode == ReportingMode.Files)

    if (!reportsEnabled) {
        tasks.withType(AbstractTestTask::class.java).configureEach {
            reports.html.required.set(false)
            reports.junitXml.required.set(false)
        }
        tasks.withType(KotlinTestReport::class.java).configureEach {
            enabled = false
        }
    }

    val androidHostSideTestClassRegex = testBalloonProperties.androidHostSideTestClassRegex
    val junit4GradleAutoConfigurationEnabled = testBalloonProperties.junit4GradleAutoConfigurationEnabled ?: true
    val testBalloonPriorityIncludePatternsExist by lazy {
        System.getenv(EnvironmentVariable.TESTBALLOON_INCLUDE_PATTERNS.name)?.ifEmpty { null } != null
    }
    val jvmTestBalloonTestsOnly = testBalloonProperties.jvmTestBalloonTestsOnly ?: true
    val junitPlatformGradleAutoConfigurationEnabled =
        testBalloonProperties.junitPlatformGradleAutoConfigurationEnabled ?: true

    tasks.withType(Test::class.java).configureEach {
        // https://docs.gradle.org/current/userguide/java_testing.html
        val testClassName = this::class.qualifiedName?.removeSuffix("_Decorated") ?: ""
        if (androidHostSideTestClassRegex.containsMatchIn(testClassName)) {
            if (junit4GradleAutoConfigurationEnabled &&
                (jvmTestBalloonTestsOnly || testBalloonPriorityIncludePatternsExist)
            ) {
                useJUnit {
                    includeCategories(Constants.JUNIT4_RUNNER_CLASS_NAME)
                }
            }
        } else {
            if (junitPlatformGradleAutoConfigurationEnabled) {
                useJUnitPlatform {
                    if (jvmTestBalloonTestsOnly || testBalloonPriorityIncludePatternsExist) {
                        includeEngines(Constants.JUNIT_ENGINE_ID)
                    }
                }
            }
        }
    }

    val browserTestTaskRegex = testBalloonProperties.browserTestTaskRegex

    val reportingPathLimit =
        providers.environmentVariable(EnvironmentVariable.TESTBALLOON_REPORTING_PATH_LIMIT.name).orNull?.ifEmpty {
            null
        }
            ?: testBalloonProperties.reportingPathLimit?.toString()

    gradle.taskGraph.whenReady {
        // Why use `taskGraph.whenReady` at this point?
        // We want to
        // 1. access the test patterns provided by `AbstractTestTask.filter` options and `--tests` command line
        //    arguments, and
        // 2. mutate the test task to populate an environment variable or Karma configuration file with those patterns.
        //
        // What are the expected failure modes of using `taskGraph.whenReady`?
        // - If another plugin modifies test-related parameters in a `taskGraph.whenReady` block, they might not be
        //   picked up, depending on the order plugins are applied to the project.

        tasks.withType(AbstractTestTask::class.java).configureEach {
            fun testBalloonEnvironment(
                secondaryIncludePatterns: List<String>,
                secondaryExcludePatterns: List<String>
            ): Map<String, String> = buildMap {
                fun prioritizedPatterns(primary: EnvironmentVariable, secondary: Iterable<String>): String =
                    System.getenv(primary.name)?.ifEmpty { null }
                        ?: secondary.joinToString("${Constants.INTERNAL_PATH_PATTERN_SEPARATOR}")

                this[EnvironmentVariable.TESTBALLOON_INCLUDE_PATTERNS.name] =
                    prioritizedPatterns(
                        EnvironmentVariable.TESTBALLOON_INCLUDE_PATTERNS,
                        secondary = secondaryIncludePatterns
                    )

                this[EnvironmentVariable.TESTBALLOON_EXCLUDE_PATTERNS.name] =
                    prioritizedPatterns(
                        EnvironmentVariable.TESTBALLOON_EXCLUDE_PATTERNS,
                        secondary = secondaryExcludePatterns
                    )

                this[EnvironmentVariable.TESTBALLOON_REPORTING.name] = reportingMode.name
                if (reportingPathLimit != null) {
                    this[EnvironmentVariable.TESTBALLOON_REPORTING_PATH_LIMIT.name] = reportingPathLimit
                }
            }

            fun KotlinJsTest.configureKarmaEnvironment() {
                val directory = Path("${layout.projectDirectory}") / "karma.config.d"
                val parameterConfigFile = directory / "testBalloonParameters.js"

                val secondaryIncludePatterns = (
                    filter.includePatterns + (filter as DefaultTestFilter).commandLineIncludePatterns
                    ).toList()
                val secondaryExcludePatterns = filter.excludePatterns.toList()

                val browserSafeEnvironmentPatternStrings = listOf(
                    testBalloonProperties.browserSafeEnvironmentPattern,
                    browserSafeEnvironmentPatternFromExtension()
                )

                doFirst {
                    @Suppress("NewApi")
                    check(directory.exists() || directory.toFile().mkdirs()) {
                        "Could not create directory '$directory'"
                    }

                    val browserSafeEnvironmentPatterns = browserSafeEnvironmentPatternStrings.mapNotNull {
                        if (it.isEmpty()) null else it.toRegex()
                    }

                    // The environment propagated to the browser. TestBalloon's own entries have precedence.
                    val browserEnvironment =
                        System.getenv().filter { (name, _) ->
                            browserSafeEnvironmentPatterns.any { it.matches(name) }
                        } + testBalloonEnvironment(secondaryIncludePatterns, secondaryExcludePatterns)

                    val clientConfiguration = buildList {
                        add("config.client = config.client || {};")
                        add("config.client.env = {")

                        add(
                            browserEnvironment.map { (name, value) ->
                                val escapedValue = value
                                    .replace("\\", "\\\\")
                                    .replace("\"", "\\\"")
                                    .filter { it.code >= 0x20 }
                                """    $name: "$escapedValue""""
                            }.joinToString(separator = ",\n")
                        )

                        add("}")
                    }

                    parameterConfigFile.writeText(clientConfiguration.joinToString(separator = "\n"))
                }

                doLast {
                    @Suppress("NewApi")
                    if (parameterConfigFile.deleteIfExists()) {
                        try {
                            directory.deleteIfExists()
                        } catch (_: DirectoryNotEmptyException) {
                        }
                    }
                }
            }

            /**
             * Invokes [setTestEnvironment] to set up TestBalloon environment variables.
             */
            fun AbstractTestTask.configureEnvironment(setTestEnvironment: (name: String, value: String) -> Unit) {
                val secondaryIncludePatterns = (
                    filter.includePatterns + (filter as DefaultTestFilter).commandLineIncludePatterns
                    ).toList()
                val secondaryExcludePatterns = filter.excludePatterns.toList()

                doFirst {
                    for ((name, value) in testBalloonEnvironment(secondaryIncludePatterns, secondaryExcludePatterns)) {
                        setTestEnvironment(name, value)
                    }
                }
            }

            when (this) {
                is KotlinNativeTest -> {
                    configureEnvironment { name, value ->
                        environment(name, value, false)
                        environment("SIMCTL_CHILD_$name", value, false) // required for Apple simulator execution
                    }
                }

                is KotlinJsTest -> {
                    if (browserTestTaskRegex.containsMatchIn(name)) {
                        configureKarmaEnvironment()
                    } else {
                        configureEnvironment { name, value ->
                            environment(name, value)
                        }
                    }

                    // Reset Gradle test-filtering patterns in order to avoid conflicts with Mocha filtering.
                    if (testBalloonProperties.jsTestFilteringResetEnabled == true) {
                        (filter as DefaultTestFilter).commandLineIncludePatterns.clear()
                        filter.includePatterns.clear()
                        filter.excludePatterns.clear()
                        // Avoid Gradle error
                        //    "...no filters are applied, but the test task did not discover any tests to execute."
                        if (hasProperty("failOnNoDiscoveredTests")) {
                            setProperty("failOnNoDiscoveredTests", false)
                        }
                    }
                }

                is Test -> {
                    configureEnvironment { name, value ->
                        environment(name, value)
                    }

                    if (testBalloonProperties.jvmTestFilteringPatchEnabled == true) {
                        with(filter as DefaultTestFilter) {
                            for (patternSet in listOf(commandLineIncludePatterns, includePatterns)) {
                                if (patternSet.isNotEmpty()) {
                                    // If TestBalloon is used via JUnit 4, it would be accidentally excluded if any
                                    // pattern is present, as such patterns are assumed to specify test classes.
                                    // Remedy: Add a pattern for the JUnit 4 entry point just in case.
                                    patternSet.add(Constants.JUNIT4_ENTRY_POINT_SIMPLE_CLASS_NAME)
                                    // Work around Gradle prematurely declaring "No tests found for given includes".
                                    // If Gradle encounters an include pattern, it tries to compare it with classes it
                                    // loads on its own initiative. It may then decide that no class can possibly match
                                    // without asking any framework. With non-class based patterns, Gradle can
                                    // prematurely fail with the above error.
                                    // Remedy: Add a fake pattern which Gradle cannot base its decision on, and which
                                    // does not match anything.
                                    patternSet.add("*TestBalloonGradleGuardPattern")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Project.configureDiagnosticsTask() = afterEvaluate {
    val taskName = "testBalloonDiagnostics"
    val kotlinMultiplatformExtension =
        runCatching { extensions.findByName("kotlin") as? KotlinMultiplatformExtension }.getOrNull()

    tasks.register(taskName) {
        group = "help"
        description = "Shows diagnostics for the TestBalloon test framework"

        fun <Result : Any> safeProvider(value: () -> Result?): Provider<Result> = project.provider {
            try {
                value()
            } catch (throwable: Throwable) {
                println("WARNING: Problem configuring task step in '$taskName': $throwable")
                throwable.printStackTrace()
                null
            }
        }

        fun doLastSafely(block: () -> Unit) = doLast {
            try {
                block()
            } catch (throwable: Throwable) {
                println("WARNING: Problem executing task step in '$taskName': $throwable")
                throwable.printStackTrace()
            }
        }

        val projectPath = project.path
        val gradleVersion = gradle.gradleVersion

        doLastSafely {
            println()
            println("--- BEGIN TestBalloon diagnostics for $projectPath ---------------------------------")
            println()
            println("Project path: $projectPath")
            println("Gradle version: $gradleVersion")
        }

        val buildScriptPluginsBlock = safeProvider {
            val buildScriptText = project.buildscript.sourceFile?.readText()
            Regex("""plugins \{.*?\}""", option = RegexOption.DOT_MATCHES_ALL).find(buildScriptText ?: "")?.value
                ?: "no plugins block found in ${project.buildscript.sourceFile?.path ?: "(unknown build script)"}"
        }

        doLastSafely {
            println()
            println("Build script plugins block:")
            println(buildScriptPluginsBlock.get().prependIndent("  "))
        }

        val catalogsExtension = safeProvider {
            project.extensions.getByType(VersionCatalogsExtension::class.java)
        }
        val versionCatalogNames = catalogsExtension.map { it.catalogNames.toList() }
        val relevantPlugins = safeProvider {
            val catalogsExtension = catalogsExtension.get()
            val versionCatalogs = catalogsExtension.catalogNames.map { catalogsExtension.named(it) }
            val catalogPluginVersions = versionCatalogs.flatMap { catalog ->
                catalog.pluginAliases.map {
                    val (artifactId, version) = catalog.findPlugin(it).get().get().toString().split(':', limit = 2)
                    artifactId to version
                }
            }.toMap()

            mapOf(
                "de.infix.testBalloon" to null,
                "org.jetbrains.kotlin.jvm" to null,
                "org.jetbrains.kotlin.multiplatform" to null,
                "com.android.application" to "Sharing a module with KMP is unsupported.",
                "com.android.library" to "deprecated",
                "org.jetbrains.kotlin.android" to "Must not be used with built-in Kotlin of AGP 9.",
                "com.android.kotlin.multiplatform.library" to null
            ).mapNotNull { (pluginId, notice) ->
                pluginManager.findPlugin(pluginId)?.let {
                    val pluginAndVersion = "$pluginId:${catalogPluginVersions[pluginId]}"
                    if (notice != null) "$pluginAndVersion  (*) $notice" else pluginAndVersion
                }
            }
        }

        doLast {
            println()
            println("Gradle plugins (excerpt, versions from catalog(s) $versionCatalogNames):")
            println(relevantPlugins.get().joinToString(separator = "\n  ", prefix = "  "))
        }

        val relevantProperties = safeProvider {
            providers.gradlePropertiesPrefixedBy("testBalloon.").get() +
                providers.gradlePropertiesPrefixedBy("org.gradle.").get() +
                providers.gradlePropertiesPrefixedBy("kotlin.").get()
        }

        doLastSafely {
            println()
            println("Gradle properties (excerpt):")
            relevantProperties.get().toSortedMap(String::compareTo).forEach { (name, value) ->
                println("  $name=$value")
            }
        }

        val testBalloonVersionsUsage = safeProvider {
            configurations
                .filter { it.isCanBeResolved }
                .flatMap { configuration ->
                    val visitedComponentIds = mutableSetOf<ComponentIdentifier>()

                    fun ResolvedComponentResult.withAllChildren(): Sequence<ResolvedComponentResult> = sequence {
                        if (!visitedComponentIds.add(id)) return@sequence
                        yield(this@withAllChildren)
                        dependencies
                            .filterIsInstance<ResolvedDependencyResult>()
                            .forEach { dep ->
                                yieldAll(dep.selected.withAllChildren())
                            }
                    }

                    val rootComponent = configuration.incoming.resolutionResult.rootComponent.get()
                    val testBalloonVersions = rootComponent.dependencies
                        .filterIsInstance<ResolvedDependencyResult>()
                        .flatMap { resolvedDependencyResult ->
                            resolvedDependencyResult.selected.withAllChildren()
                        }
                        .mapNotNull { component ->
                            (component.id as? ModuleComponentIdentifier)?.let {
                                if (it.group == "de.infix.testBalloon") VersionNumber.parse(it.version) else null
                            }
                        }
                        .toSet()

                    testBalloonVersions.map {
                        Pair(it, configuration.name)
                    }
                }
                .groupBy({ it.first }) {
                    it.second
                }
        }

        doLastSafely {
            val testBalloonVersionsUsage = testBalloonVersionsUsage.get()

            println()
            println("TestBalloon dependencies:")
            for ((version, configurations) in testBalloonVersionsUsage) {
                println("  $version: ${configurations.joinToString(limit = 3)}")
            }
            when (testBalloonVersionsUsage.size) {
                0 -> println("  (*) External TestBalloon dependencies are not present in this module.")
                1 -> {}
                else -> println("  (*) TestBalloon dependencies with different versions are unsupported and may fail.")
            }
        }

        val testSourceSetsDiagram = safeProvider {
            kotlinMultiplatformExtension?.let { kotlin ->
                val testSourceSetRegex = Regex("[tT]est")

                val testSourceSets = kotlin.sourceSets.filter { testSourceSetRegex.containsMatchIn(it.name) }

                buildString {
                    appendLine()
                    appendLine("Diagram for display on https://mermaid.live/")
                    appendLine()
                    appendLine("---")
                    appendLine("title: KMP test source sets of ${project.path}")
                    appendLine("---")
                    appendLine("classDiagram")
                    for (sourceSet in testSourceSets) {
                        appendLine("    class ${sourceSet.name}")
                    }
                    for (sourceSet in testSourceSets) {
                        for (dependsOnSourceSet in sourceSet.dependsOn.map { it.name }) {
                            appendLine("    $dependsOnSourceSet <|-- ${sourceSet.name}")
                        }
                    }
                }
            }
        }

        doLastSafely {
            testSourceSetsDiagram.orNull?.let { print(it) }
        }

        doLastSafely {
            println()
            println("--- END TestBalloon diagnostics for $projectPath -----------------------------------")
        }
    }
}
