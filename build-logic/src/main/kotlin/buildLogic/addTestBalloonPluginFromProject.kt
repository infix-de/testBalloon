package buildLogic

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.plugin.NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestReport
import java.nio.file.DirectoryNotEmptyException
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.reflect.KProperty
import kotlin.text.ifEmpty
import kotlin.text.split

/**
 * Adds the configuration normally supplied by the project's own Gradle plugin.
 *
 * This enables the project's compiler plugin without loading it from a repository.
 */
fun Project.addTestBalloonPluginFromProject(compilerPluginDependency: Dependency, abstractionsDependency: Dependency) {
    with(dependencies) {
        add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, compilerPluginDependency)
        add(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME, compilerPluginDependency)
        // WORKAROUND https://youtrack.jetbrains.com/issue/KT-53477 – KGP misses transitive compiler plugin dependencies
        add(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME, abstractionsDependency)
    }

    class PluginProperties(val project: Project) {

        /** Name pattern for test root source sets which will receive generated entry point code. */
        val testRootSourceSetRegex by regexProperty(
            """^(test$|commonTest$|androidTest|androidInstrumentedTest)"""
        )

        /**
         * Name pattern for test compilations in which the compiler plugin will look up test suites and a test session.
         *
         * The Gradle plugin will only apply the compiler plugin for compilations matching this pattern.
         */
        val testCompilationRegex by regexProperty("""(^test)|Test""")

        /**
         * Name pattern for test runtime-only configurations which will receive a JUnit Platform launcher dependency.
         */
        val testRuntimeOnlyConfigurationRegex by regexProperty("""^(test|jvmTest)RuntimeOnly$""")

        /**
         * Name pattern for test modules in which the compiler plugin will look up test suites and a test session.
         *
         * The Compiler plugin will disable itself for modules not matching this pattern.
         */
        val testModuleRegex by stringProperty("""(_test|Test)$""")

        /**
         * Test reporting mode. One of `auto` (default), `intellij`, `files`.
         *
         * The mode `intellij` supplies full test element paths to the reporting infrastructure, supporting proper
         * hierarchy display in IntelliJ's test run window.
         *
         * The mode `files` supplies test element names instead of full paths, supporting proper XML and HTML report
         * files, avoiding duplicate path segments leading to `file name too long' errors.
         *
         * `auto` detects whether tests run under IntelliJ IDEA and chooses the mode accordingly.
         */
        val reportingMode by stringProperty("auto")

        /**
         * Setting to enable or disable file-based reports. One of `auto` (default), `true`, `false`.
         *
         * The `auto` setting disables reports if tests run under IntelliJ or the [reportingMode] is `intellij`.
         */
        val reportsEnabled by booleanProperty("auto")

        @Suppress("SameParameterValue")
        private fun stringProperty(default: String) = Delegate(default) { it }

        private fun regexProperty(default: String) = Delegate(default) { Regex(it) }

        @Suppress("SameParameterValue")
        private fun booleanProperty(default: String) = Delegate(default) { it.toBooleanStrictOrNull() }

        inner class Delegate<Result>(val default: String, val conversion: (String) -> Result) {
            operator fun getValue(thisRef: Any?, property: KProperty<*>): Result =
                conversion(project.findProperty("testBalloon.${property.name}")?.toString() ?: default)
        }
    }

    val pluginProperties = PluginProperties(this)

    val generateTestBalloonInitializationTask = tasks.register("generateTestBalloonInitialization") {
        val generatedCommonTestDir = layout.buildDirectory.dir("generated/testBalloon/src/commonTest")
        outputs.dir(generatedCommonTestDir)
        doLast {
            val directory = Path("${generatedCommonTestDir.get()}/kotlin")
            @Suppress("NewApi")
            check(directory.exists() || directory.toFile().mkdirs()) { "Could not create directory '$directory'" }
            (directory / "EntryPointAnchor.kt").writeText(
                """
                        package de.infix.testBalloon.framework.internal.entryPoint

                        // This file was generated by buildLogic/addTestBalloonPluginFromProject.kt.
                        // The compiler plugin will populate it with entry point code.

                """.trimIndent()
            )
        }
    }

    afterEvaluate {
        // Why use afterEvaluate at this point?
        // In order to reliably detect all top-level source sets, we must be sure that the source set hierarchy
        // has been completely set up. Otherwise, `dependsOn.isEmpty()` would detect false positives.
        extensions.configure<KotlinBaseExtension>("kotlin") {
            val testRootSourceSetRegex = pluginProperties.testRootSourceSetRegex
            sourceSets.configureEach {
                if (testRootSourceSetRegex.containsMatchIn(name) && dependsOn.isEmpty()) {
                    kotlin.srcDir(generateTestBalloonInitializationTask)
                }
            }
        }
    }

    val reportingMode = when (pluginProperties.reportingMode) {
        "intellij" -> TestReportingMode.INTELLIJ_IDEA
        "files" -> TestReportingMode.FILES
        else -> if (providers.systemProperty("idea.active").isPresent) {
            TestReportingMode.INTELLIJ_IDEA
        } else {
            TestReportingMode.FILES
        }
    }

    val reportsEnabled = pluginProperties.reportsEnabled ?: (reportingMode == TestReportingMode.FILES)

    if (!reportsEnabled) {
        tasks.withType(AbstractTestTask::class.java).configureEach {
            reports.html.required.set(false)
            reports.junitXml.required.set(false)
        }
        tasks.withType(KotlinTestReport::class.java).configureEach {
            enabled = false
        }
    }

    tasks.withType(Test::class.java) {
        // https://docs.gradle.org/current/userguide/java_testing.html
        useJUnitPlatform()

        // Ask Gradle to skip scanning for test classes. We don't need it as our compiler plugin already
        // knows. Does this make a difference? I don't know.
        isScanForTestClasses = false
    }

    val testRuntimeOnlyConfigurationRegex = pluginProperties.testRuntimeOnlyConfigurationRegex

    configurations.configureEach {
        if (testRuntimeOnlyConfigurationRegex.containsMatchIn(name)) {
            dependencies.add(
                project.dependencies.create(libraryFromCatalog("org.junit.platform.launcher"))
            )
        }
    }

    afterEvaluate {
        tasks.withType(AbstractTestTask::class.java).configureEach {
            fun KotlinJsTest.configureKarmaEnvironment() {
                val directory = Path("${layout.projectDirectory}") / "karma.config.d"
                val parameterConfigFile = directory / "testBalloonParameters.js"

                /** Returns the prioritized patterns as a JS source string. */
                fun prioritizedPatterns(vararg primary: EnvironmentVariable, secondary: Iterable<String>): String {
                    val patterns = primary.firstNotNullOfOrNull {
                        project.providers.environmentVariable(it.name).orNull?.ifEmpty { null }
                            ?.split("$PATH_PATTERN_SEPARATOR")
                    } ?: secondary

                    return patterns.joinToString("$PATH_PATTERN_SEPARATOR", prefix = "\"", postfix = "\"") {
                        it.replace("\"", "\\\"")
                    }
                }

                val includePatternsJs = prioritizedPatterns(
                    EnvironmentVariable.TESTBALLOON_INCLUDE,
                    EnvironmentVariable.TEST_INCLUDE,
                    secondary = filter.includePatterns +
                        (filter as DefaultTestFilter).commandLineIncludePatterns
                )
                val excludePatternsJs = prioritizedPatterns(
                    EnvironmentVariable.TESTBALLOON_EXCLUDE,
                    secondary = filter.excludePatterns
                )

                doFirst {
                    @Suppress("NewApi")
                    check(directory.exists() || directory.toFile().mkdirs()) {
                        "Could not create directory '$directory'"
                    }

                    parameterConfigFile.writeText(
                        """
                                config.client = config.client || {};
                                config.client.env = {
                                    ${EnvironmentVariable.TESTBALLOON_INCLUDE.name}: $includePatternsJs,
                                    ${EnvironmentVariable.TESTBALLOON_EXCLUDE.name}: $excludePatternsJs,
                                    ${EnvironmentVariable.TESTBALLOON_REPORTING.name}: "$reportingMode"
                                }
                        """.trimIndent()
                    )
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

            fun configureEnvironment(setTestEnvironment: (variable: EnvironmentVariable, value: String) -> Unit) {
                fun prioritizedPatterns(vararg primary: EnvironmentVariable, secondary: Iterable<String>): String =
                    primary.firstNotNullOfOrNull {
                        project.providers.environmentVariable(it.name).orNull?.ifEmpty { null }
                    }
                        ?: secondary.joinToString("$PATH_PATTERN_SEPARATOR")

                setTestEnvironment(
                    EnvironmentVariable.TESTBALLOON_INCLUDE,
                    prioritizedPatterns(
                        EnvironmentVariable.TESTBALLOON_INCLUDE,
                        EnvironmentVariable.TEST_INCLUDE,
                        secondary = filter.includePatterns +
                            (filter as DefaultTestFilter).commandLineIncludePatterns
                    )
                )
                setTestEnvironment(
                    EnvironmentVariable.TESTBALLOON_EXCLUDE,
                    prioritizedPatterns(EnvironmentVariable.TESTBALLOON_EXCLUDE, secondary = filter.excludePatterns)
                )
                setTestEnvironment(EnvironmentVariable.TESTBALLOON_REPORTING, reportingMode.name)
            }

            when (this) {
                is KotlinNativeTest -> {
                    configureEnvironment { variable, value ->
                        environment(variable.name, value, false)
                    }
                }

                is KotlinJsTest -> {
                    if (testFramework is KotlinKarma) {
                        configureKarmaEnvironment()
                    } else {
                        configureEnvironment { variable, value ->
                            environment(variable.name, value)
                        }
                    }
                }

                is Test -> {
                    configureEnvironment { variable, value ->
                        environment(variable.name, value)
                    }
                }
            }
        }
    }
}

private enum class TestReportingMode {
    INTELLIJ_IDEA,
    FILES
}

private const val PATH_PATTERN_SEPARATOR: Char = '⬥'

private enum class EnvironmentVariable {
    @Deprecated("To be removed", ReplaceWith("TESTBALLOON_INCLUDE"))
    TEST_INCLUDE,

    TESTBALLOON_INCLUDE,
    TESTBALLOON_EXCLUDE,
    TESTBALLOON_REPORTING
}
