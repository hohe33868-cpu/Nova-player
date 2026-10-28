package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.player.PlayerManager

@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit
) {
    val remainingSeconds by PlayerManager.sleepTimerSecondsRemaining.collectAsState()
    var customMinutesText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sleep Timer")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (remainingSeconds != null && remainingSeconds!! > 0) {
                    val sec = remainingSeconds!!
                    val mins = sec / 60
                    val s = sec % 60
                    val formatted = String.format("%02d:%02d", mins, s)

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Music stops in",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatted,
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { PlayerManager.extendSleepTimer(5) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+5m")
                                }
                                OutlinedButton(
                                    onClick = { PlayerManager.extendSleepTimer(15) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+15m")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    PlayerManager.cancelSleepTimer()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Turn Off Timer")
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Set a timer to automatically pause music when you fall asleep:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Presets Grid
                    val presets = listOf(15, 30, 45, 60)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { mins ->
                            FilledTonalButton(
                                onClick = {
                                    PlayerManager.startSleepTimer(mins)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sleep_preset_${mins}m"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("${mins}m")
                            }
                        }
                    }

                    // Custom input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customMinutesText,
                            onValueChange = { customMinutesText = it.filter { ch -> ch.isDigit() } },
                            placeholder = { Text("Minutes") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("sleep_custom_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val mins = customMinutesText.toIntOrNull()
                                if (mins != null && mins > 0) {
                                    PlayerManager.startSleepTimer(mins)
                                    onDismiss()
                                }
                            },
                            enabled = customMinutesText.isNotBlank() && (customMinutesText.toIntOrNull() ?: 0) > 0,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("sleep_custom_start")
                        ) {
                            Text("Start")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
