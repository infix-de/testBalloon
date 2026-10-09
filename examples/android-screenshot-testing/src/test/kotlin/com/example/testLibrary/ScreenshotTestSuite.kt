package com.example.testLibrary

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import de.infix.testBalloon.framework.core.JUnit4RulesContext
import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.shared.TestRegistering
import de.infix.testBalloon.integration.robolectric.RobolectricTestSuiteContent
import de.infix.testBalloon.integration.robolectric.robolectric
import de.infix.testBalloon.integration.robolectric.robolectricTestSuite
import kotlin.math.roundToLong

/**
 * Registers a screenshot test suite.
 *
 * The series comprises tests  for all combinations of `stateVariants`
 * (see [ScreenshotTestSeries]) and [deviceConfigurations].
 *
 * The `TestSeries` class must accept a single constructor parameter
 * `parameters: ScreenshotTestSeriesParameters`.
 * It will be dynamically loaded by the Robolectric Sandbox class loader for
 * instrumentation. See [robolectricTestSuite] for more details.
 */
@TestRegistering
inline fun <reified TestSeries : ScreenshotTestSeries<*>> TestSuiteScope.screenshotTestSuite(
    name: String,
    deviceConfigurations: List<DeviceConfiguration> = DeviceConfiguration.entries
) = testSuite(
    name,
    testConfig = TestConfig.robolectric {
        portableClasses += setOf(
            DeviceConfiguration::class,
            ScreenshotTestSeries.Parameters::class
        )
    }
) {
    for (deviceConfiguration in deviceConfigurations) {
        robolectricTestSuite<TestSeries>(
            deviceConfiguration.displayName,
            arguments = arrayOf(
                ScreenshotTestSeries.Parameters(name, deviceConfiguration)
            ),
            testConfig = TestConfig.robolectric {
                sdk = SCREENSHOT_SDK
                qualifiers = deviceConfiguration.qualifiers
                fontScale = deviceConfiguration.fontScale
            }
        )
    }
}

/**
 * A screenshot test series for a single device configuration.
 */
abstract class ScreenshotTestSeries<State>(
    parameters: Parameters,
    content: @Composable (State) -> Unit,
    stateVariants: Map<String, State>
) : RobolectricTestSuiteContent({
    with(parameters) {
        testFixture {
            object : JUnit4RulesContext() {
                val composeTestRule = rule(createComposeRule())
            }
        } asContextForEach {
            stateVariants.forEach { (stateName, state) ->
                test(stateName) {
                    composeTestRule.setContent {
                        TestContent(deviceConfiguration.isDark) {
                            content(state)
                        }
                    }

                    composeTestRule.onRoot().captureRoboImage(
                        referenceImageFilePath(
                            "testBalloon/$contentName",
                            stateName,
                            deviceConfiguration.name.lowercase()
                        )
                    )

                    // showMemoryConsumption(it)
                }
            }
        }
    }
}) {
    class Parameters(val contentName: String, val deviceConfiguration: DeviceConfiguration)
}

private var lastTotalMemory: Long? = null

private suspend fun showMemoryConsumption(scope: Test.ExecutionScope) {
    val memory = heapSize()
    lastTotalMemory?.let {
        println("${scope.testElementPath}: +${(memory - it).toMBString()}, total: ${memory.toMBString()}")
    }
    lastTotalMemory = memory
    // delay(0.5.seconds)
}

private fun heapSize(): Long {
    System.gc()
    System.gc()
    return Runtime.getRuntime().run { totalMemory() - freeMemory() }
}

private fun Long.toMBString(): String = "${(toDouble() / 1024 / 1024 * 100).roundToLong().toDouble() / 100.0} MB"
