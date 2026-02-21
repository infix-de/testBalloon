package testing.integration.roboelectric

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.integration.roboelectric.ApplicationLifetime
import de.infix.testBalloon.integration.roboelectric.RoboelectricTestSuiteContent
import de.infix.testBalloon.integration.roboelectric.roboelectric
import de.infix.testBalloon.integration.roboelectric.roboelectricContext
import de.infix.testBalloon.integration.roboelectric.roboelectricTestSuite
import kotlin.test.assertEquals

val TestConfigTests by testSuite {
    val upperLevelFontScale = 1.011f
    val upperLevelApplicationLifetime = ApplicationLifetime.RoboelectricTestSuite

    testSuite(
        "Settings inheritance",
        testConfig = TestConfig.roboelectric {
            fontScale = upperLevelFontScale
            applicationLifetime = upperLevelApplicationLifetime
        }
    ) {
        val lowerLevelFontScale = 1.021f.also { check(it != upperLevelFontScale) }
        val lowerLevelApplicationLifetime = ApplicationLifetime.Test.also { check(it != upperLevelApplicationLifetime) }

        roboelectricTestSuite(
            "initial setting",
            TestConfigFontScaleTestsContent::class,
            arguments = arrayOf(upperLevelFontScale, upperLevelApplicationLifetime)
        )
        roboelectricTestSuite(
            "overriding at a lower level",
            TestConfigFontScaleTestsContent::class,
            arguments = arrayOf(lowerLevelFontScale, lowerLevelApplicationLifetime),
            testConfig = TestConfig.roboelectric {
                fontScale = lowerLevelFontScale
                applicationLifetime = lowerLevelApplicationLifetime
            }
        )
        roboelectricTestSuite(
            "override preserves the parent's setting",
            TestConfigFontScaleTestsContent::class,
            arguments = arrayOf(upperLevelFontScale, upperLevelApplicationLifetime)
        )
    }
}

internal class TestConfigFontScaleTestsContent(
    val expectedFontScale: Float,
    val expectedApplicationLifetime: ApplicationLifetime
) : RoboelectricTestSuiteContent({
    test("via Roboelectric Config") {
        assertEquals(expectedFontScale, testSuiteInScope.roboelectricContext.config.fontScale)
    }

    test("via RoboelectricSettings") {
        assertEquals(expectedApplicationLifetime, testSuiteInScope.roboelectricContext.settings?.applicationLifetime)
    }
})
