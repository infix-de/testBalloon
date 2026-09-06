package de.infix.testBalloon.gradlePlugin.internal

import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.DebugLevel
import de.infix.testBalloon.framework.shared.internal.EnvironmentVariable
import de.infix.testBalloon.gradlePlugin.TestBalloonGradleExtension
import de.infix.testBalloon.gradlePlugin.internal.buildConfig.BuildConfig.PROJECT_COMPILER_PLUGIN_ID
import de.infix.testBalloon.gradlePlugin.internal.buildConfig.BuildConfig.PROJECT_GROUP_ID
import de.infix.testBalloon.gradlePlugin.internal.buildConfig.BuildConfig.PROJECT_JUNIT_PLATFORM_LAUNCHER
import de.infix.testBalloon.gradlePlugin.internal.buildConfig.BuildConfig.PROJECT_VERSION
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
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestReport
import java.util.concurrent.atomic.AtomicBoolean

abstract class TestBalloonGradlePluginBase : KotlinCompilerPluginSupportPlugin {
    protected lateinit var extension: TestBalloonGradleExtension
    private val identificationLogged = AtomicBoolean(false)

    companion object {
        private const val DISPLAY_NAME = "Plugin $PROJECT_COMPILER_PLUGIN_ID"

        val testBalloonProperties: TestBalloonGradleProperties
            get() = _testBalloonProperties
                ?: throw IllegalStateException("The plugin '$PROJECT_COMPILER_PLUGIN_ID' must be applied")

        private var _testBalloonProperties: TestBalloonGradleProperties? = null
    }

    override fun apply(target: Project): Unit = with(target) {
        _testBalloonProperties = TestBalloonGradleProperties(this)
        extension = extensions.create(Constants.GRADLE_EXTENSION_NAME, TestBalloonGradleExtension::class.java)

        configureCompilations()
        configureJUnitPlatformLauncher()
        addEntryPointSourceFileIfNecessary()
        configureTestTasks()
        configureDiagnosticsTask()
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        // NOTE: `testBalloonProperties.isTestSourceSet` must be used in lazy task configuration or in `afterEvaluate`.
        // We rely on the documentation of `KotlinCompilerPluginSupportPlugin`, which states:
        // > the Kotlin plugin inspects the project model in an afterEvaluate handler.
        return testBalloonProperties.isTestSourceSet(
            kotlinCompilation.defaultSourceSet.name
        ).also { applies ->
            if (!applies) {
                kotlinCompilation.target.project.debugLog(
                    "$DISPLAY_NAME: [DEBUG] compiler plugin is not applicable" +
                        " ('${kotlinCompilation.defaultSourceSet.name}' is not a test source set)."
                )
            }
        }
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> =
        kotlinCompilation.target.project.provider {
            mapOf(
                "debugLevel" to extension.debugLevel,
                "junit4AutoIntegrationEnabled" to (
                    extension.junit4AutoIntegrationEnabled
                        ?: testBalloonProperties.junit4AutoIntegrationEnabled
                        ?: true
                    )
            ).map { (key, value) ->
                SubpluginOption(key, value.toString())
            }
        }

    override fun getCompilerPluginId(): String = PROJECT_COMPILER_PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = PROJECT_GROUP_ID,
        artifactId = "testBalloon-compiler-plugin",
        version = PROJECT_VERSION
    )

