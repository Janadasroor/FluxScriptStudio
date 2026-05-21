package com.jnd.fluxscriptstudio

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VisualTransformationTest {

    private val transformer = FluxScriptVisualTransformation()

    @Test
    fun testKeywordHighlighting() {
        val code = "fun main() { var x = 10; }"
        val transformed = transformer.filter(AnnotatedString(code)).text
        
        // "fun" (0-3)
        val funStyle = transformed.spanStyles.find { it.start == 0 && it.end == 3 }
        assertEquals(Color(0xFF9C27B0), funStyle?.item?.color)
        
        // "var" (13-16)
        val varStyle = transformed.spanStyles.find { it.start == 13 && it.end == 16 }
        assertEquals(Color(0xFF9C27B0), varStyle?.item?.color)
    }

    @Test
    fun testStringVsNumberHighlighting() {
        // "123" inside a string should NOT be colored as a number
        val code = "print(\"count 123\");"
        val transformed = transformer.filter(AnnotatedString(code)).text
        
        // The entire string should be Green (0xFF4CAF50)
        val stringStyle = transformed.spanStyles.find { it.start == 6 && it.end == 17 }
        assertEquals(Color(0xFF4CAF50), stringStyle?.item?.color)
        
        // Ensure no Orange (number color) exists inside that range
        val overlappingNumber = transformed.spanStyles.find { 
            it.item.color == Color(0xFFFF9800) && it.start >= 6 && it.end <= 17 
        }
        assertNull("Numbers inside strings should not be highlighted separately", overlappingNumber)
    }

    @Test
    fun testCommentHighlighting() {
        val code = "var x = 1; // This is a comment"
        val transformed = transformer.filter(AnnotatedString(code)).text
        
        val commentStyle = transformed.spanStyles.find { it.start == 11 }
        assertEquals(Color.Gray, commentStyle?.item?.color)
    }
}
