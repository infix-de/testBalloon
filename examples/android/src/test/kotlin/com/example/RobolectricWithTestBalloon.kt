package com.example

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import de.infix.testBalloon.framework.core.JUnit4RulesContext
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
        robolectricTestSuite<RobolectricWithTestBalloonApiLevelContent>(
            "API level $apiLevel",
            testConfig = TestConfig.robolectric {
                sdk = apiLevel
                qualifiers = "xlarge-port"
                applicationLifetime = ApplicationLifetime.RobolectricTestSuite
            }
        )
    }
}

class RobolectricWithTestBalloonApiLevelContent :
    RobolectricTestSuiteContent({
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

val RobolectricComposeWithTestBalloon by testSuite {
    robolectricTestSuite<RobolectricComposeWithTestBalloonContent>("Compose rule")
}

class RobolectricComposeWithTestBalloonContent :
    RobolectricTestSuiteContent({
        testFixture {
            object : JUnit4RulesContext() {
                val composeTestRule = rule(createComposeRule())
            }
        } asContextForEach {
            test("renders text") {
                composeTestRule.setContent { Text("Hello") }
            }
        }
    })
