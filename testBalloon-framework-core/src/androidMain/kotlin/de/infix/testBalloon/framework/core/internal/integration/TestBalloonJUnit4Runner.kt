package de.infix.testBalloon.framework.core.internal.integration

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestElement
import de.infix.testBalloon.framework.core.TestExecutionReport
import de.infix.testBalloon.framework.core.TestSession
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.internal.EnvironmentBasedElementSelection
import de.infix.testBalloon.framework.core.internal.TestSetupReport
import de.infix.testBalloon.framework.core.internal.logDebug
import de.infix.testBalloon.framework.core.internal.testInfrastructureIsAndroidDevice
import de.infix.testBalloon.framework.core.withSingleThreadedDispatcher
import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.InvokedByGeneratedCode
import de.infix.testBalloon.framework.shared.internal.ReportingMode
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import de.infix.testBalloon.framework.shared.internal.TestFrameworkDiscoveryResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.experimental.categories.Category
import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier
import java.util.concurrent.ConcurrentHashMap

private val testElementDescriptions = ConcurrentHashMap<TestElement, Description>()

/**
 * The [Runner] interfacing with JUnit 4 (Android only).
 *
 * This class is registered via a `@RunWith`-annotated class by compiler-plugin-generated entry point code.
 * JUnit 4 will instantiate this class and invoke its methods.
 */
@TestBalloonInternalApi
@InvokedByGeneratedCode
public class TestBalloonJUnit4Runner(@Suppress("unused") testClass: Class<*>) : Runner() {
    internal val sessionDescription by lazy {
        // Trigger the framework's initialization by invoking the `testFrameworkDiscoveryResult` function
        // in the generated `JvmEntryPoint` class.
        // We don't need the actual result here.
        try {
            Class
                .forName(Constants.JVM_ENTRY_POINT_CLASS_NAME)
                .getMethod(Constants.JVM_DISCOVERY_RESULT_METHOD_NAME).invoke(null)
                as TestFrameworkDiscoveryResult
        } catch (throwable: Throwable) {
            throw TestBalloonInitializationError(
                "Could not access the test discovery result.\n" +
                    "\tPlease ensure that the correct version of the framework's compiler plugin was applied.",
                throwable
            )
        }

        TestSession.global.setUp(
            EnvironmentBasedElementSelection(),
            report = object : TestSetupReport() {
                override fun add(event: TestElement.Event) {
                    if (event is TestElement.Event.Finished && event.throwable != null) {
                        throw TestBalloonInitializationError(
                            "Could not configure ${event.element.testElementPath}",
                            event.throwable
                        )
                    }
                }
            }
        )

        TestSession.global.newPlatformDescription()
    }

    override fun getDescription(): Description = sessionDescription

    override fun run(notifier: RunNotifier): Unit = runBlocking {
        // Why are we running on Dispatchers.Default? Because otherwise, a nested runBlocking could hang the entire
        // system due to thread starvation. See https://github.com/Kotlin/kotlinx.coroutines/issues/3983

        @OptIn(ExperimentalCoroutinesApi::class)
        withSingleThreadedDispatcher {
            // Android's `TraceRunListener`, which is invoked by `RunNotifier`, requires each event's start and
            // finish notifications to be reported on the same thread.
            // Also, the Android test infrastructure uses `RunListener`s to finish activities. This should occur in
            // order, so we cannot not use any concurrency or delayed reporting here (like using
            // `SequencingExecutionReport`).

            TestSession.global.execute(
                report = object : TestExecutionReport() {
                    // An execution relaying each TestElement.Event to the JUnit 4 run notifier.

                    override suspend fun add(event: TestElement.Event) {
                        val element = event.element
                        val description = element.platformDescription

                        when (event) {
                            is TestElement.Event.Starting -> {
                                if (element.testElementIsEnabled) {
                                    log { "$description: $element starting" }
                                    when (element) {
                                        is TestSuite -> notifier.fireTestSuiteStarted(description)
                                        is Test -> notifier.fireTestStarted(description)
                                    }
                                } else {
                                    if (element is Test) {
                                        log { "$description: $element ignored" }
                                        notifier.fireTestIgnored(description)
                                    }
                                }
                            }

                            is TestElement.Event.Finished -> {
                                if (element.testElementIsEnabled) {
                                    val throwable = event.throwable
                                    log { "$description: $element finished, result=$throwable)" }
                                    if (throwable != null) {
                                        notifier.fireTestFailure(Failure(description, throwable))
                                    }
                                    when (element) {
                                        is TestSuite -> notifier.fireTestSuiteFinished(description)
                                        is Test -> notifier.fireTestFinished(description)
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

private fun TestElement.newPlatformDescription(): Description = when (this) {
    is TestSuite -> {
        Description.createSuiteDescription(
            if (TestSession.global.reportingMode == ReportingMode.IntellijIdea && !testInfrastructureIsAndroidDevice) {
                reportingCoordinates(mode = TestElement.CoordinatesMode.FullyQualified)
            } else {
                testElementPath.reportingNameWithTopLevelPackage
            }.safeForDeviceSideTests(),
            testElementPath.internalId
        ).apply {
            testElementChildren.forEach {
                if (it.isIncluded) {
                    // Create descriptions only for included elements: These will be used to count the
                    // total number of tests. If the number of executed tests is less than the number of
                    // descriptions created for tests, a test run failure will be reported.
                    addChild(it.newPlatformDescription())
                }
            }
        }
    }

    is Test -> {
        Description.createTestDescription(
            topLevelSuiteReportingName,
            when {
                testInfrastructureIsAndroidDevice -> testElementPath.reportingNameBelowTopLevel

                TestSession.global.reportingMode == ReportingMode.IntellijIdea -> {
                    reportingCoordinates(mode = TestElement.CoordinatesMode.WithoutTopLevelPackage)
                }

                else -> testElementPath.elementReportingName
            }.safeForDeviceSideTests(),
            Category(TestBalloonJUnit4Runner::class) // Support JUnit 4 runner selection via 'includeCategories'.
        )
    }
}.also {
    testElementDescriptions[this] = it
}

// Returns a suite or test name guarded against slashes, which are suspected to crash Android device-side tests.
private fun String.safeForDeviceSideTests() = if (testInfrastructureIsAndroidDevice) replace('/', '⧸') else this

internal val TestElement.platformDescription: Description
    get() = checkNotNull(testElementDescriptions[this]) { "$this is missing its test description" }

private class TestBalloonInitializationError(message: String, cause: Throwable) : Error(message, cause)

private fun log(message: () -> String) {
    logDebug(message)
}
