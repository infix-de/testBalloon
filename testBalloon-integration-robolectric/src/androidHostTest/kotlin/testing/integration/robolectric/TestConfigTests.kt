package testing.integration.robolectric

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.integration.robolectric.ApplicationLifetime
import de.infix.testBalloon.integration.robolectric.RobolectricTestSuiteContent
import de.infix.testBalloon.integration.robolectric.internal.robolectricContext
import de.infix.testBalloon.integration.robolectric.robolectric
import de.infix.testBalloon.integration.robolectric.robolectricTestSuite
import kotlin.test.assertEquals

val TestConfigTests by testSuite {
    val upperLevelFontScale = 1.011f
    val upperLevelApplicationLifetime = ApplicationLifetime.RobolectricTestSuite

    testSuite(
        "Settings inheritance",
        testConfig = TestConfig.robolectric {
            fontScale = upperLevelFontScale
            applicationLifetime = upperLevelApplicationLifetime
        }
    ) {
        val lowerLevelFontScale = 1.021f.also { check(it != upperLevelFontScale) }
        val lowerLevelApplicationLifetime = ApplicationLifetime.Test.also { check(it != upperLevelApplicationLifetime) }

        robolectricTestSuite(
            "initial setting",
            TestConfigFontScaleTestsContent::class,
            arguments = arrayOf(upperLevelFontScale, upperLevelApplicationLifetime)
        )
        robolectricTestSuite(
            "overriding at a lower level",
            TestConfigFontScaleTestsContent::class,
            arguments = arrayOf(lowerLevelFontScale, lowerLevelApplicationLifetime),
            testConfig = TestConfig.robolectric {
                fontScale = lowerLevelFontScale
                applicationLifetime = lowerLevelApplicationLifetime
            }
        )
        robolectricTestSuite(
            "override preserves the parent's setting",
            TestConfigFontScaleTestsContent::class,
            arguments = arrayOf(upperLevelFontScale, upperLevelApplicationLifetime)
        )
    }
}

internal class TestConfigFontScaleTestsContent(
    val expectedFontScale: Float,
    val expectedApplicationLifetime: ApplicationLifetime
) : RobolectricTestSuiteContent({
    test("via Robolectric Config") {
        assertEquals(expectedFontScale, testSuiteInScope.robolectricContext.config.fontScale)
    }

    test("via RobolectricSettings") {
        assertEquals(expectedApplicationLifetime, testSuiteInScope.robolectricContext.settings?.applicationLifetime)
    }
})
