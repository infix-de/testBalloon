@file:Suppress("ktlint:standard:max-line-length", "LongLine")

package com.example.com.example

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.infix.testBalloon.framework.core.TestBalloonExperimentalApi
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.core.testWithJUnit4Rule
import de.infix.testBalloon.framework.shared.TestRegistering

@Composable
private fun ComposableUnderTest() {
    var text by remember { mutableStateOf("Idle") }
    Button(onClick = { text = "Success" }) {
        Text("Button")
    }
    Text(text)
}

// --8<-- [start:composeTest-click]
val JetpackComposeTests by testSuite {
    composeTest("click") {
        composeTestRule.setContent {
            ComposableUnderTest()
        }

        composeTestRule.onNodeWithText("Button").performClick()
        composeTestRule.onNodeWithText("Success").assertExists()
    }
}
// --8<-- [end:composeTest-click]

/**
 * Registers a `Test` with a [ComposeTestContext] providing a basic `composeTestRule`.
 */
// --8<-- [start:custom-dsl-extension]
@TestRegistering
@OptIn(TestBalloonExperimentalApi::class)
fun TestSuite.composeTest(
    name: String,
    composeTestRule: ComposeContentTestRule = createComposeRule(),
    action: suspend ComposeTestContext<ComposeContentTestRule>.() -> Unit
) = testWithJUnit4Rule(name, composeTestRule) {
    ComposeTestContext(composeTestRule).action()
}

class ComposeTestContext<Rule>(val composeTestRule: Rule)
// --8<-- [end:custom-dsl-extension]
