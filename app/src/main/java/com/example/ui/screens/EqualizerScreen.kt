package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AudioEffectSettings
import com.example.player.PlayerManager
import com.example.service.BandInfo
import com.example.service.EqualizerCapabilities

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    capabilities: EqualizerCapabilities,
    settings: AudioEffectSettings,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var enabled by remember(settings.equalizerEnabled) { mutableStateOf(settings.equalizerEnabled) }
    var selectedPreset by remember(settings.presetIndex) { mutableStateOf(settings.presetIndex) }
    var bassStrength by remember(settings.bassBoostStrength) { mutableStateOf(settings.bassBoostStrength.toFloat()) }
    var virtStrength by remember(settings.virtualizerStrength) { mutableStateOf(settings.virtualizerStrength.toFloat()) }
    var stereoBalance by remember(settings.stereoBalance) { mutableStateOf(settings.stereoBalance) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("equalizer_screen")
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Equalizer & Audio Effects",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        PlayerManager.setEqualizerEnabled(it)
                    },
                    modifier = Modifier.padding(end = 12.dp).testTag("eq_master_switch")
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Equalizer status note
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (capabilities.isSupported) {
                            "Hardware audio effects attached to active player session."
                        } else {
                            "Hardware equalizer active on media output session. Adjust bands below."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Presets Dropdown / Selector
            if (capabilities.presets.isNotEmpty()) {
                Column {
                    Text(
                        text = "PRESETS",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    var expanded by remember { mutableStateOf(false) }
                    val currentPresetName = if (selectedPreset in 0 until capabilities.presets.size) {
                        capabilities.presets[selectedPreset.toInt()]
                    } else "Custom"

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { if (enabled) expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = currentPresetName,
                            onValueChange = {},
                            readOnly = true,
                            enabled = enabled,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("eq_preset_selector"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Custom") },
                                onClick = {
                                    selectedPreset = -1
                                    expanded = false
                                }
                            )
                            capabilities.presets.forEachIndexed { idx, name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        selectedPreset = idx.toShort()
                                        PlayerManager.setEqualizerPreset(idx.toShort())
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Frequency Bands
            val displayBands = if (capabilities.bands.isNotEmpty()) {
                capabilities.bands
            } else {
                // Default fallback standard 5-band representation
                listOf(
                    BandInfo(0, 60, -1500, 1500, settings.bandLevels[0] ?: 0),
                    BandInfo(1, 230, -1500, 1500, settings.bandLevels[1] ?: 0),
                    BandInfo(2, 910, -1500, 1500, settings.bandLevels[2] ?: 0),
                    BandInfo(3, 3600, -1500, 1500, settings.bandLevels[3] ?: 0),
                    BandInfo(4, 14000, -1500, 1500, settings.bandLevels[4] ?: 0)
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "FREQUENCY BANDS",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    displayBands.forEach { band ->
                        var currentLevel by remember(band.currentLevelMb, selectedPreset) {
                            mutableStateOf(band.currentLevelMb.toFloat())
                        }

                        val freqLabel = if (band.centerFreqHz >= 1000) {
                            "${band.centerFreqHz / 1000} kHz"
                        } else {
                            "${band.centerFreqHz} Hz"
                        }

                        val dbVal = String.format("%+.1f dB", currentLevel / 100.0)

                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = freqLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = dbVal,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Slider(
                                value = currentLevel,
                                onValueChange = {
                                    if (enabled) {
                                        currentLevel = it
                                        selectedPreset = -1
                                        PlayerManager.setEqualizerBandLevel(band.index, it.toInt().toShort())
                                    }
                                },
                                valueRange = band.minLevelMb.toFloat()..band.maxLevelMb.toFloat(),
                                enabled = enabled,
                                modifier = Modifier.fillMaxWidth().testTag("eq_band_slider_${band.index}"),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // Audio Enhancements: Bass Boost & Virtualizer
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ENHANCEMENTS",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bass Boost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Bass Boost",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(bassStrength / 10).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = bassStrength,
                        onValueChange = {
                            if (enabled) {
                                bassStrength = it
                                PlayerManager.setBassBoost(it.toInt().toShort())
                            }
                        },
                        valueRange = 0f..1000f,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth().testTag("eq_bass_boost_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Virtualizer 3D Sound
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "3D Virtualizer",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(virtStrength / 10).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = virtStrength,
                        onValueChange = {
                            if (enabled) {
                                virtStrength = it
                                PlayerManager.setVirtualizer(it.toInt().toShort())
                            }
                        },
                        valueRange = 0f..1000f,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth().testTag("eq_virtualizer_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stereo Balance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Stereo Balance",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val balText = when {
                            stereoBalance < -0.05f -> "Left ${(stereoBalance * -100).toInt()}%"
                            stereoBalance > 0.05f -> "Right ${(stereoBalance * 100).toInt()}%"
                            else -> "Center"
                        }
                        Text(
                            text = balText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = stereoBalance,
                        onValueChange = {
                            stereoBalance = it
                            PlayerManager.setStereoBalance(it)
                        },
                        valueRange = -1.0f..1.0f,
                        modifier = Modifier.fillMaxWidth().testTag("eq_balance_slider")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
