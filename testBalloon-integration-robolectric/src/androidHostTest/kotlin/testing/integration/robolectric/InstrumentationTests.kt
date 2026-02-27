// NOTE: This package name intentionally deviates from the usual scheme, as packages starting with
// "de.infix.testBalloon" would be excluded from Robolectric instrumentation.
package testing.integration.robolectric

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.integration.robolectric.ApplicationLifetime
import de.infix.testBalloon.integration.robolectric.RobolectricTestSuiteContent
import de.infix.testBalloon.integration.robolectric.robolectric
import de.infix.testBalloon.integration.robolectric.robolectricTestSuite
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

val InstrumentationTests by testSuite {
    for (apiLevel in listOf(34, 28)) {
        robolectricTestSuite(
            "API level $apiLevel",
            InstrumentationTestsApiLevelContent::class,
            arguments = arrayOf(apiLevel),
            testConfig = TestConfig.robolectric {
                sdk = apiLevel
                applicationLifetime = ApplicationLifetime.RobolectricTestSuite
                portableClasses += InstrumentationTestsPortableClass::class
            }
        )
    }

    robolectricTestSuite(
        "Portability",
        InstrumentationTestsPortabilityContent::class,
        arguments = arrayOf(InstrumentationTestsPortableClass(), InstrumentationTestsNonPortableClass()),
        testConfig = TestConfig.robolectric {
            portableClasses += InstrumentationTestsPortableClass::class
        }
    )
}

internal class InstrumentationTestsApiLevelContent(val apiLevel: Int) :
    RobolectricTestSuiteContent({
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
    RobolectricTestSuiteContent({
        test("portable class") {
            assertTrue(portableObject is InstrumentationTestsPortableClass)
        }

        test("non-portable class") {
            assertTrue(nonPortableObject !is InstrumentationTestsNonPortableClass)
        }
    })

internal class InstrumentationTestsPortableClass(val name: String = "portable")
internal class InstrumentationTestsNonPortableClass(val name: String = "non-portable")
