@file:Suppress("LongLine", "ktlint:standard:function-literal")

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
// --8<-- [start:roboelectric-test-suite]
        roboelectricTestSuite( // (1)!
            "API level $apiLevel",
            MyRoboelectricTests::class, // (2)!
            testConfig = TestConfig.roboelectric { // (3)!
                sdk = apiLevel
                qualifiers = "xlarge-port"
                applicationLifetime = ApplicationLifetime.RoboelectricTestSuite
            }
        ) // (4)!
// --8<-- [end:roboelectric-test-suite]
    }
}

// --8<-- [start:roboelectric-test-suite-content]
internal class MyRoboelectricTests : // (1)!
    RoboelectricTestSuiteContent({ // (2)!
        test("Application exists") {
            val application = getApplicationContext<Application>()
            assertNotNull(application)
        }

        testSuite("Details") {
            test("Motion event sources are supported, but not present") {
                // Added in API level 34
                assertEquals(0, AccessibilityServiceInfo().motionEventSources)
            }

            test("Screen size is 'xlarge'") {
                assertContains(RuntimeEnvironment.getQualifiers(), "xlarge")
            }
        }
    })
// --8<-- [end:roboelectric-test-suite-content]
