package com.example

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import de.infix.testBalloon.framework.core.JUnit4RulesContext
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.integration.robolectric.RobolectricTestSuiteContent
import de.infix.testBalloon.integration.robolectric.robolectricTestSuite
import kotlin.getValue

val ComposeRobolectricTests by testSuite {
    robolectricTestSuite<ComposeRobolectricTestsContent>("Compose Roboelectric")
}

class ComposeRobolectricTestsContent :
    RobolectricTestSuiteContent({
        testFixture {
            object : JUnit4RulesContext() {
                val composeTestRule = rule(createComposeRule())
            }
        } asContextForEach {
            test("renders text") {
                println("before")
                composeTestRule.setContent { Text("Hello") }
                // fails before reaching this point
                println("after")
            }
        }
    })
