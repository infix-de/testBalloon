package de.infix.testBalloon.framework.core.internal

import de.infix.testBalloon.framework.core.TestElement
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.EnvironmentVariable
import de.infix.testBalloon.framework.shared.internal.safeAsInternalId
import kotlin.text.iterator

/**
 * A [TestElement.Selection] based on lists of [includePatterns] and [excludePatterns].
 */
internal open class ListsBasedElementSelection protected constructor(
    private val includePatterns: List<Regex>,
    private val excludePatterns: List<Regex>,
    private val includePrefixes: List<String>
) : TestElement.Selection {
    protected constructor(includePatterns: List<String>, excludePatterns: List<String>) :
        this(
            includePatterns = includePatterns.map { it.patternToRegex() },
            excludePatterns = excludePatterns.map { it.patternToRegex() },
            includePrefixes = includePatterns.map { it.patternToPrefix() }.toSet().toList()
        )

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
         * Returns regular expressions from a string of path patterns with `*` wildcards.
         *
         * The first character of each pattern may define an input separator (the default of which is '|').
         */
        private fun String?.toRegexList(): List<Regex> = toPatternList().map { it.patternToRegex() }

        private fun String.patternToRegex() = try {
            var inputElementSeparator: Char? = null
            buildString {
                for (character in this@patternToRegex.safeAsInternalId()) {
                    // If the first character is not a letter, use it as an element separator, which will then
                    // be transformed into the framework's internal separator.
                    if (inputElementSeparator == null) {
                        if (character.definesSeparator()) {
                            inputElementSeparator = character
                            continue
                        } else {
                            inputElementSeparator = DEFAULT_INPUT_ELEMENT_SEPARATOR
                        }
                    }
                    when (character) {
                        inputElementSeparator -> append(Constants.INTERNAL_PATH_ELEMENT_SEPARATOR)
                        '*' -> append(".*")
                        in REGEX_META_CHARACTERS -> append("\\$character")
                        else -> append(character)
                    }
                }
            }.toRegex()
        } catch (throwable: Throwable) {
            throw IllegalArgumentException("Could not convert regex pattern '$this'.", throwable)
        }

        /**
         * Returns literal prefixes from a string of path patterns with `*` wildcards.
         *
         * A literal prefix is the longest prefix of a pattern that is free of any wildcard. For example,
         * the literal prefix of "com.example.MySuite|sub-suite*|test2*" is "com.example.MySuite|sub-suite".
         *
         * Also considered:
         * - The first character of each pattern may define an input separator (the default of which is '|').
         * - A trailing path element separator is always dropped from a literal prefix.
         */
        private fun String?.toPrefixList(): List<String> = toPatternList().map { it.patternToPrefix() }.toSet().toList()

        private fun String.patternToPrefix(): String {
            var result = substringBefore('*')
            var inputElementSeparator: Char = DEFAULT_INPUT_ELEMENT_SEPARATOR
            if (result.firstOrNull().definesSeparator()) {
                inputElementSeparator = result.first()
                result = result.drop(1)
            }
            result = result.replace(inputElementSeparator, Constants.INTERNAL_PATH_ELEMENT_SEPARATOR)
            if (result.endsWith(Constants.INTERNAL_PATH_ELEMENT_SEPARATOR)) result = result.dropLast(1)
            return result.safeAsInternalId()
        }

        private fun String?.toPatternList(): List<String> =
            this?.ifEmpty { null }?.split(Constants.INTERNAL_PATH_PATTERN_SEPARATOR) ?: listOf()

        private fun Char?.definesSeparator() = this != null && !isLetter() && this != '*'

        private val REGEX_META_CHARACTERS = "\\[].^$?+{}|()".toSet()
        private const val DEFAULT_INPUT_ELEMENT_SEPARATOR = '|'
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
    includePatterns: String? = EnvironmentVariable.TESTBALLOON_INCLUDE_PATTERNS.value(),
    excludePatterns: String? = EnvironmentVariable.TESTBALLOON_EXCLUDE_PATTERNS.value()
) : ListsBasedElementSelection(includePatterns, excludePatterns) {
    companion object {
        internal fun isAvailable(): Boolean = EnvironmentVariable.TESTBALLOON_INCLUDE_PATTERNS.value() != null
    }
}
