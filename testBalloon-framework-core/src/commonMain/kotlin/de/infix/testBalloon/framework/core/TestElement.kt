package de.infix.testBalloon.framework.core

import de.infix.testBalloon.framework.core.internal.TestSetupReport
import de.infix.testBalloon.framework.core.internal.reportingPathLimit
import de.infix.testBalloon.framework.core.internal.reportingPathLimitBelowTopLevel
import de.infix.testBalloon.framework.shared.AbstractTestElement
import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.ReportingMode
import de.infix.testBalloon.framework.shared.internal.safeAsInternalId
import de.infix.testBalloon.framework.shared.internal.safelyTransformed
import kotlinx.atomicfu.atomic
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

public sealed class TestElement(
    parent: TestSuite?,
    name: String,
    propertyFqn: String? = null,
    internal var testConfig: TestConfig
) : AbstractTestElement {

    internal val testElementParent: TestSuite? = parent

    internal val isSessionOrCompartment: Boolean = parent == null || this is TestCompartment

    internal val isTopLevelSuite: Boolean = parent is TestCompartment

    /** The element's identifying name (made unique per hierarchy level, otherwise unmodified). */
    internal val testElementName: String

    /** The element's display name (length-limited, unique per hierarchy level). */
    internal val testElementDisplayName: String

    init {
        fun elementInfo() = if (parent == null) "a test element" else "a test element in $parent"
        require(name.isNotEmpty() && name.isNotBlank()) {
            "Could not register ${elementInfo()} with an empty or blank name '$name'"
        }

        val namingParent = if (isTopLevelSuite) TestSession.global else parent

        testElementName = propertyFqn ?: namingParent?.childElementNamesRegistry?.uniqueName(name) ?: name

        testElementDisplayName =
            namingParent?.childDisplayNamesRegistry?.uniqueName(name.asLengthLimitedReportingPathName())
                ?: name.asLengthLimitedReportingPathName()
    }

    override val testElementPath: Path = Path(this)

    /**
     * A path uniquely identifying a test element in its test hierarchy.
     */
    public class Path internal constructor(private val targetElement: TestElement) : AbstractTestElement.Path {
        override fun toString(): String = "«$internalId»"

        /**
         * This path's internal ID, with as little transformation as possible, but safe for low-level infrastructure.
         */
        internal val internalId: String by lazy {
            asString(separator = INTERNAL_PATH_ELEMENT_SEPARATOR_STRING) { testElementName.safeAsInternalId() }
        }

        /**
         * This path's fully qualified reporting name, including the top-level suite's package name, if present.
         */
        internal val reportingNameFullyQualified: String by lazy {
            asString(separator = REPORTING_SEPARATOR) { externalizedName(includeTopLevelPackageName = true) }
        }

        /**
         * This path's unqualified reporting name, excluding a top-level suite's package name.
         */
        internal val reportingNameWithoutTopLevelPackage: String by lazy {
            asString(separator = REPORTING_SEPARATOR) { externalizedName(includeTopLevelPackageName = false) }
        }

        /**
         * This path's reporting name, excluding the top-level suite name, made globally unique.
         *
         * For a top-level suite, the value will be the unqualified suite name, made globally unique.
         *
         * This name is used in Android device tests. To avoid crashing the Android test infrastructure, the name
         * - must not exceed a certain length, and
         * - it must be globally unique.
         *
         * For details, see comment for `defaultReportingPathLimitBelowTopLevel` in `TestFramework.android.kt`.
         */
        internal val reportingNameBelowTopLevel: String by lazy {
            val parentReportingNameBelowTopLevel =
                targetElement.testElementParent
                    ?.takeIf { !it.isTopLevelSuite && !targetElement.isTopLevelSuite }
                    ?.testElementPath?.reportingNameBelowTopLevel
            val originalName = buildString {
                if (parentReportingNameBelowTopLevel != null) {
                    append(parentReportingNameBelowTopLevel)
                    append(REPORTING_SEPARATOR)
                }
                append(targetElement.externalizedName(includeTopLevelPackageName = false))
            }
            val originalUniquePathLength = originalName.length + TestSuite.UNIQUE_APPENDIX_LENGTH_LIMIT

            val candidateName = if (originalUniquePathLength <= reportingPathLimitBelowTopLevel) {
                originalName
            } else {
                val newSafeNameLength =
                    originalName.length - (originalUniquePathLength - reportingPathLimitBelowTopLevel) - 1 /*ellipsis*/
                require(newSafeNameLength >= 0) {
                    "Could not produce a test element path starting below the top level, which observes the" +
                        " length limit of $reportingPathLimitBelowTopLevel characters.\n" +
                        "\tElement path: `$originalName`"
                }
                originalName.dropLast(originalName.length - newSafeNameLength) + "…"
            }

            TestSession.global.reportingNamesBelowTopLevelRegistry.uniqueName(candidateName)
        }

        /**
         * This path element's reporting name.
         */
        internal val elementReportingName: String
            get() = targetElement.externalizedName(includeTopLevelPackageName = false)

        private fun TestElement.externalizedName(includeTopLevelPackageName: Boolean): String =
            if (isTopLevelSuite && includeTopLevelPackageName) {
                // Do not escape fully qualified element names for top-level suites as reporting tools extract a
                // package name.
                // NOTE: If users explicitly supply element names without a package prefix, reports might deviate from
                // expectations.
                testElementName.safeAsSuiteDisplayName()
            } else {
                if (this is Test) {
                    testElementDisplayName.safeAsTestDisplayName()
                } else {
                    // lower-level suite or top-level suite without package name
                    testElementDisplayName.safeAsLowerLevelSuiteDisplayName()
                }
            }

        private fun asString(
            separator: String,
            topLevelSuiteExcluded: Boolean = false,
            elementName: TestElement.() -> String
        ): String = if (targetElement.testElementParent == null ||
            targetElement.isTopLevelSuite ||
            (topLevelSuiteExcluded && targetElement.testElementParent.isTopLevelSuite)
        ) {
            targetElement.elementName()
        } else {
            buildString {
                append(
                    targetElement.testElementParent.testElementPath.asString(
                        separator = separator,
                        topLevelSuiteExcluded = topLevelSuiteExcluded,
                        elementName = elementName
                    )
                )
                append(separator)
                append(elementName(targetElement))
            }
        }

        internal companion object {
            internal const val REPORTING_SEPARATOR_LENGTH = REPORTING_SEPARATOR.length
            private const val INTERNAL_PATH_ELEMENT_SEPARATOR_STRING = "${Constants.INTERNAL_PATH_ELEMENT_SEPARATOR}"
        }
    }

    internal enum class DisplayNameMode {
        PathFullyQualified,
        PathWithoutTopLevelPackage,
        ElementOnly
    }

    /**
     * Returns the element's reporting coordinates in a form to be parsed by the IDE plugin.
     *
     * The format must be synchronized with the IDE plugin's `TestElementReportingCoordinates.from()`.
     */
    internal fun reportingCoordinates(mode: DisplayNameMode): String =
        reportingCoordinates(suiteMode = mode, testMode = mode)

    /**
     * Returns the element's reporting coordinates in a form to be parsed by the IDE plugin.
     *
     * The format is: <BEGIN_MARK><elementPathInternalId><COMPONENT_SEPARATOR><displayName><END_MARK>
     * The format must be synchronized with the IDE plugin's `TestElementReportingCoordinates.from()`.
     */
    internal fun reportingCoordinates(suiteMode: DisplayNameMode, testMode: DisplayNameMode): String = buildString {
        append(Constants.INTERNAL_ELEMENT_REPORTING_COORDINATES_BEGIN_MARK)
        append(testElementPath.internalId)
        append(Constants.INTERNAL_ELEMENT_REPORTING_COORDINATES_COMPONENT_SEPARATOR)
        append(
            when (if (this@TestElement is TestSuite) suiteMode else testMode) {
                DisplayNameMode.PathFullyQualified -> testElementPath.reportingNameFullyQualified
                DisplayNameMode.PathWithoutTopLevelPackage -> testElementPath.reportingNameWithoutTopLevelPackage
                DisplayNameMode.ElementOnly -> testElementDisplayName
            }
        )
        append(Constants.INTERNAL_ELEMENT_REPORTING_COORDINATES_END_MARK)
    }

    internal val reportingNameForJsAndTeamCity: String
        get() = when (TestSession.global.reportingMode) {
            ReportingMode.GradleIntellijIdeaWithNesting,
            ReportingMode.GradleIntellijIdeaWithoutNesting -> {
                // JS frameworks will replace the last dot in an element's name, so we place it after our
                // significant content, which must be unchanged. The phrase "FOR_IDE_PLUGIN" exposes a missing IDE
                // plugin. An IDE plugin always relies on the reporting coordinates and ignores everything outside.
                reportingCoordinates(
                    suiteMode = DisplayNameMode.PathWithoutTopLevelPackage,
                    testMode = DisplayNameMode.ElementOnly
                ) + ".FOR_IDE_PLUGIN"
            }

            ReportingMode.GradleIntellijIdeaLegacy,
            ReportingMode.GradleFilesWithoutNesting -> {
                if (isTopLevelSuite) {
                    testElementPath.reportingNameFullyQualified
                } else {
                    testElementPath.elementReportingName
                }
            }

            ReportingMode.GradleFilesWithNesting,
            ReportingMode.Amper ->
                testElementPath.elementReportingName

            ReportingMode.AndroidDevice ->
                throw IllegalArgumentException("JS/TeamCity reporting is unsuitable for Android device tests")
        }

    internal val topLevelSuiteReportingName: String
        get() {
            var element = this
            while (!element.isTopLevelSuite) {
                element = element.testElementParent ?: break
            }
            return element.testElementName.safeAsSuiteDisplayName()
        }

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

    internal data class Parameters(
        val isEnabled: Boolean = true,
        val permits: Set<TestConfig.Permit> = emptySet(),
        val keyedParameters: Map<KeyedParameter.Key<*>, KeyedParameter> = mapOf()
    ) {
        companion object {
            val default = Parameters()
        }
    }

    /**
     * A parameter uniquely identified by its [key]. The key also determines the specific parameter's type.
     *
     * Usage:
     * ```
     * class MyParameter : TestElement.KeyedParameter(Key) {
     *     companion object {
     *         val Key = object : Key<MyParameter> {}
     *     }
     * }
     * ```
     */
    public abstract class KeyedParameter(internal val key: Key<*>) {
        public interface Key<SpecificParameter : KeyedParameter>
    }

    /**
     * Returns the parameter identified by [key], if it exists, otherwise null.
     */
    public fun <SpecificParameter : KeyedParameter> testElementParameter(
        key: KeyedParameter.Key<SpecificParameter>
    ): SpecificParameter? = parameters.keyedParameters[key]?.let {
        @Suppress("UNCHECKED_CAST")
        it as SpecificParameter
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

    /**
     * `true`, if the element (including all of its child elements) executed successfully so far.
     */
    private val executedSuccessfully = atomic(true)

    internal fun executedSuccessfully() = executedSuccessfully.value

    internal fun registerFailure() {
        if (executedSuccessfully.compareAndSet(expect = true, update = false)) {
            testElementParent?.registerFailure()
        }
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
                registerFailure()
                Event.Finished(this, startingEvent, throwable).addToReports()
                if (throwable is FailFastException) throw throwable
            }
        }
    }

    /**
     * Returns the length-limited name considered path of the element's reporting path.
     *
     * The length limitation ensures that the resulting reporting path, ending with [this] name, and appended
     * with a unique appendix, stays within the length limits, if possible.
     */
    private fun String.asLengthLimitedReportingPathName(): String {
        val parentPathPlusSeparatorLength = testElementParent?.testElementPath?.reportingNameFullyQualified?.length
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
         * A [Selection] including all test elements.
         */
        internal val AllInSelection = object : Selection {
            override fun includes(testElement: TestElement): Boolean = true
            override fun mayInclude(testSuite: TestSuite): Boolean = true
        }
    }
}

