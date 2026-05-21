package com.jnd.fluxscriptstudio

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompilerInstrumentationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testNativeCompilerCall() {
        activityRule.scenario.onActivity { activity ->
            val source = "print(\"test\");"
            val result = activity.compileFluxScript(source)
            
            // Verify that the result contains expected version info or mock output
            assertTrue("Result should contain version info", result.contains("FluxScript"))
            assertTrue("Result should contain input source", result.contains("print(\"test\");"))
        }
    }
}
