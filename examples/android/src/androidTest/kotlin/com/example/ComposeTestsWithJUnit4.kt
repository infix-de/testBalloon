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
import org.junit.Rule
import org.junit.Test

class ComposeTestsWithJUnit4 {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun setup() {
        composeTestRule.setContent {
            ComposableUnderTest()
        }

        composeTestRule.onNodeWithText("Idle").assertExists()
    }

    @Test
    fun click() {
        composeTestRule.setContent {
            ComposableUnderTest()
        }

        composeTestRule.onNodeWithText("Button").performClick()
        composeTestRule.onNodeWithText("Success").assertExists()
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
