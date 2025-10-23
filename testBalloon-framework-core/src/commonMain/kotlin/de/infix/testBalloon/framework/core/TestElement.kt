package de.infix.testBalloon.framework.core

import de.infix.testBalloon.framework.core.internal.reportingPathLimit
import de.infix.testBalloon.framework.shared.AbstractTestElement
import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.ReportingMode

public sealed class TestElement(parent: TestSuite?, name: String, displayName: String = name, testConfig: TestConfig) :
    AbstractTestElement {

    /**
     * The element's test configuration.
     *
     * Note: Using `testSuite(...) { testConfig = ... }` is unsafe and will be removed in the next minor release(s).
     */
    @Suppress("CanBePrimaryConstructorProperty")
    @Deprecated(
        "Use 'testSuite(..., testConfig = ...)' instead. Scheduled for removal in TestBalloon 0.8."
    )
    public var testConfig: TestConfig = testConfig

    internal val testElementParent: TestSuite? = parent
    internal val testElementName: String = parent?.uniqueChildName(name, TestSuite.ChildNameType.ELEMENT) ?: name
    internal val testElementDisplayName: String =
        parent?.uniqueChildName(displayName.lengthLimited(), TestSuite.ChildNameType.DISPLAY)
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
            flattened(INTERNAL_PATH_ELEMENT_SEPARATOR_STRING) { testElementName }
        }

        /**
         * This path's qualified reporting name.
         */
        internal val qualifiedReportingName: String by lazy { flattened(REPORTING_SEPARATOR) { externalizedName() } }

        /**
         * This path's simple reporting name.
         */
        internal val simpleReportingName: String by lazy { element.externalizedName() }

        /**
         * This path's reporting name, simple or qualified, depending on the reporting mode.
         */
        internal val modeDependentReportingName: String by lazy {
            when (TestSession.global.reportingMode) {
                ReportingMode.INTELLIJ_IDEA -> {
                    // A qualified path name for suites ensures proper nesting display in IntelliJ IDEA, but
                    // duplicates path elements in XML and HTML file reports.
                    if (element is Test) simpleReportingName else qualifiedReportingName
                }

                ReportingMode.FILES -> {
                    // Simple element names work for file reports.
                    simpleReportingName
                }
            }
        }

        private fun TestElement.externalizedName(): String = if (isTopLevelSuite) {
            // Do not escape display names for top-level suites as reporting tools extract a package name.
            // If users supply display names formatted without a package prefix, reports might deviate from
            // expectations.
            testElementDisplayName
        } else {
            if (this is Test) {
                testElementDisplayName.replace(".", ESCAPED_DOT)
            } else {
                // lower-level suite
                testElementDisplayName.replace(' ', ESCAPED_SPACE).replace(".", ESCAPED_DOT)
            }
        }

        private fun flattened(separator: String, elementName: TestElement.() -> String): String =
            if (element.testElementParent == null || element.isTopLevelSuite) {
                element.elementName()
            } else {
                buildString {
                    append(element.testElementParent.testElementPath.flattened(separator, elementName))
                    append(separator)
                    append(elementName(element))
                }
            }

        private companion object {
            private const val ESCAPED_SPACE = '\u00a0' // non-breaking space
            private const val ESCAPED_DOT = "·" // middle dot
            private const val REPORTING_SEPARATOR: String = "$ESCAPED_SPACE↘$ESCAPED_SPACE"
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

    internal data class Parameters(val isEnabled: Boolean = true, val permits: Set<TestPermit> = emptySet()) {
        companion object {
            val default = Parameters()
        }
    }

    /** The most recent event observed by a `SequencingExecutionReport`. */
    internal var recentEvent: TestElementEvent? = null

    /** The state of this element's event forwarding by a `SequencingExecutionReport`. */
    internal var forwardingState: ForwardingState = ForwardingState.NOT_FORWARDED

    internal enum class ForwardingState { NOT_FORWARDED, START_FORWARDED, FINISH_FORWARDED }

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
     * Parameterizes this test element, preparing it for execution.
     *
     * The framework invokes this method for all test elements before creating an execution plan.
     */
    internal open fun parameterize(selection: Selection, report: TestConfigurationReport) {
        @Suppress("DEPRECATION")
        testConfig.parameterize(this)
    }

    /**
     * Executes the test element, adding [TestElementEvent]s to the [report].
     *
     * For proper reporting, this method is also invoked for disabled elements.
     */
    internal abstract suspend fun execute(report: TestExecutionReport)

    /**
     * Executes the configuration [action], reporting its [TestElementEvent]s to the [report].
     */
    internal fun configureReporting(report: TestConfigurationReport, action: () -> Unit) {
        val startingEvent = TestElementEvent.Starting(this)

        report.add(startingEvent)

        try {
            action()
            report.add(TestElementEvent.Finished(this, startingEvent))
        } catch (throwable: Throwable) {
            report.add(TestElementEvent.Finished(this, startingEvent, throwable))
            if (throwable is FailFastException) throw throwable
        }
    }

    /**
     * Executes [action], reporting its [TestElementEvent]s to the [report].
     */
    internal suspend fun executeReporting(report: TestExecutionReport, action: suspend () -> Unit) {
        @Suppress("DEPRECATION")
        testConfig.withExecutionReportSetup(this) { additionalReports ->
            suspend fun TestElementEvent.Finished.addToReports() {
                // address reports in reverse order for finish events
                additionalReports?.reversed()?.forEach { it.add(this) }
                report.add(this)
            }

            val startingEvent = TestElementEvent.Starting(this)

            report.add(startingEvent)
            additionalReports?.forEach { it.add(startingEvent) }

            try {
                action()
                TestElementEvent.Finished(this, startingEvent).addToReports()
            } catch (throwable: Throwable) {
                TestElementEvent.Finished(this, startingEvent, throwable).addToReports()
                if (throwable is FailFastException) throw throwable
            }
        }
    }

    private fun String.lengthLimited(): String {
        val originalPathLength = (testElementParent?.testElementPath?.qualifiedReportingName?.length ?: 0) + length
        if (originalPathLength <= reportingPathLimit) return this
        val newLength = originalPathLength - reportingPathLimit - TestSuite.UNIQUE_APPENDIX_LENGTH_LIMIT - 1
        require(newLength >= 0) {
            "Could not produce a test element name observing the length limit" +
                " of $reportingPathLimit for its element path.\n" +
                "\tParent path: ${testElementParent?.testElementPath}\n" +
                "\tElement name: '$this'"
        }
        return this.dropLast(originalPathLength - reportingPathLimit - 1) + "…"
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
