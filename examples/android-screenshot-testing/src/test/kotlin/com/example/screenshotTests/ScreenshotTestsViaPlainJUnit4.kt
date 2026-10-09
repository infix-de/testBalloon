package com.example.screenshotTests

import com.example.AboutScreen
import com.example.AboutState
import com.example.testLibrary.JUnitScreenshotTestSeries
import com.example.testLibrary.SCREENSHOT_SDK
import com.example.testLibrary.versionFake
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK])
class ScreenshotTestsViaPlainJUnit4(
    @Suppress("unused") private val testName: String,
    parameters: Parameters<AboutState>
) : JUnitScreenshotTestSeries<AboutState>(parameters) {
    companion object {
        @Suppress("unused")
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun parameterCombinations() = parameterCombinations(
            seriesName = "About screen",
            stateVariants = mapOf(
                "with version" to AboutState(version = versionFake),
                "without version" to AboutState(version = null),
                "with maps" to AboutState(version = versionFake, mapsEnabled = true)
            )
        )
    }

    @Test
    fun captureScreenshot() {
        captureScreenshot { state ->
            AboutScreen(state = state, onClose = {})
        }
    }
}
