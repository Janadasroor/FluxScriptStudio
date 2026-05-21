package com.jnd.fluxscriptstudio

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FluxScriptIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testMultiLineCompilation() {
        val multiLineCode = """
            fun main() {
                var x = 10;
                var y = 20;
                print(x + y);
            }
        """.trimIndent()

        // Input multi-line code
        composeTestRule.onNodeWithTag("code_editor")
            .performTextReplacement(multiLineCode)

        // Run compiler
        composeTestRule.onNodeWithTag("run_button")
            .performClick()

        // Verify output exists and contains the code snippet
        composeTestRule.onNodeWithTag("console_output")
            .assertTextContains("print(x + y)", substring = true)
            .assertTextContains("FluxScript 0.1.0", substring = true)
    }

    @Test
    fun testEditorPersistenceAfterCompile() {
        val testCode = "print(\"persistent\");"
        
        composeTestRule.onNodeWithTag("code_editor")
            .performTextReplacement(testCode)
            
        composeTestRule.onNodeWithTag("run_button")
            .performClick()
            
        // Editor should still contain the code
        composeTestRule.onNodeWithTag("code_editor")
            .assertTextContains(testCode)
    }
}
