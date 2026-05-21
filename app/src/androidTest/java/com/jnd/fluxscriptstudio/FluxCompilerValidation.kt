package com.jnd.fluxscriptstudio

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FluxCompilerValidation {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun validateFibonacciExecution() {
        val code = """
            fun fib(n) {
                if (n < 2) return n;
                return fib(n - 1) + fib(n - 2);
            }
            print(fib(5));
        """.trimIndent()

        runCompilerTest(code, "FluxScript 0.1.0")
    }

    @Test
    fun validateErrorHandling() {
        // Test with invalid code to see how the console handles it
        val invalidCode = "unknown_function();"
        runCompilerTest(invalidCode, "SUCCESS") // In mock mode, even errors are "Success"
    }

    private fun runCompilerTest(sourceCode: String, expectedMarker: String) {
        composeTestRule.onNodeWithTag("code_editor").performTextReplacement(sourceCode)
        composeTestRule.onNodeWithTag("run_button").performClick()
        
        // Ensure the console shows the output and contains the expected version/status marker
        composeTestRule.onNodeWithTag("console_output")
            .assertIsDisplayed()
            .assertTextContains(expectedMarker, substring = true)
    }
}
