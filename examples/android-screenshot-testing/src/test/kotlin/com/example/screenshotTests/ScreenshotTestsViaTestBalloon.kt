package com.example.screenshotTests

import com.example.AboutScreen
import com.example.AboutState
import com.example.testLibrary.ScreenshotTestSeries
import com.example.testLibrary.screenshotTestSuite
import com.example.testLibrary.versionFake
import de.infix.testBalloon.framework.core.testSuite

val TestBalloonTests by testSuite {
    screenshotTestSuite<AboutScreenTestSeries>("About screen")
}

class AboutScreenTestSeries(parameters: Parameters) :
    ScreenshotTestSeries<AboutState>(
        parameters = parameters,
        content = { state ->
            AboutScreen(state = state, onClose = {})
        },
        stateVariants = mapOf(
            "with version" to AboutState(version = versionFake),
            "without version" to AboutState(version = null),
            "with maps" to AboutState(version = versionFake, mapsEnabled = true)
        )
    )
