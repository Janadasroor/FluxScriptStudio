package com.jnd.fluxscriptstudio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmokeTest {

    @Test
    fun testSmokeTestAsset() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val assetManager = appContext.assets
        
        // Read the smoke test file from assets
        val code = assetManager.open("smoke_test.flux").bufferedReader().use { it.readText() }
        
        // Load the library and call the native method
        // We can use ActivityScenario to get the activity instance
        androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val result = activity.compileFluxScript(code)
                
                // Basic validation of the result
                assertTrue("Result should contain version info", result.contains("FluxScript 0.1.0"))
                // In mock mode, it echoes the source. In real mode, it would contain output.
                // Our mock specifically says "Execution Result for:"
                assertTrue("Result should indicate success", result.contains("SUCCESS"))
            }
        }
    }
}
