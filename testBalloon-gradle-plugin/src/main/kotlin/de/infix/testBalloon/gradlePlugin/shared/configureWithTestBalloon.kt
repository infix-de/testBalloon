@file:Suppress("NewApi")
@file:OptIn(TestBalloonInternalApi::class)

package de.infix.testBalloon.gradlePlugin.shared

import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.EnvironmentVariable
import de.infix.testBalloon.framework.shared.internal.ReportingMode
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import org.gradle.api.Project
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
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
}

/**
 * Configures the test tasks for TestBalloon.
 */
private fun Project.configureTestTasks(
    testBalloonProperties: TestBalloonGradleProperties,
    browserSafeEnvironmentPatternFromExtension: () -> String
) {
    val reportingMode = when (testBalloonProperties.reportingMode) {
        "intellij" -> ReportingMode.INTELLIJ_IDEA
        "files" -> ReportingMode.FILES
        else -> if (providers.systemProperty("idea.active").isPresent) {
            ReportingMode.INTELLIJ_IDEA
        } else {
            ReportingMode.FILES
        }
    }

    val reportsEnabled = testBalloonProperties.reportsEnabled ?: (reportingMode == ReportingMode.FILES)

    if (!reportsEnabled) {
        tasks.withType(AbstractTestTask::class.java).configureEach {
            reports.html.required.set(false)
            reports.junitXml.required.set(false)
        }
        tasks.withType(KotlinTestReport::class.java).configureEach {
            enabled = false
        }
    }

    val androidLocalTestClassRegex = testBalloonProperties.androidLocalTestClassRegex
    val junit4AutoIntegrationEnabled = testBalloonProperties.junit4AutoIntegrationEnabled ?: true
    val testBalloonPriorityIncludePatternsExist by lazy {
        System.getenv(EnvironmentVariable.TESTBALLOON_INCLUDE_PATTERNS.name)?.ifEmpty { null } != null
    }
    val jvmTestBalloonTestsOnly = testBalloonProperties.jvmTestBalloonTestsOnly ?: true
    val junitPlatformAutoconfigurationEnabled = testBalloonProperties.junitPlatformAutoconfigurationEnabled ?: true

    tasks.withType(Test::class.java).configureEach {
        // https://docs.gradle.org/current/userguide/java_testing.html
        val testClassName = this::class.qualifiedName?.removeSuffix("_Decorated") ?: ""
        if (androidLocalTestClassRegex.containsMatchIn(testClassName)) {
            if (junit4AutoIntegrationEnabled && (jvmTestBalloonTestsOnly || testBalloonPriorityIncludePatternsExist)) {
                useJUnit {
                    includeCategories(Constants.JUNIT4_RUNNER_CLASS_NAME)
                }
            }
        } else {
            if (junitPlatformAutoconfigurationEnabled) {
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
                fun prioritizedPatterns(vararg primary: EnvironmentVariable, secondary: Iterable<String>): String =
                    primary.firstNotNullOfOrNull { System.getenv(it.name)?.ifEmpty { null } }
                        ?: secondary.joinToString("${Constants.INTERNAL_PATH_PATTERN_SEPARATOR}")

                this[EnvironmentVariable.TESTBALLOON_INCLUDE_PATTERNS.name] =
                    prioritizedPatterns(
                        EnvironmentVariable.TESTBALLOON_INCLUDE_PATTERNS,
                        EnvironmentVariable.TEST_INCLUDE,
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