private fun String.safeAsSuiteDisplayName() = safelyTransformed(suiteDisplayNameReplacements)

private fun String.safeAsLowerLevelSuiteDisplayName() = safelyTransformed(lowerLevelSuiteDisplayNameReplacements)

private fun String.safeAsTestDisplayName() = safelyTransformed(testDisplayNameReplacements)

private val suiteDisplayNameReplacements: Map<Char, Char>
    get() = if (TestSession.global.reportingMode.isGradleFiles) {
        suiteDisplayNameReplacementsForGradleFileReporting
    } else {
        emptyMap()
    }

// These characters are potentially file-system-incompatible on Windows. Gradle reporting does not escape
// characters in what it considers to be a "package name", which in our hierarchy is every non-leaf suite.
private val suiteDisplayNameReplacementsForGradleFileReporting = mapOf(
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

private val lowerLevelSuiteDisplayNameReplacements by lazy {
    mapOf(' ' to Constants.ESCAPED_SPACE, '.' to ESCAPED_DOT) + suiteDisplayNameReplacements
}

private val testDisplayNameReplacements = mapOf('.' to ESCAPED_DOT)

private const val ESCAPED_DOT = '·' // middle dot
internal const val REPORTING_SEPARATOR: String = "${Constants.ESCAPED_SPACE}↘${Constants.ESCAPED_SPACE}"