    private fun Project.configureCompilations() {
        val notIncrementallyCompilableTestSourceSetsRegex =
            testBalloonProperties.notIncrementallyCompilableTestSourceSetsRegex

        fun disablePluginOptions(sourceSetName: String) = listOf(
            "-P",
            "plugin:${Constants.COMPILER_PLUGIN_NAME}:disablingReason='$sourceSetName' is not a test source set"
        )

        fun KotlinCompilation<*>.configureForTestBalloon() {
            val sourceSetName = defaultSourceSet.name
            compileTaskProvider.configure {
                if (testBalloonProperties.isTestSourceSet(sourceSetName)) {
                    if (notIncrementallyCompilableTestSourceSetsRegex.containsMatchIn(sourceSetName)) {
                        if (this is AbstractKotlinCompile<*>) {
                            debugLog("Disabling incremental compilation for $project, task '$name'.")
                            incremental = false
                            if (this is Kotlin2JsCompile) {
                                @Suppress("INVISIBLE_REFERENCE")
                                incrementalJsKlib = false
                            }
                        }
                    }
                } else {
                    this.compilerOptions.freeCompilerArgs.addAll(disablePluginOptions(sourceSetName))
                }
            }
        }

        plugins.configureEach {
            if (this !is KotlinBasePlugin) return@configureEach

            extensions.configure<KotlinProjectExtension>("kotlin") {
                when (this) {
                    is KotlinMultiplatformExtension ->
                        targets.configureEach {
                            compilations.configureEach {
                                configureForTestBalloon()
                            }
                        }

                    is KotlinSingleTargetExtension<*> ->
                        target.compilations.configureEach {
                            configureForTestBalloon()
                        }
                }
            }
        }
    }

    private fun Project.configureJUnitPlatformLauncher() {
        configurations.configureEach {
            // Lazy configuration ensures that all plugins are applied, which is a prerequisite for
            // accessing `junitPlatformLauncherDependentConfigurationRegex`.
            if (testBalloonProperties.junitPlatformLauncherDependentConfigurationRegex.containsMatchIn(name)) {
                dependencies.addLater(
                    provider {
                        // Lazily configuring the dependency ensures that the extension is guaranteed to be present
                        // and the build script (including extension settings) has been completely evaluated.
                        debugLog("Adding JUnit Platform launcher to $this.")
                        project.dependencies.create(PROJECT_JUNIT_PLATFORM_LAUNCHER)
                    }
                )
            }
        }
    }

    /**
     * Adds TestBalloon's entry point source file to all test root source sets (such as "commonTest").
     */
    abstract fun Project.addEntryPointSourceFileIfNecessary()

