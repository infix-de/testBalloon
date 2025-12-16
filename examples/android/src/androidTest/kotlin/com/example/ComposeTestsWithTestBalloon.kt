package com.example

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.infix.testBalloon.framework.core.JUnit4RulesContext
import de.infix.testBalloon.framework.core.TestBalloonExperimentalApi
import de.infix.testBalloon.framework.core.testSuite

@OptIn(TestBalloonExperimentalApi::class)
val ComposeTestsWithTestBalloon by testSuite {
    testFixture {
        object : JUnit4RulesContext() {
            val composeTestRule = rule(createComposeRule())
        }
    } asContextForEach {
        test("setup") {
            composeTestRule.setContent {
                ComposableUnderTest()
            }

            composeTestRule.onNodeWithText("Idle").assertExists()
        }

        testSuite("nested suite") {
            test("click") {
                composeTestRule.setContent {
                    ComposableUnderTest()
                }

                composeTestRule.onNodeWithText("Button").performClick()
                composeTestRule.onNodeWithText("Success").assertExists()
            }
        }
    }
}

@Composable
private fun ComposableUnderTest() {
    var text by remember { mutableStateOf("Idle") }
    Button(onClick = { text = "Success" }) {
        Text("Button")
    }
    Text(text)
}
