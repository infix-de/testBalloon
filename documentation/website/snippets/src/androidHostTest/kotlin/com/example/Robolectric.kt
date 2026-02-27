@file:Suppress("LongLine", "ktlint:standard:function-literal")

package com.example

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.integration.robolectric.ApplicationLifetime
import de.infix.testBalloon.integration.robolectric.RobolectricTestSuiteContent
import de.infix.testBalloon.integration.robolectric.robolectric
import de.infix.testBalloon.integration.robolectric.robolectricTestSuite
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

val RobolectricWithTestBalloon by testSuite {
    for (apiLevel in listOf(36, 34, 28)) {
// --8<-- [start:robolectric-test-suite]
        robolectricTestSuite( // (1)!
            "API level $apiLevel",
            MyRobolectricTests::class, // (2)!
            testConfig = TestConfig.robolectric { // (3)!
                sdk = apiLevel
                qualifiers = "xlarge-port"
                applicationLifetime = ApplicationLifetime.RobolectricTestSuite
            }
        ) // (4)!
// --8<-- [end:robolectric-test-suite]
    }
}

// --8<-- [start:robolectric-test-suite-content]
internal class MyRobolectricTests : // (1)!
    RobolectricTestSuiteContent({ // (2)!
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
// --8<-- [end:robolectric-test-suite-content]
