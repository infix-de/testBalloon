package com.example

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestFixture
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.integration.roboelectric.ApplicationLifetime
import de.infix.testBalloon.integration.roboelectric.RoboelectricTestSuiteContent
import de.infix.testBalloon.integration.roboelectric.roboelectric
import de.infix.testBalloon.integration.roboelectric.roboelectricTestSuite
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MyType(var i: Int = 0)

val RoboelectricWithTestBalloon by testSuite {
    for (apiLevel in listOf(36, 34, 28)) {
        testSuite(
            "API level $apiLevel",
            testConfig = TestConfig.roboelectric {
                sdk = apiLevel
                qualifiers = "xlarge-port"
                applicationLifetime = ApplicationLifetime.RoboelectricTestSuite
                portableClasses += MyType::class
            }
        ) {
            val myFixture = testFixture { MyType() }
            roboelectricTestSuite("Portrait", MyRoboelectricTests::class, arguments = arrayOf(myFixture))
            roboelectricTestSuite(
                "Landscape",
                MyRoboelectricTests::class,
                arguments = arrayOf(myFixture),
                testConfig = TestConfig.roboelectric {
                    qualifiers = "+land"
                    applicationLifetime = ApplicationLifetime.Test
                }
            )
        }
    }
}

internal class MyRoboelectricTests(private val myFixture: TestFixture<MyType>) :
    RoboelectricTestSuiteContent({
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
