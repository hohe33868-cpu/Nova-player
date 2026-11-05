package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import com.example.ai.ModelCapabilityValidator
import com.example.ui.components.CameraPreviewComposable
import com.example.ui.components.StatusTelemetryBadge
import com.example.ui.theme.*
import com.example.vision.CameraManager
import com.example.vision.VisionAnalysisResult
import com.example.vision.VisionEngine
import com.example.vision.VisionPreset
import com.example.voice.JarvisVoiceController
import kotlinx.coroutines.launch

@Composable
fun VisionScreen(
    cameraManager: CameraManager,
    visionEngine: VisionEngine,
    voiceController: JarvisVoiceController,
    apiKey: String,
    selectedModelId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var activePrompt by remember { mutableStateOf(VisionPreset.WHAT_DO_YOU_SEE.prompt) }
    var customQueryText by remember { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf<VisionAnalysisResult?>(null) }

    val isVisionSupported = remember(selectedModelId) {
        ModelCapabilityValidator.isVisionCapable(selectedModelId)
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisDarkBg)
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Telemetry Status Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusTelemetryBadge(
                label = "Optical Eye",
                statusText = if (hasCameraPermission) "ACTIVE" else "NO PERMISSION",
                color = if (hasCameraPermission) JarvisEmerald else JarvisCrimson
            )

            StatusTelemetryBadge(
                label = "Model",
                statusText = if (isVisionSupported) "VISION READY" else "TEXT ONLY",
                color = if (isVisionSupported) JarvisCyan else JarvisAmber
            )
        }

        // Model Vision Capability Warning if Text-only model is selected
        if (!isVisionSupported) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, JarvisAmber, RoundedCornerShape(10.dp)),
                color = JarvisDarkSurface
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Limitation Warning",
                        tint = JarvisAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CURRENT MODEL LACKS VISION CAPABILITIES",
                            color = JarvisAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = ModelCapabilityValidator.getLimitationExplanation(selectedModelId),
                            color = JarvisOnSurface,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Live CameraX Preview Card with HUD Controls
        CameraPreviewComposable(
            cameraManager = cameraManager,
            hasCameraPermission = hasCameraPermission,
            isCapturing = isCapturing,
            onCaptureClick = {
                isCapturing = true
                coroutineScope.launch {
                    val captureRes = cameraManager.capturePhoto()
                    isCapturing = false
                    if (captureRes.isSuccess) {
                        val bmp = captureRes.getOrThrow()
                        capturedBitmap = bmp
                        // Auto-run analysis with current prompt
                        isAnalyzing = true
                        val promptToRun = if (customQueryText.isNotBlank()) customQueryText else activePrompt
                        val result = visionEngine.analyzeImage(bmp, promptToRun, apiKey, selectedModelId)
                        analysisResult = result
                        isAnalyzing = false
                        voiceController.speak(result.responseText)
                    } else {
                        Toast.makeText(context, "Capture failed: ${captureRes.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onSwitchLensClick = {},
            onToggleTorchClick = {
                cameraManager.toggleTorch()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        // Preset Vision Commands
        Text(
            text = "TACTICAL VISION DIRECTIVES:",
            color = JarvisCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(VisionPreset.values()) { preset ->
                val isSelected = activePrompt == preset.prompt
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) JarvisCyan.copy(alpha = 0.2f) else JarvisDarkSurface)
                        .border(1.dp, if (isSelected) JarvisCyan else JarvisBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            activePrompt = preset.prompt
                            customQueryText = ""
                            // If we already have a captured bitmap, re-analyze with new prompt immediately
                            capturedBitmap?.let { bmp ->
                                coroutineScope.launch {
                                    isAnalyzing = true
                                    val res = visionEngine.analyzeImage(bmp, preset.prompt, apiKey, selectedModelId)
                                    analysisResult = res
                                    isAnalyzing = false
                                    voiceController.speak(res.responseText)
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

        // Custom Question Input for Captured Image
        OutlinedTextField(
            value = customQueryText,
            onValueChange = { customQueryText = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vision_custom_query_input"),
            placeholder = {
                Text(
                    text = "Ask custom question about image (e.g. 'Find the object on the table')...",
                    color = JarvisOnSurfaceVariant,
                    fontSize = 12.sp
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        val query = customQueryText
                        if (query.isNotBlank()) {
                            capturedBitmap?.let { bmp ->
                                coroutineScope.launch {
                                    isAnalyzing = true
                                    val res = visionEngine.analyzeImage(bmp, query, apiKey, selectedModelId)
                                    analysisResult = res
                                    isAnalyzing = false
                                    voiceController.speak(res.responseText)
                                }
                            } ?: run {
                                Toast.makeText(context, "Tap the shutter button to capture a frame first, sir.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = customQueryText.isNotBlank() && !isAnalyzing
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Submit Query", tint = JarvisCyan)
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

        // Loading Indicator during Vision Inference
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
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = JarvisCyan,
                        strokeWidth = 2.5.dp
                    )
                    Column {
                        Text(
                            text = "NEURAL OPTICAL INFERENCE IN PROGRESS",
                            color = JarvisCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Transcribing OCR tokens & computing scene embeddings...",
                            color = JarvisOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Captured Frame & Vision Analysis Output Card
        analysisResult?.let { result ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, JarvisCyan, RoundedCornerShape(14.dp))
                    .testTag("vision_result_card"),
                color = JarvisDarkSurface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Result Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Analysis Result",
                                tint = JarvisCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "VISION ANALYSIS TELEMETRY",
                                color = JarvisCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row {
                            // Speak result
                            IconButton(
                                onClick = { voiceController.speak(result.responseText) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = JarvisCyan, modifier = Modifier.size(18.dp))
                            }
                            // Copy to clipboard
                            IconButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("JARVIS Vision OCR", result.responseText))
                                    Toast.makeText(context, "Transcribed text copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = JarvisOnSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Thumbnail & Optical Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        capturedBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Captured Frame",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, JarvisBorder, RoundedCornerShape(8.dp))
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Model: ${result.modelUsed}",
                                color = JarvisGold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Frame: ${result.metrics.width}x${result.metrics.height} | Illumination: ${result.metrics.averageLuminance.toInt()}/255",
                                color = JarvisOnSurfaceVariant,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Edge Sharpness: ${result.metrics.sharpnessScore.toInt()}% | Palette: ${result.metrics.dominantColors.joinToString(" ")}",
                                color = JarvisOnSurfaceVariant,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Divider(color = JarvisBorder, thickness = 0.5.dp)

                    // Formatted Response Text
                    Text(
                        text = result.responseText,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
