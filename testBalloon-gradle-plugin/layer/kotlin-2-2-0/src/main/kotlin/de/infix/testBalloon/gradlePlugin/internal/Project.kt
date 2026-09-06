package de.infix.testBalloon.gradlePlugin.internal

import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.EnvironmentVariable
import de.infix.testBalloon.framework.shared.internal.ReportingMode
import org.gradle.api.Project
import org.gradle.util.internal.VersionNumber
import kotlin.text.ifEmpty

val Project.reportingMode: ReportingMode
    get() {
        // Gradle 9.3.0 introduces hierarchical test results reporting.
        // See https://docs.gradle.org/9.3.0/release-notes.html#test-reporting-improvements
        val gradleSupportsNesting = VersionNumber.parse(gradle.gradleVersion) >= VersionNumber.version(9, 3)
        val gradleFilesMode = if (gradleSupportsNesting) {
            ReportingMode.GradleFilesWithNesting
        } else {
            ReportingMode.GradleFilesWithoutNesting
        }
        val gradleIntellijIdeaMode = if (gradleSupportsNesting) {
            ReportingMode.GradleIntellijIdeaWithNesting
        } else {
            ReportingMode.GradleIntellijIdeaWithoutNesting
        }

        return when (TestBalloonGradlePluginBase.testBalloonProperties.reportingMode) {
            "intellij-legacy" -> if (providers.systemProperty("idea.active").isPresent) {
                ReportingMode.GradleIntellijIdeaLegacy
            } else {
                gradleFilesMode
            }

            "intellij" -> gradleIntellijIdeaMode

            "files" -> gradleFilesMode

            else -> if (providers.systemProperty("idea.active").isPresent) {
                gradleIntellijIdeaMode
            } else {
                gradleFilesMode
            }
        }
    }

val Project.reportingPathLimit: String?
    get() =
        providers.environmentVariable(EnvironmentVariable.TESTBALLOON_REPORTING_PATH_LIMIT.name).orNull?.ifEmpty {
            null
        }
            ?: TestBalloonGradlePluginBase.testBalloonProperties.reportingPathLimit?.toString()

/**
 * Returns `TESTBALLOON_*` environment variable settings as a map of `name` to `value`.
 */
fun Project.testBalloonEnvironment(
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
    reportingPathLimit?.let {
        this[EnvironmentVariable.TESTBALLOON_REPORTING_PATH_LIMIT.name] = it
    }
}
