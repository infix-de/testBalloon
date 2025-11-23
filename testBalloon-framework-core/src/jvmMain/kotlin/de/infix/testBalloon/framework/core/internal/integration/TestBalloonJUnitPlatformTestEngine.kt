package de.infix.testBalloon.framework.core.internal.integration

import de.infix.testBalloon.framework.core.FailFastException
import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestElement
import de.infix.testBalloon.framework.core.TestElementEvent
import de.infix.testBalloon.framework.core.TestExecutionReport
import de.infix.testBalloon.framework.core.TestSession
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.internal.TestSetupReport
import de.infix.testBalloon.framework.core.internal.logDebug
import de.infix.testBalloon.framework.core.internal.value
import de.infix.testBalloon.framework.shared.AbstractTestSuite
import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.EnvironmentVariable
import de.infix.testBalloon.framework.shared.internal.ReportingMode
import de.infix.testBalloon.framework.shared.internal.TestFrameworkDiscoveryResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.platform.engine.DiscoveryIssue
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestEngine
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import java.util.concurrent.ConcurrentHashMap

private var topLevelTestSuites = setOf<AbstractTestSuite>()
private val testElementDescriptors = ConcurrentHashMap<TestElement, AbstractTestDescriptor>()

/**
 * The [TestEngine] interfacing with JUnit Platform (JVM only).
 *
 * This class is registered via the `ServiceLoader` mechanism with a provider configuration file on the classpath.
 * JUnit Platform will instantiate it and invoke its methods.
 */
internal class TestBalloonJUnitPlatformTestEngine : TestEngine {
    override fun getId(): String = Constants.JUNIT_ENGINE_ID

    override fun discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
        // We use the framework's compiler plugin to discover tests. That means we are ignoring the
        // discoveryRequest's selectors and filters.

        val engineUniqueId = UniqueId.forEngine(id)
        val engineDescriptor = EngineDescriptor(engineUniqueId, "${this::class.qualifiedName}")

        // Find the generated file-level class for `EntryPointAnchor.kt`.
        val frameworkDiscoveryResultFileClass = try {
            Class.forName(Constants.ENTRY_POINT_ANCHOR_CLASS_NAME)
        } catch (_: ClassNotFoundException) {
            // Do not initialize the test framework if the entry point anchor file class is not on the classpath.
            return engineDescriptor
        }

        var discoveryIssueCount = 0

        fun reportDiscoveryIssue(throwable: Throwable?, vararg additionalMessages: String) {
            if (throwable != null) {
                discoveryIssueCount++
            }
            discoveryRequest.discoveryListener.issueEncountered(
                engineUniqueId,
                DiscoveryIssue.create(
                    if (throwable != null) DiscoveryIssue.Severity.ERROR else DiscoveryIssue.Severity.INFO,
                    buildString {
                        for (message in additionalMessages) {
                            append("${message.prependIndent("\t")}\n")
                        }
                        throwable?.let { append(it.stackTraceToString().prependIndent("\t")) }
                    }
                )
            )
        }

        // Here we try to find out how we were invoked: Normally, we don't need to care about the discovery request,
        // because our Gradle plugin will provide everything we need in environment variables.
        // However, this happens to fail with tests under the following conditions:
        // - they run via an IntelliJ IDEA run configuration,
        // - the "Run as test" option is active,
        // - a '--tests' argument is present.
        // In this case, for some reason our Gradle plugin does not pick up the command line arguments and provides
        // an empty value in 'TESTBALLOON_INCLUDE_PATTERNS'.
        val selectorsArePresent = discoveryRequest.getSelectorsByType(DiscoverySelector::class.java).isNotEmpty()
        // We need to differentiate this case from Gradle naively dumping all classes at us, which we can detect by
        // checking for our entry point class, which can never be a legitimate test selector.
        val testClassSelectorsAreGuesswork =
            selectorsArePresent && discoveryRequest.getSelectorsByType(ClassSelector::class.java).any {
                it.className == Constants.ENTRY_POINT_ANCHOR_CLASS_NAME
            }
        if (selectorsArePresent &&
            !testClassSelectorsAreGuesswork &&
            EnvironmentVariable.TESTBALLOON_INCLUDE_PATTERNS.value()?.ifEmpty { null } == null
        ) {
            // In this case, the invocation was triggered by an IDE plugin targeting JUnit tests. Skip it.
            reportDiscoveryIssue(null, "Test selectors for another test engine were detected, skipping.")
            return engineDescriptor
        }

        // Trigger the framework's initialization by invoking the `testFrameworkDiscoveryResult` property getter
        // in the generated file-level class for `EntryPointAnchor.kt`.
        val frameworkDiscoveryResult = try {
            frameworkDiscoveryResultFileClass.getMethod(Constants.JVM_DISCOVERY_RESULT_PROPERTY_GETTER).invoke(null)
                as TestFrameworkDiscoveryResult
        } catch (throwable: Throwable) {
            reportDiscoveryIssue(
                throwable,
                "Could not access the test discovery result.",
                "Please ensure that the correct version of the framework's compiler plugin was applied."
            )
            null
        }

