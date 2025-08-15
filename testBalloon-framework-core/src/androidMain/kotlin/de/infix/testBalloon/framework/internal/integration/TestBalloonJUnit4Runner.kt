package de.infix.testBalloon.framework.internal.integration

import androidx.test.platform.app.InstrumentationRegistry
import de.infix.testBalloon.framework.Test
import de.infix.testBalloon.framework.TestConfigurationReport
import de.infix.testBalloon.framework.TestElement
import de.infix.testBalloon.framework.TestElementEvent
import de.infix.testBalloon.framework.TestSession
import de.infix.testBalloon.framework.TestSuite
import de.infix.testBalloon.framework.internal.ListsBasedElementSelection
import de.infix.testBalloon.framework.internal.TestFrameworkDiscoveryResult
import de.infix.testBalloon.framework.internal.logInfo
import de.infix.testBalloon.framework.withSingleThreadedDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runner.Runner
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier
import java.util.concurrent.ConcurrentHashMap

private val testElementDescriptions = ConcurrentHashMap<TestElement, Description>()

/**
 * Entry point registering TestBalloon with JUnit 4.
 */
@RunWith(TestBalloonJUnit4Runner::class)
internal class TestBalloonJUnit4EntryPoint

/**
 * The [Runner] interfacing with JUnit 4 (Android only).
 *
 * This class is registered via a `@RunWith`-annotated class (see above, it doesn't matter what class is used).
 * JUnit 4 will instantiate this class and invoke its methods.
 */
internal class TestBalloonJUnit4Runner(@Suppress("unused") testClass: Class<*>) : Runner() {
    val sessionDescription by lazy {
        // Trigger the framework's initialization by invoking the `testFrameworkDiscoveryResult` property getter
        // in the generated file-level class for `EntryPointAnchor.kt`.
        // We don't need the actual result here.
        try {
            Class
                .forName("de.infix.testBalloon.framework.internal.entryPoint.EntryPointAnchorKt")
                .getMethod("getTestFrameworkDiscoveryResult").invoke(null)
                as TestFrameworkDiscoveryResult
        } catch (throwable: Throwable) {
            throw TestBalloonInitializationError(
                "Could not access the test discovery result.\n" +
                    "\tPlease ensure that the correct version of the framework's compiler plugin was applied.",
                throwable
            )
        }

        TestSession.global.parameterize(
            InstrumentationArgumentsBasedElementSelection(),
            report = object : TestConfigurationReport() {
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

    override fun run(notifier: RunNotifier) = runBlocking(Dispatchers.Default) {
        // Why are we running on Dispatchers.Default? Because otherwise, a nested runBlocking could hang the entire
        // system due to thread starvation. See https://github.com/Kotlin/kotlinx.coroutines/issues/3983

        withSingleThreadedDispatcher { notificationDispatcher ->

            // Android's `TraceRunListener`, which is invoked by `RunNotifier`, requires each event's start and
            // finish notifications to be reported on the same thread. We guarantee this by using a dedicated thread
            // for reporting. In addition, we use a channel to avoid CPU context switches.
            val notificationChannel = Channel<TestElementEvent>(1024)

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

                            if (element.testElementParent == null) return@launch
                        }
                    }
                }
            }

            TestSession.global.execute(
                // We use a [SequencingExecutionReport] because Android's `TraceRunListener` does not support
                // concurrency (although JUnit 4 does).
                report = object : SequencingExecutionReport() {
                    // A TestReport relaying each TestElementEvent to the JUnit 4 notifier.

                    override suspend fun forward(event: TestElementEvent) {
                        notificationChannel.send(event)
                    }
                }
            )
        }
    }
}

/**
 * A [TestElement.Selection] created from instrumentation arguments.
 */
private class InstrumentationArgumentsBasedElementSelection :
    ListsBasedElementSelection(includePatterns, excludePatterns) {

    companion object {
        private val instrumentationArguments = InstrumentationRegistry.getArguments()

        private val includePatterns = instrumentationArguments.getString("TEST_INCLUDE")
        private val excludePatterns = instrumentationArguments.getString("TEST_EXCLUDE")
    }
}

private fun TestElement.newPlatformDescription(): Description = when (this) {
    is TestSuite -> {
        Description.createSuiteDescription(flattenedPath, testElementPath).apply {
            testElementChildren.forEach {
                addChild(it.newPlatformDescription())
            }
        }
    }

    is Test -> Description.createTestDescription(
        testElementParent!!.flattenedPath,
        testElementDisplayName,
        testElementPath
    )
}.also {
    testElementDescriptions[this] = it
}

private val TestElement.platformDescription: Description
    get() = checkNotNull(testElementDescriptions[this]) { "$this is missing its test description" }

private class TestBalloonInitializationError(message: String, cause: Throwable) : Error(message, cause)

private fun log(message: () -> String) {
    logInfo(message)
}
