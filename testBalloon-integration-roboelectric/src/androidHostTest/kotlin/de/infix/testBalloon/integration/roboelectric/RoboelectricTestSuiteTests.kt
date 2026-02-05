package de.infix.testBalloon.integration.roboelectric

import de.infix.testBalloon.framework.core.internal.FrameworkTestUtilities
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalTestingApi
import kotlin.getValue
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFails

@OptIn(TestBalloonInternalTestingApi::class)
class RoboelectricTestSuiteTests {
    @BeforeTest
    fun initialize() {
        // TestBalloon tests reside in the same module: Guarantee a clean state at the start.
        FrameworkTestUtilities.resetTestFramework()
    }

    @Test
    fun `roboelectricTestSuite calls must not nest`() = FrameworkTestUtilities.withTestFramework {
        val topLevelSuite by testSuite("topLevel") {
            roboelectricTestSuite("outer roboelectric suite", RoboelectricTestSuiteTestsOuterContent::class)
        }

        assertFails {
            FrameworkTestUtilities.withTestReport(topLevelSuite) {}
        }.also {
            assertContains(it.cause?.message ?: "", "Roboelectric test suites must not nest")
        }
    }
}

internal class RoboelectricTestSuiteTestsOuterContent :
    RoboelectricTestSuiteContent({
        roboelectricTestSuite("inner roboelectric suite", RoboelectricTestSuiteTestsInnerContent::class)
    })

internal class RoboelectricTestSuiteTestsInnerContent :
    RoboelectricTestSuiteContent({
        test("dummy") {}
    })
