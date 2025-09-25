package de.infix.testBalloon.framework

import de.infix.testBalloon.framework.internal.PATH_SEGMENT_SEPARATOR
import de.infix.testBalloon.framework.internal.externalId

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
    internal val testElementName: String = parent?.registerUniqueChildElementName(name) ?: name
    internal val testElementDisplayName: String = displayName

    /**
     * A path uniquely identifying a test element in its test hierarchy.
     */
    public class Path internal constructor(private val element: TestElement) : AbstractTestElement.Path {
        override fun toString(): String = "«$externalId»"

        /**
         * This path's internal ID, which is only stable inside a single TestSession.
         */
        internal val internalId: String by lazy { flattened("|") { testElementName } }

        /**
         * This path's external ID, which is stable and copyable outside a single TestSession.
         */
        internal val externalId: String by lazy { flattened { testElementName.externalId() } }

        /**
         * This path's human-readable string of display name segments.
         */
        internal val displayNameSegments: String get() = flattened { testElementDisplayName }

        private fun flattened(separator: String = PATH_SEGMENT_SEPARATOR, segment: TestElement.() -> String): String =
            when (element.testElementParent) {
                null, is TestCompartment, is TestSession -> element.segment()
                else -> buildString {
                    append(element.testElementParent.testElementPath.flattened(separator, segment))
                    append(separator)
                    append(segment(element))
                }
            }
    }

    override val testElementPath: Path = Path(this)

    override var testElementIsEnabled: Boolean = true

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
    }

    /**
     * Parameterizes this test element, preparing it for execution.
     *
     * The framework invokes this method for all test elements before creating an execution plan.
     */
    internal open fun parameterize(selection: Selection, report: TestConfigurationReport) {
        testElementParent?.let { parent ->
            if (!parent.testElementIsEnabled) testElementIsEnabled = false // Inherit a 'disabled' state
        }
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

    override fun toString(): String = "${this::class.simpleName}($testElementPath)"

    internal companion object {
        /**
         * A [TestElement.Selection] including all test elements.
         */
        internal val AllInSelection = object : Selection {
            override fun includes(testElement: TestElement): Boolean = true
        }
    }
}
