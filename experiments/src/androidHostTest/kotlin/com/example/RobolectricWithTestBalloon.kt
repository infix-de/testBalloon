package com.example

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestFixture
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.integration.robolectric.ApplicationLifetime
import de.infix.testBalloon.integration.robolectric.RobolectricTestSuiteContent
import de.infix.testBalloon.integration.robolectric.robolectric
import de.infix.testBalloon.integration.robolectric.robolectricTestSuite
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MyType(var i: Int = 0)

val RobolectricWithTestBalloon by testSuite {
    for (apiLevel in listOf(36, 34, 28)) {
        testSuite(
            "API level $apiLevel",
            testConfig = TestConfig.robolectric {
                sdk = apiLevel
                qualifiers = "xlarge-port"
                applicationLifetime = ApplicationLifetime.RobolectricTestSuite
                portableClasses += MyType::class
            }
        ) {
            val myFixture = testFixture { MyType() }
            robolectricTestSuite(
                "Portrait",
                RobolectricWithTestBalloonVariantContent::class,
                arguments = arrayOf(myFixture)
            )
            robolectricTestSuite(
                "Landscape",
                RobolectricWithTestBalloonVariantContent::class,
                arguments = arrayOf(myFixture),
                testConfig = TestConfig.robolectric {
                    qualifiers = "+land"
                    applicationLifetime = ApplicationLifetime.Test
                }
            )
        }
    }
}

class RobolectricWithTestBalloonVariantContent(private val myFixture: TestFixture<MyType>) :
    RobolectricTestSuiteContent({
        test("Application exists") {
            val application = getApplicationContext<Application>()
            assertNotNull(application)
            println("f=${++myFixture().i}")
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
