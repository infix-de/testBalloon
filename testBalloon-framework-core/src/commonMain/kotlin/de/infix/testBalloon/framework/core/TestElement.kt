package de.infix.testBalloon.framework.core

import de.infix.testBalloon.framework.core.internal.TestSetupReport
import de.infix.testBalloon.framework.core.internal.reportingPathLimit
import de.infix.testBalloon.framework.shared.AbstractTestElement
import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.ReportingMode
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

public sealed class TestElement(parent: TestSuite?, name: String, displayName: String = name, testConfig: TestConfig) :
    AbstractTestElement {

    init {
        fun elementInfo() = if (parent == null) "a test element" else "a test element in $parent"
        require(name.isNotEmpty() && name.isNotBlank()) {
            "Could not register ${elementInfo()} with an empty or blank name '$name'"
        }
        require(displayName.isNotEmpty() && displayName.isNotBlank()) {
            "Could not register ${elementInfo()} with an empty or blank displayName '$displayName'"
        }
    }

    /**
     * The element's configuration.
     *
     * Note: Using `testSuite(...) { testConfig = ... }` is unsafe and will be removed.
     */
    @Suppress("CanBePrimaryConstructorProperty")
    @Deprecated(
        "Use 'testSuite(..., testConfig = ...)' instead. Scheduled for removal in TestBalloon 0.8."
    )
    public var testConfig: TestConfig = testConfig

    internal val testElementParent: TestSuite? = parent
    internal val testElementName: String = parent?.uniqueChildName(name, TestSuite.ChildNameType.Element) ?: name
    internal val testElementDisplayName: String =
        parent?.uniqueChildName(displayName.lengthLimited(), TestSuite.ChildNameType.Display)
            ?: displayName.lengthLimited()

    /**
     * A path uniquely identifying a test element in its test hierarchy.
     */
    public class Path internal constructor(private val element: TestElement) : AbstractTestElement.Path {
        override fun toString(): String = "«$internalId»"

        /**
         * This path's internal ID, directly derived from element names.
         */
        internal val internalId: String by lazy {
            flattened(separator = INTERNAL_PATH_ELEMENT_SEPARATOR_STRING) { testElementName }
        }

        /**
         * This path's fully qualified reporting name, including the top-level package name, if present.
         */
        internal val fullyQualifiedReportingName: String by lazy {
            flattened(separator = REPORTING_SEPARATOR) { externalizedName(fullyQualified = true) }
        }

        /**
         * This path's partially qualified reporting name, including a top-level package name.
         */
        internal val partiallyQualifiedReportingName: String by lazy {
            flattened(separator = REPORTING_SEPARATOR) { externalizedName(fullyQualified = false) }
        }

        /**
         * This path's partially qualified reporting name, including a top-level package name.
         */
        internal val qualifiedReportingNameBelowTopLevel: String by lazy {
            flattened(separator = REPORTING_SEPARATOR, topLevelStrippingEnabled = true) {
                externalizedName(fullyQualified = false)
            }
        }

        /**
         * This path element's reporting name.
         */
        internal val elementReportingName: String get() = element.externalizedName(fullyQualified = false)

        private fun TestElement.externalizedName(fullyQualified: Boolean): String =
            if (isTopLevelSuite && fullyQualified) {
                // Do not escape fully qualified element names for top-level suites as reporting tools extract a
                // package name.
                // NOTE: If users explicitly supply element names without a package prefix, reports might deviate from
                // expectations.
                testElementName.safeAsSuiteDisplayName()
            } else {
                if (this is Test) {
                    testElementDisplayName.safeAsTestDisplayName()
                } else {
                    // lower-level suite
                    testElementDisplayName.safeAsLowerLevelSuiteDisplayName()
                }
            }

        private fun flattened(
            separator: String,
            topLevelStrippingEnabled: Boolean = false,
            elementName: TestElement.() -> String
        ): String = if (element.testElementParent == null ||
            element.isTopLevelSuite ||
            (topLevelStrippingEnabled && element.testElementParent.isTopLevelSuite)
        ) {
            element.elementName()
        } else {
            buildString {
                append(
                    element.testElementParent.testElementPath.flattened(
                        separator = separator,
                        topLevelStrippingEnabled = topLevelStrippingEnabled,
                        elementName = elementName
                    )
                )
                append(separator)
                append(elementName(element))
            }
        }

        internal companion object {
            internal const val REPORTING_SEPARATOR_LENGTH = REPORTING_SEPARATOR.length
            private const val INTERNAL_PATH_ELEMENT_SEPARATOR_STRING = "${Constants.INTERNAL_PATH_ELEMENT_SEPARATOR}"
        }
    }

    override val testElementPath: Path = Path(this)

    internal val isSessionOrCompartment: Boolean = parent == null || this is TestCompartment

    internal val isTopLevelSuite: Boolean = parent is TestCompartment

    override val testElementIsEnabled: Boolean get() = parameters.isEnabled

    /** Indicates whether the element included by the session's [Selection]? */
    internal var isIncluded: Boolean = true

    @Suppress("ktlint:standard:backing-property-naming")
    private var _parameters: Parameters? = null

    internal var parameters: Parameters
        get() = _parameters ?: Parameters.default
        set(value) {
            require(_parameters == null) { "$this: parameters may be set only once" }
            _parameters = value
        }

    internal data class Parameters(val isEnabled: Boolean = true, val permits: Set<TestConfig.Permit> = emptySet()) {
        companion object {
            val default = Parameters()
        }
    }

    /**
     * An event occurring as part of a test element's setup or execution.
     */
    public sealed class Event(public val element: TestElement) {
        @ExperimentalTime
        public val instant: Instant = Clock.System.now()

        public class Starting(element: TestElement) : Event(element)

        public class Finished(
            element: TestElement,
            public val startingEvent: Starting,
            public val throwable: Throwable? = null
        ) : Event(element) {

            public val succeeded: Boolean get() = throwable == null
            public val failed: Boolean get() = throwable != null

            override fun toString(): String = "${super.toString()} – throwable=$throwable"
        }

        override fun toString(): String = "$element: ${this::class.simpleName}"
    }

    /** The most recent event observed by a `SequencingExecutionReport`. */
    internal var recentEvent: Event? = null

    /** The state of this element's event forwarding by a `SequencingExecutionReport`. */
    internal var forwardingState: ForwardingState = ForwardingState.NotForwarded

    internal enum class ForwardingState { NotForwarded, StartForwarded, FinishForwarded }

    init {
        @Suppress("LeakingThis")
        testElementParent?.registerChildElement(this)
    }

    /**
     * A strategy deciding which elements to include in a test execution.
     */
    internal interface Selection {
        fun includes(testElement: TestElement): Boolean

        /**
         * Returns true if the selection may include [testSuite], false if it is guaranteed to exclude it.
         *
         * Normally, the inclusion of a suite cannot be fully determined until all its child elements have been
         * considered. But if the selection uses a literal pattern (without wildcards) or a literal prefix,
         * we can use it to determine up-front which suites cannot be included.
         */
        fun mayInclude(testSuite: TestSuite): Boolean
    }

    /**
     * Sets up this test element, preparing it for execution.
     *
     * If this test element has children, it must register and set them up in this method's override.
     */
    internal open fun setUp(selection: Selection, report: TestSetupReport) {
        @Suppress("DEPRECATION")
        testConfig.parameterize(this)
    }

    /**
     * Executes the test element, adding [Event]s to the [report].
     *
     * For proper reporting, this method is also invoked for disabled elements.
     */
    internal abstract suspend fun execute(report: TestExecutionReport)

    /**
     * Executes the [setupAction], reporting its [Event]s to the [report].
     */
    internal fun setUpReporting(report: TestSetupReport, setupAction: () -> Unit) {
        val startingEvent = Event.Starting(this)

        report.add(startingEvent)

        try {
            setupAction()
            report.add(Event.Finished(this, startingEvent))
        } catch (throwable: Throwable) {
            report.add(Event.Finished(this, startingEvent, throwable))
            if (throwable is FailFastException) throw throwable
        }
    }

    /**
     * Executes [action], reporting its [Event]s to the [report].
     */
    internal suspend fun executeReporting(report: TestExecutionReport, action: suspend () -> Unit) {
        @Suppress("DEPRECATION")
        testConfig.withExecutionReportSetup(this) { additionalReports ->
            suspend fun Event.Finished.addToReports() {
                // address reports in reverse order for finish events
                additionalReports?.reversed()?.forEach { it.add(this) }
                report.add(this)
            }

            val startingEvent = Event.Starting(this)

            report.add(startingEvent)
            additionalReports?.forEach { it.add(startingEvent) }

            try {
                action()
                Event.Finished(this, startingEvent).addToReports()
            } catch (throwable: Throwable) {
                Event.Finished(this, startingEvent, throwable).addToReports()
                if (throwable is FailFastException) throw throwable
            }
        }
    }

    private fun String.lengthLimited(): String {
        val parentPathPlusSeparatorLength = testElementParent?.testElementPath?.fullyQualifiedReportingName?.length
            ?.plus(Path.REPORTING_SEPARATOR_LENGTH)
            ?: 0
        val originalUniquePathLength = parentPathPlusSeparatorLength + length + TestSuite.UNIQUE_APPENDIX_LENGTH_LIMIT
        if (originalUniquePathLength <= reportingPathLimit) return this
        val newSafeNameLength = length - (originalUniquePathLength - reportingPathLimit) - 1 /* ellipsis */
        require(newSafeNameLength >= 0) {
            "Could not produce a test element name observing the length limit" +
                " of $reportingPathLimit for its element path.\n" +
                "\tParent path: ${testElementParent?.testElementPath}\n" +
                "\tElement name: '$this'"
        }
        return this.dropLast(length - newSafeNameLength) + "…"
    }

    override fun toString(): String = "${this::class.simpleName}($testElementPath)"

    internal companion object {
        /**
         * A [TestElement.Selection] including all test elements.
         */
        internal val AllInSelection = object : Selection {
            override fun includes(testElement: TestElement): Boolean = true
            override fun mayInclude(testSuite: TestSuite): Boolean = true
        }
    }
}

