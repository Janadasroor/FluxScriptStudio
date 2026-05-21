package com.jnd.fluxscriptstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnd.fluxscriptstudio.ui.theme.FluxScriptStudioTheme
import java.util.regex.Pattern

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FluxScriptStudioTheme {
                CompilerScreen(
                    onCompile = { source -> compileFluxScript(source) }
                )
            }
        }
    }

    external fun compileFluxScript(source: String): String

    companion object {
        init {
            System.loadLibrary("fluxscriptstudio")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompilerScreen(onCompile: (String) -> String) {
    var code by remember { 
        mutableStateOf("// FluxScript 0.1.0\nfun main() {\n    print(\"Hello FluxScript!\");\n}") 
    }
    var result by remember { mutableStateOf("Ready to compile...") }
    var showSamples by remember { mutableStateOf(false) }
    val consoleScrollState = rememberScrollState()

    val samples = mapOf(
        "Hello World" to "fun main() {\n    print(\"Hello FluxScript!\");\n}",
        "Fibonacci" to "fun fib(n) {\n    if (n < 2) return n;\n    return fib(n - 1) + fib(n - 2);\n}\n\nprint(fib(10));",
        "Variables" to "var x = 10;\nvar y = 20;\nvar sum = x + y;\nprint(\"Sum is: \" + sum);",
        "Loops" to "for (var i = 0; i < 5; i++) {\n    print(\"Iteration: \" + i);\n}"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FluxScript Studio", fontWeight = FontWeight.Bold) },
                actions = {
                    Box {
                        IconButton(onClick = { showSamples = true }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Samples")
                        }
                        DropdownMenu(expanded = showSamples, onDismissRequest = { showSamples = false }) {
                            samples.forEach { (name, snippet) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        code = snippet
                                        showSamples = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { code = "" }, modifier = Modifier.testTag("clear_button")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Clear")
                    }
                    Button(
                        onClick = { result = onCompile(code) },
                        modifier = Modifier.padding(end = 8.dp).testTag("run_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Run")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Box(modifier = Modifier.weight(0.6f).fillMaxWidth()) {
                CodeEditor(
                    value = code,
                    onValueChange = { code = it },
                    modifier = Modifier.fillMaxSize().testTag("code_editor")
                )
            }
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier.weight(0.4f).fillMaxWidth().background(Color(0xFF121212)).padding(12.dp).verticalScroll(consoleScrollState)
            ) {
                Text("CONSOLE OUTPUT", style = TextStyle(color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                Spacer(Modifier.height(8.dp))
                Text(text = result, modifier = Modifier.testTag("console_output"), style = TextStyle(color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace, lineHeight = 18.sp))
            }
        }
    }
}

@Composable
fun CodeEditor(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val lineCount = value.split("\n").size
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    Row(modifier = modifier.verticalScroll(verticalScrollState).background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.width(44.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).padding(top = 16.dp), horizontalAlignment = Alignment.End) {
            for (i in 1..lineCount) {
                Text(text = i.toString(), style = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), fontSize = 13.sp, fontFamily = FontFamily.Monospace, textAlign = androidx.compose.ui.text.style.TextAlign.End), modifier = Modifier.padding(end = 10.dp))
            }
        }
        Box(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.widthIn(min = 1000.dp).padding(top = 16.dp, start = 8.dp, end = 16.dp, bottom = 16.dp),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontFamily = FontFamily.Monospace, lineHeight = 20.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = FluxScriptVisualTransformation()
            )
        }
    }
}

class FluxScriptVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(highlightFluxScript(text.text), OffsetMapping.Identity)
    }

    private fun highlightFluxScript(code: String): AnnotatedString {
        val keywords = Pattern.compile("\\b(fun|var|val|if|else|while|for|return|print|class|true|false)\\b")
        val strings = Pattern.compile("\".*?\"")
        val comments = Pattern.compile("//.*")
        val numbers = Pattern.compile("\\b\\d+\\b")
        
        return buildAnnotatedString {
            append(code)
            
            val stringRanges = mutableListOf<IntRange>()
            val sm = strings.matcher(code)
            while (sm.find()) {
                addStyle(SpanStyle(color = Color(0xFF4CAF50)), sm.start(), sm.end())
                stringRanges.add(sm.start() until sm.end())
            }

            val cm = comments.matcher(code)
            while (cm.find()) {
                addStyle(SpanStyle(color = Color.Gray), cm.start(), cm.end())
            }

            val km = keywords.matcher(code)
            while (km.find()) {
                if (stringRanges.none { km.start() in it }) {
                    addStyle(SpanStyle(color = Color(0xFF9C27B0), fontWeight = FontWeight.Bold), km.start(), km.end())
                }
            }

            val nm = numbers.matcher(code)
            while (nm.find()) {
                if (stringRanges.none { nm.start() in it }) {
                    addStyle(SpanStyle(color = Color(0xFFFF9800)), nm.start(), nm.end())
                }
            }
        }
    }
}
