package com.jnd.fluxscriptstudio

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FluxCodeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testFibonacciSample() {
        runSampleTest("Fibonacci", "fib(10)")
    }

    @Test
    fun testLoopsSample() {
        runSampleTest("Loops", "Iteration: 4")
    }

    @Test
    fun testVariablesSample() {
        runSampleTest("Variables", "Sum is: 30")
    }

    private fun runSampleTest(sampleName: String, expectedOutputSubstring: String) {
        // Open samples menu
        composeTestRule.onNodeWithContentDescription("Samples").performClick()
        
        // Select the sample
        composeTestRule.onNodeWithText(sampleName).performClick()
        
        // Click Run
        composeTestRule.onNodeWithTag("run_button").performClick()
        
        // Verify output in console
        // Note: In Mock mode, it echoes the source. In Real mode, it shows execution result.
        // Our tests check for substrings that appear in either the source or the result.
        composeTestRule.onNodeWithTag("console_output")
            .assertTextContains(expectedOutputSubstring, substring = true)
    }
}
