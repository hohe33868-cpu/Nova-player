package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agent.safety.PermissionManager
import com.example.ai.ModelCapabilityValidator
import com.example.ui.components.StatusTelemetryBadge
import com.example.ui.theme.*
import com.example.voice.JarvisVoiceController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisSettingsScreen(
    currentApiKey: String,
    onApiKeyChange: (String) -> Unit,
    currentModelId: String,
    onModelChange: (String) -> Unit,
    voiceController: JarvisVoiceController,
    permissionManager: PermissionManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    var voicePitch by remember { mutableStateOf(voiceController.speechPitch) }
    var voiceRate by remember { mutableStateOf(voiceController.speechRate) }
    var voiceEnabled by remember { mutableStateOf(voiceController.isVoiceOutputEnabled) }

    var reqDel by remember { mutableStateOf(permissionManager.requireApprovalForDeletions) }
    var reqNav by remember { mutableStateOf(permissionManager.requireApprovalForExternalNavigation) }
    var reqExport by remember { mutableStateOf(permissionManager.requireApprovalForDataExport) }

    val currentModel = remember(currentModelId) {
        ModelCapabilityValidator.getModel(currentModelId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisDarkBg)
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusTelemetryBadge(
                label = "Configuration",
                statusText = "SYSTEM PROTOCOLS",
                color = JarvisCyan
            )
        }

        // Section 1: Groq API Key & Neural Vision Link
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, JarvisBorder, RoundedCornerShape(12.dp)),
            color = JarvisDarkSurface
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.VpnKey, contentDescription = "API Key", tint = JarvisCyan, modifier = Modifier.size(18.dp))
                    Text("GROQ API CREDENTIALS", color = JarvisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Text(
                    text = "Power real-time multimodal optical reasoning with Groq LLaMA 3.2 Vision (11B & 90B) and high-speed text tool dispatching.",
                    color = JarvisOnSurface,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {
                        apiKeyInput = it
                        onApiKeyChange(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input_field"),
                    label = { Text("Groq API Key (gsk_...)") },
                    singleLine = true,
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Visibility",
                                tint = JarvisCyan
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                if (apiKeyInput.isBlank()) {
                    Text(
                        text = "ℹ️ Running in Local Optical Mode: Device sensors and heuristic analyzers active. Enter a Groq key for full neural cloud inference.",
                        color = JarvisAmber,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Section 2: Model Selection & Dynamic Capability Validation
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, JarvisBorder, RoundedCornerShape(12.dp)),
            color = JarvisDarkSurface
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = "Model Selection", tint = JarvisGold, modifier = Modifier.size(18.dp))
                    Text("AI MODEL & CAPABILITY VALIDATION", color = JarvisGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                // Dropdown Selector
                ExposedDropdownMenuBox(
                    expanded = modelDropdownExpanded,
                    onExpandedChange = { modelDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currentModel?.name ?: currentModelId,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("model_selector_dropdown"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = modelDropdownExpanded,
                        onDismissRequest = { modelDropdownExpanded = false },
                        modifier = Modifier.background(JarvisDarkSurface)
                    ) {
                        ModelCapabilityValidator.AVAILABLE_MODELS.forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(model.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (model.supportsVision) JarvisCyan.copy(alpha = 0.2f) else JarvisAmber.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (model.supportsVision) "VISION" else "TEXT ONLY",
                                                    color = if (model.supportsVision) JarvisCyan else JarvisAmber,
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(model.description, color = JarvisOnSurfaceVariant, fontSize = 10.sp)
                                    }
                                },
                                onClick = {
                                    onModelChange(model.id)
                                    modelDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Capability Breakdown Card
                currentModel?.let { model ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(JarvisDarkSurfaceVariant)
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Optical Perception:", color = JarvisOnSurfaceVariant, fontSize = 11.sp)
                                Text(
                                    text = if (model.supportsVision) "SUPPORTED (Camera & Screen)" else "UNSUPPORTED (Text Only)",
                                    color = if (model.supportsVision) JarvisEmerald else JarvisAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Context Window:", color = JarvisOnSurfaceVariant, fontSize = 11.sp)
                                Text("${model.maxTokens} Tokens", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            if (!model.supportsVision) {
                                Text(
                                    text = "⚠️ Warning: Selected model will automatically explain limitations when optical vision or screen frames are provided.",
                                    color = JarvisAmber,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Voice Interaction & Speech Synthesis
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, JarvisBorder, RoundedCornerShape(12.dp)),
            color = JarvisDarkSurface
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = "Voice", tint = JarvisCyan, modifier = Modifier.size(18.dp))
                    Text("VOICE SYNTHESIZER & SPEECH", color = JarvisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vocal Response Output", color = Color.White, fontSize = 13.sp)
                    Switch(
                        checked = voiceEnabled,
                        onCheckedChange = {
                            voiceEnabled = it
                            voiceController.isVoiceOutputEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan, checkedTrackColor = JarvisDarkSurfaceVariant)
                    )
                }

                // Pitch
                Text("Vocal Pitch (${String.format("%.2f", voicePitch)}x)", color = JarvisOnSurfaceVariant, fontSize = 11.sp)
                Slider(
                    value = voicePitch,
                    onValueChange = {
                        voicePitch = it
                        voiceController.speechPitch = it
                    },
                    valueRange = 0.7f..1.4f,
                    colors = SliderDefaults.colors(thumbColor = JarvisCyan, activeTrackColor = JarvisCyan)
                )

                // Rate / Speed
                Text("Vocal Cadence / Speed (${String.format("%.2f", voiceRate)}x)", color = JarvisOnSurfaceVariant, fontSize = 11.sp)
                Slider(
                    value = voiceRate,
                    onValueChange = {
                        voiceRate = it
                        voiceController.speechRate = it
                    },
                    valueRange = 0.8f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = JarvisCyan, activeTrackColor = JarvisCyan)
                )

                Button(
                    onClick = {
                        voiceController.speak("JARVIS audio synthesis online, sir. All telemetry arrays functioning within optimal tolerances.")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisDarkSurfaceVariant, contentColor = JarvisCyan),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(JarvisBorder))
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Test Audio", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test JARVIS Voice Synthesis", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 4: Safety & Human Confirmation Policies
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, JarvisBorder, RoundedCornerShape(12.dp)),
            color = JarvisDarkSurface
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Security, contentDescription = "Safety", tint = JarvisCrimson, modifier = Modifier.size(18.dp))
                    Text("SAFETY & HUMAN CONFIRMATION POLICIES", color = JarvisCrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Text(
                    text = "Enforce human authorization gates before the autonomous agent performs sensitive operations.",
                    color = JarvisOnSurface,
                    fontSize = 11.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Confirm Data & File Deletions", color = Color.White, fontSize = 12.sp)
                    Switch(
                        checked = reqDel,
                        onCheckedChange = {
                            reqDel = it
                            permissionManager.requireApprovalForDeletions = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = JarvisEmerald, checkedTrackColor = JarvisDarkSurfaceVariant)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Confirm External Web Launches", color = Color.White, fontSize = 12.sp)
                    Switch(
                        checked = reqNav,
                        onCheckedChange = {
                            reqNav = it
                            permissionManager.requireApprovalForExternalNavigation = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = JarvisEmerald, checkedTrackColor = JarvisDarkSurfaceVariant)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Confirm Archive & Data Exports", color = Color.White, fontSize = 12.sp)
                    Switch(
                        checked = reqExport,
                        onCheckedChange = {
                            reqExport = it
                            permissionManager.requireApprovalForDataExport = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = JarvisEmerald, checkedTrackColor = JarvisDarkSurfaceVariant)
                    )
                }
            }
        }
    }
}
