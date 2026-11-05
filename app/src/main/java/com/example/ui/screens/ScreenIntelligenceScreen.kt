package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.StatusTelemetryBadge
import com.example.ui.theme.*
import com.example.vision.ScreenAnalysisResult
import com.example.vision.ScreenIntelligence
import com.example.vision.ScreenPromptPreset
import com.example.voice.JarvisVoiceController
import kotlinx.coroutines.launch

@Composable
fun ScreenIntelligenceScreen(
    screenIntelligence: ScreenIntelligence,
    voiceController: JarvisVoiceController,
    apiKey: String,
    selectedModelId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isSharingActive by screenIntelligence.isSharingActive.collectAsState()
    val hasConsent by screenIntelligence.hasUserConsent.collectAsState()
    val activeScreenshot by screenIntelligence.currentScreenshot.collectAsState()

    var activeQuery by remember { mutableStateOf(ScreenPromptPreset.ANALYZE_SCREEN.prompt) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<ScreenAnalysisResult?>(null) }
    var customPrompt by remember { mutableStateOf("") }

    // Pulsing indicator for active screen sharing
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_sharing")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Photo picker for selecting real screenshots from gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                screenIntelligence.grantConsentAndStartSharing(bitmap)
                // Automatically run active analysis
                coroutineScope.launch {
                    isAnalyzing = true
                    val res = screenIntelligence.analyzeScreen(activeQuery, bitmap, apiKey, selectedModelId)
                    analysisResult = res
                    isAnalyzing = false
                    res.fullAiResponse?.let { voiceController.speak(it) }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading screenshot: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisDarkBg)
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Sharing Status Banner
        if (isSharingActive) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, JarvisCrimson, RoundedCornerShape(12.dp))
                    .testTag("active_screen_sharing_banner"),
                color = JarvisDarkSurface
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(JarvisCrimson.copy(alpha = pulseAlpha))
                        )
                        Column {
                            Text(
                                text = "SCREEN INTELLIGENCE SESSION ACTIVE",
                                color = JarvisCrimson,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Visual telemetry active • User consent granted",
                                color = JarvisOnSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Immediate Stop Sharing Button
                    Button(
                        onClick = { screenIntelligence.stopSharing() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisCrimson,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("stop_screen_sharing_button")
                    ) {
                        Text("Stop Sharing", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Permission & Consent Prompt Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, JarvisBorder, RoundedCornerShape(12.dp)),
                color = JarvisDarkSurface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Consent Notice",
                            tint = JarvisCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "SCREEN UNDERSTANDING AUTHORIZATION",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "JARVIS analyzes screenshots to inspect UI layouts, debug crash exceptions, explain source code, and recommend actions. Screen capture always requires explicit authorization. No recording occurs in the background.",
                        color = JarvisOnSurface,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("pick_screenshot_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Pick Screenshot", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Screenshot", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val demoBmp = screenIntelligence.generateSyntheticScreenBitmap("error")
                                screenIntelligence.grantConsentAndStartSharing(demoBmp)
                                coroutineScope.launch {
                                    isAnalyzing = true
                                    val res = screenIntelligence.analyzeScreen(
                                        ScreenPromptPreset.EXPLAIN_ERROR.prompt,
                                        demoBmp,
                                        apiKey,
                                        selectedModelId
                                    )
                                    analysisResult = res
                                    isAnalyzing = false
                                    res.fullAiResponse?.let { voiceController.speak(it) }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("load_demo_crash_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisDarkSurfaceVariant,
                                contentColor = JarvisGold
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(JarvisBorder)
                            )
                        ) {
                            Text("Load Demo Crash", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Preset Synthetic Templates when active
        if (isSharingActive) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SCREENSHOT SOURCES & TEMPLATES:",
                    color = JarvisCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val bmp = screenIntelligence.generateSyntheticScreenBitmap("error")
                            screenIntelligence.setScreenshot(bmp)
                            coroutineScope.launch {
                                isAnalyzing = true
                                val res = screenIntelligence.analyzeScreen(ScreenPromptPreset.EXPLAIN_ERROR.prompt, bmp, apiKey, selectedModelId)
                                analysisResult = res
                                isAnalyzing = false
                                res.fullAiResponse?.let { voiceController.speak(it) }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisDarkSurfaceVariant, contentColor = JarvisCrimson),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(JarvisBorder))
                    ) {
                        Text("App Crash Trace", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val bmp = screenIntelligence.generateSyntheticScreenBitmap("code")
                            screenIntelligence.setScreenshot(bmp)
                            coroutineScope.launch {
                                isAnalyzing = true
                                val res = screenIntelligence.analyzeScreen(ScreenPromptPreset.EXPLAIN_CODE.prompt, bmp, apiKey, selectedModelId)
                                analysisResult = res
                                isAnalyzing = false
                                res.fullAiResponse?.let { voiceController.speak(it) }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisDarkSurfaceVariant, contentColor = JarvisGold),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(JarvisBorder))
                    ) {
                        Text("Kotlin Source", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisDarkSurfaceVariant, contentColor = JarvisCyan),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(JarvisBorder))
                    ) {
                        Text("Pick Custom", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Preset Analysis Commands
        Text(
            text = "SCREEN DIRECTIVES:",
            color = JarvisCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ScreenPromptPreset.values()) { preset ->
                val isSelected = activeQuery == preset.prompt
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) JarvisCyan.copy(alpha = 0.2f) else JarvisDarkSurface)
                        .border(1.dp, if (isSelected) JarvisCyan else JarvisBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            activeQuery = preset.prompt
                            customPrompt = ""
                            activeScreenshot?.let { bmp ->
                                coroutineScope.launch {
                                    isAnalyzing = true
                                    val res = screenIntelligence.analyzeScreen(preset.prompt, bmp, apiKey, selectedModelId)
                                    analysisResult = res
                                    isAnalyzing = false
                                    res.fullAiResponse?.let { voiceController.speak(it) }
                                }
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = preset.label,
                        color = if (isSelected) JarvisCyan else Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Custom Question Input
        OutlinedTextField(
            value = customPrompt,
            onValueChange = { customPrompt = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("screen_custom_prompt_input"),
            placeholder = {
                Text(
                    text = "Ask question about screen (e.g. 'What button should I click next?')...",
                    color = JarvisOnSurfaceVariant,
                    fontSize = 12.sp
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        val q = customPrompt
                        if (q.isNotBlank()) {
                            activeScreenshot?.let { bmp ->
                                coroutineScope.launch {
                                    isAnalyzing = true
                                    val res = screenIntelligence.analyzeScreen(q, bmp, apiKey, selectedModelId)
                                    analysisResult = res
                                    isAnalyzing = false
                                    res.fullAiResponse?.let { voiceController.speak(it) }
                                }
                            } ?: run {
                                Toast.makeText(context, "Select or generate a screenshot first.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = customPrompt.isNotBlank() && !isAnalyzing
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = JarvisCyan)
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JarvisCyan,
                unfocusedBorderColor = JarvisBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp)
        )

        // Loading Indicator
        if (isAnalyzing) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, JarvisCyan, RoundedCornerShape(10.dp)),
                color = JarvisDarkSurface
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = JarvisCyan, strokeWidth = 2.5.dp)
                    Column {
                        Text(
                            text = "PARSING SCREEN GEOMETRY & OCR",
                            color = JarvisCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Identifying interactive components, stack traces, and layout hierarchy...",
                            color = JarvisOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Active Screenshot Preview & Analysis Output
        activeScreenshot?.let { bmp ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, JarvisBorder, RoundedCornerShape(14.dp)),
                color = JarvisDarkSurface
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE SCREEN BUFFER (${bmp.width}x${bmp.height} PX)",
                            color = JarvisCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(JarvisEmerald.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("USER PERMISSION GRANTED", color = JarvisEmerald, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Screen Capture Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, JarvisBorder, RoundedCornerShape(8.dp))
                    )
                }
            }
        }

        // Analysis Results
        analysisResult?.let { result ->
            // UI Element Mapping Card
            if (result.detectedElements.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, JarvisBorder, RoundedCornerShape(12.dp)),
                    color = JarvisDarkSurface
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "DETECTED INTERFACE ELEMENTS (${result.detectedElements.size})",
                            color = JarvisGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        result.detectedElements.forEach { element ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(JarvisDarkSurfaceVariant)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "[${element.type}] ${element.label}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = element.boundingBoxDesc,
                                        color = JarvisOnSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (element.clickable) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(JarvisCyan.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("CLICKABLE", color = JarvisCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Next Action Recommendation Card
            result.recommendedNextAction?.let { rec ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, JarvisCyan, RoundedCornerShape(12.dp)),
                    color = JarvisDarkSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = "Next Step", tint = JarvisCyan, modifier = Modifier.size(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("RECOMMENDED NEXT ACTION", color = JarvisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(rec, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Full Comprehensive AI Report Card
            result.fullAiResponse?.let { fullText ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, JarvisBorder, RoundedCornerShape(12.dp)),
                    color = JarvisDarkSurface
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SCREEN REASONING & OCR REPORT",
                                color = JarvisCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Row {
                                IconButton(
                                    onClick = { voiceController.speak(fullText) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = JarvisCyan, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("JARVIS Screen OCR", fullText))
                                        Toast.makeText(context, "Copied analysis report", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = JarvisOnSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Divider(color = JarvisBorder, thickness = 0.5.dp)

                        Text(
                            text = fullText,
                            color = Color.White,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}
