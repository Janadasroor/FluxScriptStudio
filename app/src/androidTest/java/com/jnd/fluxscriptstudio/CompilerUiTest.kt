package com.jnd.fluxscriptstudio

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompilerUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testCompileFlow() {
        val testCode = "print(\"Hello from CI Test\");"
        
        // Use testTag for reliable node finding
        composeTestRule.onNodeWithTag("code_editor")
            .performTextReplacement(testCode)

        // Click the Run button via testTag
        composeTestRule.onNodeWithTag("run_button")
            .performClick()

        // Check if the console output contains the result
        composeTestRule.onNodeWithTag("console_output")
            .assertTextContains("Hello from CI Test", substring = true)
    }

    @Test
    fun testClearButton() {
        // Initial text should exist
        composeTestRule.onNodeWithTag("code_editor")
            .assertTextContains("FluxScript", substring = true)

        // Click the clear button
        composeTestRule.onNodeWithTag("clear_button")
            .performClick()

        // Verify editor no longer contains the initial text
        composeTestRule.onNodeWithTag("code_editor")
            .assertTextDoesNotContain("FluxScript")
    }
}
