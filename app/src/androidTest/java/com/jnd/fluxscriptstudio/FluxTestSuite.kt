package com.jnd.fluxscriptstudio

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.core.app.ActivityScenario
import org.junit.runners.Parameterized
import java.util.regex.Pattern

@RunWith(Parameterized::class)
class FluxTestSuite(private val fileName: String) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val assetManager = appContext.assets
            val fluxFiles = assetManager.list("")?.filter { it.endsWith(".flux") } ?: emptyList()
            return fluxFiles.map { arrayOf<Any>(it) }
        }
    }

    @Test
    fun runFluxScript() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val assetManager = appContext.assets

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val code = assetManager.open(fileName).bufferedReader().use { it.readText() }
                val result = activity.compileFluxScript(code)
                
                // Verify the standard header
                assertTrue("Header missing in $fileName", result.contains("FluxScript 0.1.0"))

                // If the file has an '// Expected: ' comment, verify the mock/real output matches it
                val pattern = Pattern.compile("// Expected: (.*)")
                val matcher = pattern.matcher(code)
                if (matcher.find()) {
                    val expectedValue = matcher.group(1)?.trim()
                    assertTrue("Output for $fileName did not contain expected value: $expectedValue. Result: $result", 
                        result.contains(expectedValue!!))
                }
                
                assertTrue("Compilation failed for $fileName. Result: $result", 
                    result.contains("SUCCESS") || result.contains("Success"))
            }
        }
    }
}
