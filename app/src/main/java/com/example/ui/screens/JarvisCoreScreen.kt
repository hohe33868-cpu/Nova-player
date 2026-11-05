package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agent.AutonomousAgent
import com.example.agent.ChatMessage
import com.example.ui.components.ArcReactorCore
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.StatusTelemetryBadge
import com.example.ui.theme.*
import com.example.voice.JarvisVoiceController
import kotlinx.coroutines.launch

@Composable
fun JarvisCoreScreen(
    agent: AutonomousAgent,
    voiceController: JarvisVoiceController,
    apiKey: String,
    onNavigateToVision: () -> Unit,
    onNavigateToScreenIntel: () -> Unit,
    onNavigateToActivityCenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val chatHistory by agent.conversationHistory.collectAsState()
    val isProcessing by agent.isProcessing.collectAsState()
    val isSpeaking by voiceController.isSpeaking.collectAsState()
    val isListening by voiceController.isListening.collectAsState()
    val audioAmp by voiceController.audioAmplitude.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll when new messages arrive
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisDarkBg)
    ) {
        // Top Holographic Arc Reactor & Telemetry Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = JarvisDarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusTelemetryBadge(
                        label = "Core Status",
                        statusText = if (isProcessing) "COMPUTING" else "ONLINE",
                        color = if (isProcessing) JarvisGold else JarvisEmerald
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusTelemetryBadge(
                            label = "Neural Link",
                            statusText = if (apiKey.isNotBlank()) "GROQ VISION" else "LOCAL ONLY",
                            color = if (apiKey.isNotBlank()) JarvisCyan else JarvisAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Arc Reactor Core Animation
                ArcReactorCore(
                    size = 130.dp,
                    isActive = isProcessing || isSpeaking,
                    energyOutput = if (isProcessing) "RUNNING" else "100% NOMINAL"
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Audio Waveform Visualizer
                if (isSpeaking || isListening) {
                    AudioWaveformVisualizer(
                        amplitude = audioAmp,
                        color = if (isListening) JarvisGold else JarvisCyan,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Text(
                        text = if (isListening) "LISTENING TO VOICE INPUT..." else "SYNTHESIZING SPEECH...",
                        color = if (isListening) JarvisGold else JarvisCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Quick Navigation Shortcuts to Vision & Screen Intelligence
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onNavigateToVision,
                modifier = Modifier
                    .weight(1f)
                    .testTag("shortcut_vision_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisDarkSurfaceVariant,
                    contentColor = JarvisCyan
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(JarvisBorder)
                )
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Camera Vision", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Vision HUD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onNavigateToScreenIntel,
                modifier = Modifier
                    .weight(1f)
                    .testTag("shortcut_screen_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisDarkSurfaceVariant,
                    contentColor = JarvisGold
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(JarvisBorder)
                )
            ) {
                Icon(Icons.Default.Screenshot, contentDescription = "Screen Intel", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Screen Intel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onNavigateToActivityCenter,
                modifier = Modifier
                    .weight(1f)
                    .testTag("shortcut_agent_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisDarkSurfaceVariant,
                    contentColor = JarvisEmerald
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(JarvisBorder)
                )
            ) {
                Icon(Icons.Default.Psychology, contentDescription = "Agent Center", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Agent HUD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Chat Message History
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatHistory, key = { it.id }) { msg ->
                ChatBubble(
                    message = msg,
                    onSpeakClick = { text ->
                        if (isSpeaking) voiceController.stopSpeaking() else voiceController.speak(text)
                    }
                )
            }

            if (isProcessing) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = JarvisCyan,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "JARVIS is formulating plan & running tool queries...",
                            color = JarvisCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Proactive Suggestion Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(agent.getProactiveSuggestions()) { suggestion ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(JarvisDarkSurface)
                        .border(1.dp, JarvisBorder, RoundedCornerShape(16.dp))
                        .clickable {
                            inputText = suggestion
                            coroutineScope.launch {
                                val reply = agent.processUserDirective(suggestion, apiKey)
                                voiceController.speak(reply)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = suggestion,
                        color = JarvisCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Bottom Input Bar with Mic and Send
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = JarvisDarkSurface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Microphone Voice Input
                IconButton(
                    onClick = {
                        if (isListening) {
                            voiceController.stopListening()
                        } else {
                            voiceController.startListening { spoken ->
                                inputText = spoken
                                coroutineScope.launch {
                                    val reply = agent.processUserDirective(spoken, apiKey)
                                    voiceController.speak(reply)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isListening) JarvisGold else JarvisDarkSurfaceVariant)
                        .border(1.dp, if (isListening) JarvisGold else JarvisBorder, CircleShape)
                        .testTag("voice_input_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                        contentDescription = "Voice Input",
                        tint = if (isListening) Color.Black else JarvisCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Text Input
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("command_input_field"),
                    placeholder = {
                        Text(
                            text = "State your directive, sir...",
                            color = JarvisOnSurfaceVariant,
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = JarvisCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                val query = inputText
                                inputText = ""
                                coroutineScope.launch {
                                    val reply = agent.processUserDirective(query, apiKey)
                                    voiceController.speak(reply)
                                }
                            }
                        }
                    )
                )

                // Send Button
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val query = inputText
                            inputText = ""
                            coroutineScope.launch {
                                val reply = agent.processUserDirective(query, apiKey)
                                voiceController.speak(reply)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(JarvisCyan)
                        .testTag("send_command_button"),
                    enabled = inputText.isNotBlank() && !isProcessing
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Directive",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onSpeakClick: (String) -> Unit
) {
    val isUser = message.sender == "USER"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.padding(bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (isUser) "OPERATOR" else "JARVIS AI CORE",
                color = if (isUser) JarvisGold else JarvisCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (message.isAgentPlanResult) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(JarvisEmerald.copy(alpha = 0.2f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "PLAN EXECUTED",
                        color = JarvisEmerald,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    1.dp,
                    if (isUser) JarvisGold.copy(alpha = 0.4f) else JarvisBorder,
                    RoundedCornerShape(12.dp)
                ),
            color = if (isUser) JarvisDarkSurfaceVariant else JarvisDarkSurface
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                if (!isUser) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { onSpeakClick(message.text) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Read Aloud",
                                tint = JarvisCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
