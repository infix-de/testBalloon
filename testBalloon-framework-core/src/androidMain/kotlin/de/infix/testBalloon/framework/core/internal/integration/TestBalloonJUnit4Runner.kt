package de.infix.testBalloon.framework.core.internal.integration

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestElement
import de.infix.testBalloon.framework.core.TestElementEvent
import de.infix.testBalloon.framework.core.TestSession
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.internal.EnvironmentBasedElementSelection
import de.infix.testBalloon.framework.core.internal.TestSetupReport
import de.infix.testBalloon.framework.core.internal.logDebug
import de.infix.testBalloon.framework.core.withSingleThreadedDispatcher
import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.ReportingMode
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import de.infix.testBalloon.framework.shared.internal.TestFrameworkDiscoveryResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
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
 * This class is registered via a `@RunWith`-annotated class (see above, it doesn't matter what class is used).
 * JUnit 4 will instantiate this class and invoke its methods.
 */
@TestBalloonInternalApi
public class TestBalloonJUnit4Runner(@Suppress("unused") testClass: Class<*>) : Runner() {
    internal val sessionDescription by lazy {
        // Trigger the framework's initialization by invoking the `testFrameworkDiscoveryResult` property getter
        // in the generated file-level class for `EntryPointAnchor.kt`.
        // We don't need the actual result here.
        try {
            Class
                .forName(Constants.ENTRY_POINT_ANCHOR_CLASS_NAME)
                .getMethod(Constants.JVM_DISCOVERY_RESULT_PROPERTY_GETTER).invoke(null)
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
                override fun add(event: TestElementEvent) {
                    if (event is TestElementEvent.Finished && event.throwable != null) {
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

    override fun run(notifier: RunNotifier): Unit = runBlocking(Dispatchers.Default) {
        // Why are we running on Dispatchers.Default? Because otherwise, a nested runBlocking could hang the entire
        // system due to thread starvation. See https://github.com/Kotlin/kotlinx.coroutines/issues/3983

        @OptIn(ExperimentalCoroutinesApi::class)
        withSingleThreadedDispatcher { notificationDispatcher ->

            // Android's `TraceRunListener`, which is invoked by `RunNotifier`, requires each event's start and
            // finish notifications to be reported on the same thread. We guarantee this by using a dedicated thread
            // for reporting. In addition, we use a channel to avoid CPU context switches.
            // NOTE: The channel size determines the number of events which can be generated before tests are
            // stalled by a slow reporting infrastructure. SequencingExecutionReport can deal with stalling and
            // will not deadlock.
            val notificationChannel = Channel<TestElementEvent>(10_000)

            launch(notificationDispatcher) {
                for (event in notificationChannel) {
                    val element = event.element
                    val description = element.platformDescription

                    when (event) {
                        is TestElementEvent.Starting -> {
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

                        is TestElementEvent.Finished -> {
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

            // Here we stay in runBlocking on Dispatchers.Default.

            try {
                TestSession.global.execute(
                    // We use a [SequencingExecutionReport] because Android's `TraceRunListener` does not support
                    // concurrency (although JUnit 4 could).
                    report = object : SequencingExecutionReport() {
                        // A TestReport relaying each TestElementEvent to the JUnit 4 notifier.

                        override suspend fun forward(event: TestElementEvent) {
                            notificationChannel.send(event)
                        }
                    }
                )
            } finally {
                notificationChannel.close()
            }
        }
    }
}

private fun TestElement.newPlatformDescription(): Description = when (this) {
    is TestSuite -> {
        Description.createSuiteDescription(
            testElementPath.fullyQualifiedReportingName.printable(),
            testElementPath.internalId
        ).apply {
            testElementChildren.forEach {
                addChild(it.newPlatformDescription())
            }
        }
    }

    is Test -> {
        testElementParent as TestSuite

        val displayName = if (TestSession.global.reportingMode == ReportingMode.INTELLIJ_IDEA) {
            testElementPath.partiallyQualifiedReportingName.printable()
        } else {
            testElementDisplayName.printable()
        }

        Description.createTestDescription(
            testElementParent.testElementPath.fullyQualifiedReportingName.printable(),
            displayName.printable(), // Guard against a slash crashing Android Device tests.
            Category(TestBalloonJUnit4Runner::class) // Support JUnit 4 runner selection via 'includeCategories'.
        )
    }
}.also {
    testElementDescriptions[this] = it
}

private fun String.printable(): String = buildString(length) {
    for (character in this@printable) {
        append(
            when {
                character.code < 32 -> '�'
                character == '/' -> '⧸'
                else -> character
            }
        )
    }
}

internal val TestElement.platformDescription: Description
    get() = checkNotNull(testElementDescriptions[this]) { "$this is missing its test description" }

private class TestBalloonInitializationError(message: String, cause: Throwable) : Error(message, cause)

private fun log(message: () -> String) {
    logDebug(message)
}
