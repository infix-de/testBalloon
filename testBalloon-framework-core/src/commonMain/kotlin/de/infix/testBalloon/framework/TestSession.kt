package de.infix.testBalloon.framework

import de.infix.testBalloon.framework.internal.EnvironmentBasedElementSelection
import de.infix.testBalloon.framework.internal.EnvironmentVariable
import de.infix.testBalloon.framework.internal.TestReportingMode
import de.infix.testBalloon.framework.internal.argumentsBasedElementSelection
import de.infix.testBalloon.framework.internal.value
import kotlinx.coroutines.Dispatchers

/**
 * A compilation module's root test suite, holding the module-wide default configuration.
 *
 * A compilation module may declare at most one test session. It is the root of the test element hierarchy.
 * The test framework's generated code invokes `initializeTestFramework` at module initialization time, making
 * sure that a valid [TestSession] exists before instantiating any top-level [TestSuite].
 *
 * A custom [TestSession] specifying a global configuration can be declared like this:
 * ```
 * class MyTestSession :
 *     TestSession(
 *         testConfig = TestConfig.coroutineContext(UnconfinedTestDispatcher()),
 *         defaultCompartment = { TestCompartment.Concurrent }
 *     )
 * ```
 */
@TestDiscoverable
public open class TestSession protected constructor(
    testConfig: TestConfig = DefaultConfiguration,
    defaultCompartment: (() -> TestCompartment) = { TestCompartment.Default },
    reportingMode: TestReportingMode? = null
) : TestSuite(
    parent = null,
    name = "${testPlatform.displayName} session",
    testConfig = testConfig
),
    AbstractTestSession {

    internal val defaultCompartment: TestCompartment by lazy { defaultCompartment() }

    internal val reportingMode: TestReportingMode =
        reportingMode
            ?: EnvironmentVariable.TESTBALLOON_REPORTING.value()?.let {
                try {
                    TestReportingMode.valueOf(it)
                } catch (_: IllegalArgumentException) {
                    throw IllegalArgumentException(
                        "The environment variable '${EnvironmentVariable.TESTBALLOON_REPORTING}'" +
                            " contains the value '$it', which is unsupported.\n" +
                            "\tPlease choose one of ${TestReportingMode.entries}."
                    )
                }
            }
            ?: TestReportingMode.FILES

    init {
        if (singleton != null) {
            throw IllegalArgumentException(
                "The module has been initialized with a TestSession before." +
                    " There must be only one TestSession per compilation module."
            )
        }
        @Suppress("LeakingThis")
        singleton = this
    }

    internal constructor(reportingMode: TestReportingMode? = null) : this(
        testConfig = DefaultConfiguration,
        reportingMode = reportingMode
    )

    /**
     * Parameterizes the session with a default selection, prioritizing [EnvironmentVariable.TESTBALLOON_INCLUDE].
     */
    internal fun parameterize(report: TestConfigurationReport) {
        val includePatternsFromEnvironment = EnvironmentVariable.TESTBALLOON_INCLUDE.value()
        val selection = if (includePatternsFromEnvironment != null) {
            EnvironmentBasedElementSelection(
                includePatternsFromEnvironment,
                EnvironmentVariable.TESTBALLOON_EXCLUDE.value()
            )
        } else {
            argumentsBasedElementSelection ?: AllInSelection
        }
        parameterize(selection, report)
    }

    public companion object {
        /**
         * The default session configuration.
         *
         * Executing elements sequentially on [Dispatchers.Default], using [kotlinx.coroutines.test.TestScope]
         * inside tests.
         */
        public val DefaultConfiguration: TestConfig =
            TestConfig.invocation(TestInvocation.SEQUENTIAL)
                .coroutineContext(Dispatchers.Default)
                .testScope(true)

        private var singleton: TestSession? = null

        internal val global: TestSession
            get() =
                singleton ?: throw IllegalStateException(
                    "The test framework was not initialized." +
                        " A TestSession must exist before creating any top-level TestSuite." +
                        "\n\tPlease ensure that the test framework's Gradle plugin is configured."
                )

        /** Resets global state, enabling the execution of multiple test sessions in one process. */
        internal fun resetState() {
            singleton = null
        }
    }
}
