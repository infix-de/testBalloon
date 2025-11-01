package com.example

import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.ActivityScenarioRule
import de.infix.testBalloon.framework.core.TestBalloonExperimentalApi
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.core.testWithJUnit4Rule
import de.infix.testBalloon.framework.shared.TestDiscoverable

val ComposeTestsWithTestBalloon by testSuite {
    @Composable
    fun ComposableUnderTest() {
        var text by remember { mutableStateOf("Idle") }
        Button(onClick = { text = "Success" }) {
            Text("Button")
        }
        Text(text)
    }

    composeTest("setup") {
        composeTestRule.setContent {
            ComposableUnderTest()
        }

        composeTestRule.onNodeWithText("Idle").assertExists()
    }

    composeTest("click") {
        composeTestRule.setContent {
            ComposableUnderTest()
        }

        composeTestRule.onNodeWithText("Button").performClick()
        composeTestRule.awaitIdle() // this is not necessary here, it demonstrates a suspend function invocation
        composeTestRule.onNodeWithText("Success").assertExists()
    }
}

/**
 * Registers a `Test` with a [ComposeTestContext] providing a basic `composeTestRule`.
 */
@TestDiscoverable
@OptIn(TestBalloonExperimentalApi::class)
fun TestSuite.composeTest(
    name: String,
    composeTestRule: ComposeContentTestRule = createComposeRule(),
    action: suspend ComposeTestContext<ComposeContentTestRule>.() -> Unit
) = testWithJUnit4Rule(name, composeTestRule) {
    ComposeTestContext(composeTestRule).action()
}

class ComposeTestContext<Rule>(val composeTestRule: Rule)

/**
 * Registers a `Test` with a [ComposeTestContext] providing an Activity-based `composeTestRule`.
 */
@TestDiscoverable
@OptIn(TestBalloonExperimentalApi::class)
inline fun <reified A : ComponentActivity> TestSuite.androidComposeTest(
    name: String,
    composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<A>, A> = createAndroidComposeRule<A>(),
    noinline action: suspend ComposeTestContext<AndroidComposeTestRule<ActivityScenarioRule<A>, A>>.() -> Unit
) = testWithJUnit4Rule(name, composeTestRule) {
    ComposeTestContext<AndroidComposeTestRule<ActivityScenarioRule<A>, A>>(composeTestRule).action()
}
