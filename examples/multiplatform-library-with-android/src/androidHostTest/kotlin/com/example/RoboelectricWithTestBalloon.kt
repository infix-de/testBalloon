package com.example

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.integration.roboelectric.ApplicationLifetime
import de.infix.testBalloon.integration.roboelectric.RoboelectricTestSuiteContent
import de.infix.testBalloon.integration.roboelectric.roboelectric
import de.infix.testBalloon.integration.roboelectric.roboelectricTestSuite
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

val RoboelectricWithTestBalloon by testSuite {
    for (apiLevel in listOf(36, 34, 28)) {
        roboelectricTestSuite(
            "API level $apiLevel",
            MyRoboelectricTests::class,
            testConfig = TestConfig.roboelectric {
                sdk = apiLevel
                qualifiers = "xlarge-port"
                applicationLifetime = ApplicationLifetime.RoboelectricTestSuite
            }
        )
    }
}

internal class MyRoboelectricTests :
    RoboelectricTestSuiteContent({
        test("Application exists") {
            val application = getApplicationContext<Application>()
            assertNotNull(application)
        }

        testSuite("Details") {
            test("Motion event sources are supported, but not present") {
                assertEquals(0, AccessibilityServiceInfo().motionEventSources) // Added in API level 34
            }

            test("Screen size is 'xlarge'") {
                assertContains(RuntimeEnvironment.getQualifiers(), "xlarge")
            }
        }
    })
