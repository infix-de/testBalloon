package de.infix.testBalloon.integration.robolectric

import de.infix.testBalloon.framework.core.internal.FrameworkTestUtilities
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalTestingApi
import kotlin.getValue
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFails

@OptIn(TestBalloonInternalTestingApi::class)
class RobolectricTestSuiteTests {
    @BeforeTest
    fun initialize() {
        // TestBalloon tests reside in the same module: Guarantee a clean state at the start.
        FrameworkTestUtilities.resetTestFramework()
    }

    @Test
    fun `robolectricTestSuite calls must not nest`() = FrameworkTestUtilities.withTestFramework {
        val topLevelSuite by testSuite(propertyFqn = "topLevel") {
            robolectricTestSuite<RobolectricTestSuiteTestsOuterContent>("outer robolectric suite")
        }

        assertFails {
            FrameworkTestUtilities.withTestReport(topLevelSuite) {}
        }.also {
            assertContains(it.cause?.message ?: "", "Robolectric test suites must not nest")
        }
    }
}

internal class RobolectricTestSuiteTestsOuterContent :
    RobolectricTestSuiteContent({
        robolectricTestSuite<RobolectricTestSuiteTestsInnerContent>("inner robolectric suite")
    })

internal class RobolectricTestSuiteTestsInnerContent :
    RobolectricTestSuiteContent({
        test("dummy") {}
    })
