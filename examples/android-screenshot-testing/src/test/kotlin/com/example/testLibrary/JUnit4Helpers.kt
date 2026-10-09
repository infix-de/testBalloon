package com.example.testLibrary

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.robolectric.RuntimeEnvironment

/**
 * A plain JUnit screenshot test series.
 *
 * Its subclass must have a constructor accepting the following parameters:
 * ```
 *     private val testName: String,
 *     parameters: Parameters<State>
 * ```
 *
 * It must also provide parameters as follows:
 * ```
 *     companion object {
 *         @JvmStatic
 *         @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
 *         fun parameterCombinations() = parameterCombinations(...)
 *     }
 * ```
 *
 * and define a test function as follows:
 * ```
 *     @Test
 *     fun captureScreenshot() {
 *         captureScreenshot { state ->
 *             // @Composable content according to `state`.
 *         }
 *     }
 * ```
 */
abstract class JUnitScreenshotTestSeries<State : Any>(val parameters: Parameters<State>) {
    @get:Rule
    internal val composeTestRule = createComposeRule()

    companion object {
        /**
         * Returns the test parameters for all combinations of [stateVariants]
         * and [deviceConfigurations].
         */
        fun <State : Any> parameterCombinations(
            seriesName: String,
            stateVariants: Map<String, State>,
            deviceConfigurations: List<DeviceConfiguration> =
                DeviceConfiguration.entries
        ): List<Array<Any>> = buildList {
            for (deviceConfiguration in deviceConfigurations) {
                for ((stateName, state) in stateVariants) {
                    val testName = seriesName +
                        "-${deviceConfiguration.name.lowercase()}-$stateName"
                    add(
                        arrayOf(
                            testName,
                            Parameters(testName, state, deviceConfiguration)
                        )
                    )
                }
            }
        }
    }

    fun captureScreenshot(content: @Composable (State) -> Unit) {
        with(parameters) {
            RuntimeEnvironment.setQualifiers(deviceConfiguration.qualifiers)
            RuntimeEnvironment.setFontScale(deviceConfiguration.fontScale)

            composeTestRule.setContent {
                TestContent(deviceConfiguration.isDark) {
                    content(state)
                }
            }

            composeTestRule.onRoot().captureRoboImage(
                referenceImageFilePath("junit4/$testName")
            )
        }
    }

    class Parameters<State : Any>(val testName: String, val state: State, val deviceConfiguration: DeviceConfiguration)
}
