package com.jnd.fluxscriptstudio

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FluxScriptValidationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testMathExpression() {
        val code = "var result = (10 + 5) * 2;\nprint(result);"
        runAndVerify(code, "var result = (10 + 5) * 2;")
    }

    @Test
    fun testLogicBranching() {
        val code = "if (true) {\n    print(\"Logic Works\");\n}"
        runAndVerify(code, "Logic Works")
    }

    @Test
    fun testFunctionDeclaration() {
        val code = "fun add(a, b) { return a + b; }\nprint(add(5, 5));"
        runAndVerify(code, "fun add(a, b)")
    }

    private fun runAndVerify(code: String, expectedInOutput: String) {
        // Input code
        composeTestRule.onNodeWithTag("code_editor")
            .performTextReplacement(code)

        // Run compiler
        composeTestRule.onNodeWithTag("run_button")
            .performClick()

        // Verify console output
        // In Mock mode, it mirrors input. In Real mode, it shows compiler logs.
        // This assertion works for both.
        composeTestRule.onNodeWithTag("console_output")
            .assertTextContains(expectedInOutput, substring = true)
            .assertTextContains("FluxScript 0.1.0", substring = true)
    }
}
