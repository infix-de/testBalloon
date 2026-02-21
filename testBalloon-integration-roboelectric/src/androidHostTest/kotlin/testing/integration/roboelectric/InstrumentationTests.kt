// NOTE: This package name intentionally deviates from the usual scheme, as packages starting with
// "de.infix.testBalloon" would be excluded from Roboelectric instrumentation.
package testing.integration.roboelectric

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.integration.roboelectric.ApplicationLifetime
import de.infix.testBalloon.integration.roboelectric.RoboelectricTestSuiteContent
import de.infix.testBalloon.integration.roboelectric.roboelectric
import de.infix.testBalloon.integration.roboelectric.roboelectricTestSuite
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

val InstrumentationTests by testSuite {
    for (apiLevel in listOf(34, 28)) {
        roboelectricTestSuite(
            "API level $apiLevel",
            InstrumentationTestsApiLevelContent::class,
            arguments = arrayOf(apiLevel),
            testConfig = TestConfig.roboelectric {
                sdk = apiLevel
                applicationLifetime = ApplicationLifetime.RoboelectricTestSuite
                portableClasses += InstrumentationTestsPortableClass::class
            }
        )
    }

    roboelectricTestSuite(
        "Portability",
        InstrumentationTestsPortabilityContent::class,
        arguments = arrayOf(InstrumentationTestsPortableClass(), InstrumentationTestsNonPortableClass()),
        testConfig = TestConfig.roboelectric {
            portableClasses += InstrumentationTestsPortableClass::class
        }
    )
}

internal class InstrumentationTestsApiLevelContent(val apiLevel: Int) :
    RoboelectricTestSuiteContent({
        test("Application exists") {
            assertNotNull(getApplicationContext<Application>())
        }

        if (apiLevel >= 34) {
            test("motion event sources API is present") {
                assertEquals(0, AccessibilityServiceInfo().motionEventSources) // Added in API level 34
            }
        } else {
            test("motion event sources API is not present") {
                assertFailsWith(NoSuchMethodError::class) {
                    assertEquals(0, AccessibilityServiceInfo().motionEventSources) // Added in API level 34
                }
            }
        }
    })

internal class InstrumentationTestsPortabilityContent(val portableObject: Any, val nonPortableObject: Any) :
    RoboelectricTestSuiteContent({
        test("portable class") {
            assertTrue(portableObject is InstrumentationTestsPortableClass)
        }

        test("non-portable class") {
            assertTrue(nonPortableObject !is InstrumentationTestsNonPortableClass)
        }
    })

internal class InstrumentationTestsPortableClass(val name: String = "portable")
internal class InstrumentationTestsNonPortableClass(val name: String = "non-portable")
