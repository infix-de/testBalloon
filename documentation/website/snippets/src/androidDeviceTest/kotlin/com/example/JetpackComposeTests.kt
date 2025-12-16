@file:Suppress("ktlint:standard:max-line-length", "LongLine")

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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runners.model.Statement

@Composable
private fun ComposableUnderTest() {
    var text by remember { mutableStateOf("Idle") }
    Button(onClick = { text = "Success" }) {
        Text("Button")
    }
    Text(text)
}

// --8<-- [start:junit-jetpackCompose]
class JetpackComposeWithJUnit4 {
    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val myCustomRule = myCustomRule()

    @Test
    fun click() {
        composeTestRule.setContent {
            ComposableUnderTest()
        }

        composeTestRule.onNodeWithText("Button").performClick()
        composeTestRule.onNodeWithText("Success").assertExists()
    }
}
// --8<-- [end:junit-jetpackCompose]

@Suppress("ktlint:standard:annotation-spacing")
@OptIn(TestBalloonExperimentalApi::class)
// --8<-- [start:testballoon-jetpackCompose]
val JetpackComposeWithTestBalloon by testSuite {
    testFixture {
        object : JUnit4RulesContext() { // (1)!
            val composeTestRule = rule(createComposeRule()) // (2)!
            val myCustomRule = rule(myCustomRule())
        }
    } asContextForEach {
        test("click") {
            composeTestRule.setContent {
                ComposableUnderTest()
            }

            composeTestRule.onNodeWithText("Button").performClick()
            composeTestRule.onNodeWithText("Success").assertExists()
        }
    }
}
// --8<-- [end:testballoon-jetpackCompose]

private fun myCustomRule(): TestRule = TestRule { base, description ->
    object : Statement() {
        override fun evaluate() {
            println("Before test: $description")
            try {
                base.evaluate()
            } finally {
                println("After test: $description")
            }
        }
    }
}