        if (frameworkDiscoveryResult != null) {
            topLevelTestSuites = frameworkDiscoveryResult.topLevelTestSuites.toSet()

            TestSession.global.setUp(
                report = object : TestSetupReport() {
                    override fun add(event: TestElementEvent) {
                        if (event is TestElementEvent.Finished && event.throwable != null) {
                            reportDiscoveryIssue(
                                event.throwable,
                                "Could not configure ${event.element.testElementPath}"
                            )
                        }
                    }
                }
            )
        }

        if (discoveryIssueCount > 0) {
            return engineDescriptor
        }

        log { "created EngineDescriptor(${engineDescriptor.uniqueId}, ${engineDescriptor.displayName})" }
        testElementDescriptors[TestSession.global] = engineDescriptor
        for (topLevelSuite in topLevelTestSuites) {
            engineDescriptor.addChild((topLevelSuite as TestSuite).newPlatformDescriptor(uniqueId))
        }

        return engineDescriptor
    }

    override fun execute(request: ExecutionRequest) {
        if (topLevelTestSuites.isEmpty()) {
            // No tests were discovered. This is typically the case if the framework has been included
            // as a dependency, but another test framework is in charge. Make sure we don't report anything
            // to JUnit Platform's listener, otherwise JUnit Platform will complain.
            return
        }

        val jUnitListener = request.engineExecutionListener

        runBlocking(Dispatchers.Default) {
            // Why are we running on Dispatchers.Default? Because otherwise, a nested runBlocking could hang the entire
            // system due to thread starvation. See https://github.com/Kotlin/kotlinx.coroutines/issues/3983

            TestSession.global.execute(
                report = object : TestExecutionReport() {
                    // A TestReport relaying each TestElementEvent to the JUnit listener.

                    override suspend fun add(event: TestElementEvent) {
                        if (event.element.isSessionOrCompartment) {
                            log { "$event: skipping session or compartment" }
                            return
                        }

                        when (event) {
                            is TestElementEvent.Starting -> {
                                if (event.element.testElementIsEnabled) {
                                    log { "${event.element.platformDescriptor}: ${event.element} starting" }
                                    jUnitListener.executionStarted(event.element.platformDescriptor)
                                } else {
                                    if (event.element.testElementParent?.testElementIsEnabled == true) {
                                        // Report skipping only if it has not already been reported by a parent.
                                        // (Report nothing if this is the disabled root element.)
                                        log { "${event.element.platformDescriptor}: ${event.element} skipped" }
                                        jUnitListener.executionSkipped(event.element.platformDescriptor, "disabled")
                                    }
                                }
                            }

                            is TestElementEvent.Finished -> {
                                if (event.element.testElementIsEnabled) {
                                    log {
                                        "${event.element.platformDescriptor}: ${event.element} finished," +
                                            " result=${event.executionResult})"
                                    }
                                    jUnitListener.executionFinished(
                                        event.element.platformDescriptor,
                                        event.executionResult
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

private class TestElementJUnitPlatformDescriptor(
    uniqueId: UniqueId,
    displayName: String,
    source: TestSource?,
    val element: TestElement
) : AbstractTestDescriptor(uniqueId, displayName, source) {
    override fun getType(): TestDescriptor.Type = when (element) {
        is Test -> TestDescriptor.Type.TEST
        is TestSuite -> TestDescriptor.Type.CONTAINER
    }

    override fun toString(): String = "PD(uId=$uniqueId, dN=\"$displayName\", t=$type)"
}

private fun TestElement.newPlatformDescriptor(parentUniqueId: UniqueId): TestElementJUnitPlatformDescriptor {
    val uniqueId: UniqueId
    val element = this
    var source: TestSource? = null

    val segmentType = when (element) {
        is Test -> "test"
        is TestSuite -> {
            if (isTopLevelSuite) {
                source = ClassSource.from(testElementName)
                "class"
            } else {
                "suite"
            }
        }
    }
    uniqueId = parentUniqueId.append(segmentType, testElementName)
    val displayName = if (TestSession.global.reportingMode == ReportingMode.INTELLIJ_IDEA) {
        testElementDisplayName
    } else {
        testElementPath.qualifiedReportingNameBelowTopLevel
    }

    return TestElementJUnitPlatformDescriptor(
        uniqueId = uniqueId,
        displayName = displayName,
        source = source,
        element = element
    ).apply {
        log { "created TestDescriptor($uniqueId, $displayName)" }
        testElementDescriptors[element] = this
        if (this@newPlatformDescriptor is TestSuite) {
            testElementChildren.forEach { addChild(it.newPlatformDescriptor(uniqueId)) }
        }
    }
}

private val TestElement.platformDescriptor: AbstractTestDescriptor
    get() =
        checkNotNull(testElementDescriptors[this]) { "$this is missing its TestDescriptor" }

private val TestElementEvent.Finished.executionResult: TestExecutionResult
    get() =
        when (throwable) {
            null -> TestExecutionResult.successful()
            is FailFastException -> TestExecutionResult.aborted(throwable)
            else -> TestExecutionResult.failed(throwable)
        }

private fun log(message: () -> String) {
    logDebug(message)
}