private fun String.safeAsSuiteDisplayName() = transformed(suiteDisplayNameReplacements)

private fun String.safeAsLowerLevelSuiteDisplayName() = transformed(lowerLevelSuiteDisplayNameReplacements)

private fun String.safeAsTestDisplayName() = transformed(testDisplayNameReplacements)

private fun String.transformed(replacementCharacters: Map<Char, Char>): String = buildString(length) {
    for (character in this@transformed) {
        val replacementCharacter = replacementCharacters[character]
        append(
            when {
                replacementCharacter != null -> replacementCharacter
                character.code < 32 -> '�'
                else -> character
            }
        )
    }
}

private val suiteDisplayNameReplacements = if (TestSession.global.reportingMode == ReportingMode.Files) {
    // These characters are potentially file-system-incompatible on Windows. Gradle reporting does not escape
    // characters in what it considers to be a "package name", which in our hierarchy is every non-leaf suite.
    mapOf(
        '<' to '＜',
        '>' to '＞',
        ':' to '։',
        '"' to '＂',
        '/' to '⧸', // The slash is also a problem on other platforms, making Gradle create unexpected directories.
        '\\' to '⧹',
        '|' to '❘',
        '?' to '？',
        '*' to '＊'
    )
} else {
    mapOf()
}

private val lowerLevelSuiteDisplayNameReplacements =
    mapOf(' ' to ESCAPED_SPACE, '.' to ESCAPED_DOT) + suiteDisplayNameReplacements

private val testDisplayNameReplacements = mapOf('.' to ESCAPED_DOT)

private const val ESCAPED_SPACE = '\u00a0' // non-breaking space
private const val ESCAPED_DOT = '·' // middle dot
private const val REPORTING_SEPARATOR: String = "$ESCAPED_SPACE↘$ESCAPED_SPACE"
