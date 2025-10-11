package de.infix.testBalloon.framework.internal

import de.infix.testBalloon.framework.TestElement
import de.infix.testBalloon.framework.TestSuite
import de.infix.testBalloon.framework.testPlatform

/**
 * A [TestElement.Selection] based on lists of [includePatterns] and [excludePatterns].
 */
internal open class ListsBasedElementSelection protected constructor(
    private val includePatterns: List<Regex>,
    private val excludePatterns: List<Regex>,
    private val includePrefixes: List<String>
) : TestElement.Selection {
    protected constructor(includePatterns: String?, excludePatterns: String?) :
        this(
            includePatterns = includePatterns.toRegexList(),
            excludePatterns = excludePatterns.toRegexList(),
            includePrefixes = includePatterns.toPrefixList()
        )

    private var used = false

    override fun includes(testElement: TestElement): Boolean {
        if (!used) {
            logInfo { "${testPlatform.displayName}: Tests selected via $this" }
            used = true
        }

        return with(testElement) {
            (includePatterns.isEmpty() || includePatterns.any { it.matches(testElementPath.internalId) }) &&
                excludePatterns.none { it.matches(testElementPath.internalId) }
        }
    }

    override fun mayInclude(testSuite: TestSuite): Boolean = testSuite.isSessionOrCompartment ||
        includePrefixes.isEmpty() ||
        includePrefixes.any { includePrefix ->
            val pathId = testSuite.testElementPath.internalId
            if (pathId.length > includePrefix.length) {
                pathId.startsWith(includePrefix)
            } else {
                includePrefix.startsWith(pathId)
            }
        }

    override fun toString(): String =
        "${this::class.simpleName}(includePatterns=$includePatterns, excludePatterns=$excludePatterns)"

    companion object {
        /**
         * Returns regular expressions from a string of [PATH_PATTERN_SEPARATOR]-separated patterns with `*` wildcards.
         */
        private fun String?.toRegexList(): List<Regex> = toPatternList().map {
            try {
                buildString {
                    for (character in it) {
                        when (character) {
                            '*' -> append(".*")
                            in REGEX_META_CHARACTERS -> append("\\$character")
                            else -> append(character)
                        }
                    }
                }.toRegex()
            } catch (throwable: Throwable) {
                throw IllegalArgumentException("Could not convert regex pattern '$it'.", throwable)
            }
        }

        /**
         * Returns literal prefixes from a string of [PATH_PATTERN_SEPARATOR]-separated patterns with `*` wildcards.
         *
         * A literal prefix is the longest prefix of a pattern that is free of any wildcard. For example,
         * the literal prefix of "com.example.MySuite|sub-suite*|test2*" is "com.example.MySuite|sub-suite".
         * A trailing path segment separator is always dropped from a literal prefix.
         */
        private fun String?.toPrefixList(): List<String> = toPatternList().map {
            it.substringBefore('*').run {
                if (this.endsWith(INTERNAL_PATH_SEGMENT_SEPARATOR)) dropLast(1) else this
            }
        }.toSet().toList()

        private fun String?.toPatternList(): List<String> =
            this?.ifEmpty { null }?.split(PATH_PATTERN_SEPARATOR) ?: listOf()

        private val REGEX_META_CHARACTERS = "\\[].^$?+{}|()".toSet()
    }
}

/**
 * A [TestElement.Selection] created from command line arguments which define [includePatterns] and [excludePatterns].
 */
internal class ArgumentsBasedElementSelection(arguments: Array<String>) :
    ListsBasedElementSelection(
        includePatterns = arguments.optionValue("include"),
        excludePatterns = arguments.optionValue("exclude")
    ) {
    companion object {
        private fun Array<String>.optionValue(optionName: String): String? {
            val optionNameIndex = indexOfFirst { it == "--$optionName" }
            return if (optionNameIndex >= 0) getOrNull(optionNameIndex + 1) else null
        }
    }
}

/**
 * A [TestElement.Selection] created from environment variables which define [includePatterns] and [excludePatterns].
 */
internal class EnvironmentBasedElementSelection(
    includePatterns: String? = EnvironmentVariable.TESTBALLOON_INCLUDE.value(),
    excludePatterns: String? = EnvironmentVariable.TESTBALLOON_EXCLUDE.value()
) : ListsBasedElementSelection(includePatterns, excludePatterns)