    /**
     * Configures the test tasks for TestBalloon.
     */
    private fun Project.configureTestTasks() {
        val reportsEnabled = testBalloonProperties.reportsEnabled ?: (reportingMode.isGradleFiles)

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
                            includeEngines(Constants.JUNIT_PLATFORM_ENGINE_ID)
                        }
                    }
                }
            }
        }

        val browserTestTaskRegex = testBalloonProperties.browserTestTaskRegex

        gradle.taskGraph.whenReady {
            // Why use `taskGraph.whenReady` at this point?
            // We want to
            // 1. access the test patterns provided by `AbstractTestTask.filter` options and `--tests` command line
            //    arguments, and
            // 2. mutate the test task to populate an environment variable or Karma configuration file with those
            //    patterns.
            //
            // What are the expected failure modes of using `taskGraph.whenReady`?
            // - If another plugin modifies test-related parameters in a `taskGraph.whenReady` block, they might not be
            //   picked up, depending on the order plugins are applied to the project.

            tasks.withType(AbstractTestTask::class.java).configureEach {
                when (this) {
                    is KotlinNativeTest -> {
                        configureEnvironment(project) { name, value ->
                            environment(name, value, false)
                            environment("SIMCTL_CHILD_$name", value, false) // required for Apple simulator execution
                        }

                        val simulatorSafeEnvironmentPattern: Provider<String> = project.provider {
                            extension.simulatorSafeEnvironmentPattern
                                ?: testBalloonProperties.simulatorSafeEnvironmentPattern
                        }

                        doFirst {
                            // The environment propagated to the simulator. May be overridden by TestBalloon's own
                            // entries by the previously registered `doFirst` action of `configureEnvironment`, which
                            // actually runs _after_ this action.
                            val simulatorSafeEnvironmentPattern =
                                simulatorSafeEnvironmentPattern.get().ifEmpty { null }?.toRegex()
                            for ((name, value) in System.getenv()) {
                                if (simulatorSafeEnvironmentPattern?.containsMatchIn(name) == true) {
                                    environment(
                                        "SIMCTL_CHILD_$name",
                                        value,
                                        false
                                    ) // required for Apple simulator execution
                                }
                            }
                        }
                    }

                    is KotlinJsTest -> {
                        if (browserTestTaskRegex.containsMatchIn(name)) {
                            configureKarma(project = project)
                        } else {
                            configureEnvironment(project) { name, value ->
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
                        configureEnvironment(project) { name, value ->
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
                                        // If Gradle encounters an include pattern, it tries to compare it with classes
                                        // it loads on its own initiative. It may then decide that no class can possibly
                                        // match without asking any framework. With non-class based patterns, Gradle can
                                        // prematurely fail with the above error.
                                        // Remedy: Add a fake pattern which Gradle cannot base its decision on, and
                                        // which does not match anything.
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

    /**
     * Invokes [setTestEnvironment] to set up TestBalloon environment variables.
     */
    private fun AbstractTestTask.configureEnvironment(
        project: Project,
        setTestEnvironment: (name: String, value: String) -> Unit
    ) {
        val secondaryIncludePatterns = (
            filter.includePatterns + (filter as DefaultTestFilter).commandLineIncludePatterns
            ).toList()
        val secondaryExcludePatterns = filter.excludePatterns.toList()
        val testBalloonEnvironment = project.testBalloonEnvironment(
            secondaryIncludePatterns = secondaryIncludePatterns,
            secondaryExcludePatterns = secondaryExcludePatterns
        )

        doFirst {
            for ((name, value) in testBalloonEnvironment) {
                setTestEnvironment(name, value)
            }
        }
    }

    /**
     * Configures the Karma JS browser launcher.
     */
    abstract fun KotlinJsTest.configureKarma(project: Project)

    @Suppress("NewApi")
    private fun Project.configureDiagnosticsTask() {
        val taskName = "testBalloonDiagnostics"

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

            fun doLastSafely(block: () -> Unit) {
                doLast {
                    try {
                        block()
                    } catch (throwable: Throwable) {
                        println("WARNING: Problem executing task step in '$taskName': $throwable")
                        throwable.printStackTrace()
                    }
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
                project.extensions.findByType(VersionCatalogsExtension::class.java)
            }
            val versionCatalogNames = catalogsExtension.map { it.catalogNames.toList() }
            val relevantPlugins = safeProvider {
                val catalogsExtension = catalogsExtension.orNull
                val versionCatalogs =
                    catalogsExtension?.catalogNames?.map { catalogsExtension.named(it) } ?: emptyList()
                val catalogPluginVersions = versionCatalogs.flatMap { catalog ->
                    catalog.pluginAliases.map {
                        val (artifactId, version) = catalog.findPlugin(it).get().get().toString().split(':', limit = 2)
                        artifactId to version
                    }
                }.toMap()

                mapOf(
                    Constants.COMPILER_PLUGIN_NAME to null,
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
                println("Gradle plugins (excerpt, versions from catalog(s) ${versionCatalogNames.orNull ?: "(none)"}):")
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
                                (component.id as? ModuleComponentIdentifier)
                                    ?.takeIf { it.group == Constants.ARTIFACT_GROUP_ID }
                                    ?.let { VersionNumber.parse(it.version) }
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

                    else -> println(
                        "  (*) TestBalloon dependencies with different versions are unsupported and may fail."
                    )
                }
            }

            val testSourceSetsDiagram = safeProvider {
                project.extensions.findByType(KotlinProjectExtension::class.java)?.let { kotlin ->
                    val sourceSets = kotlin.sourceSets

                    buildString {
                        appendLine()
                        appendLine("Diagram for display on https://mermaid.live/")
                        appendLine()
                        appendLine("---")
                        appendLine("title: Source sets of ${project.path}")
                        appendLine("---")
                        appendLine("classDiagram")
                        for (sourceSet in sourceSets) {
                            appendLine("    class ${sourceSet.name}")
                        }
                        for (sourceSet in sourceSets) {
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

    protected fun browserAutoIntegrationEnabled(): Boolean =
        extension.browserAutoIntegrationEnabled ?: testBalloonProperties.browserAutoIntegrationEnabled ?: true

    protected fun Project.debugLog(message: String) {
        if (extension.debugLevel > DebugLevel.NONE) {
            if (!identificationLogged.getAndSet(true)) {
                debugLog("using ${this@TestBalloonGradlePluginBase::class.qualifiedName}")
            }
            logger.warn("$DISPLAY_NAME: [DEBUG] (${project.path}) $message")
        }
    }
}
