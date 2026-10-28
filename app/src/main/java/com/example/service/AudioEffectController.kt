package com.example.service

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import com.example.data.model.AudioEffectSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BandInfo(
    val index: Int,
    val centerFreqHz: Int,
    val minLevelMb: Short,
    val maxLevelMb: Short,
    val currentLevelMb: Short
)

data class EqualizerCapabilities(
    val isSupported: Boolean = false,
    val isBassBoostSupported: Boolean = false,
    val isVirtualizerSupported: Boolean = false,
    val numberOfBands: Int = 0,
    val presets: List<String> = emptyList(),
    val bands: List<BandInfo> = emptyList()
)

class AudioEffectController {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var currentSessionId: Int = 0

    private val _capabilities = MutableStateFlow(EqualizerCapabilities())
    val capabilities: StateFlow<EqualizerCapabilities> = _capabilities.asStateFlow()

    fun attachAudioSession(audioSessionId: Int, initialSettings: AudioEffectSettings) {
        if (audioSessionId == 0) return
        if (currentSessionId == audioSessionId && equalizer != null) return

        release()
        currentSessionId = audioSessionId

        var eqSupported = false
        var bbSupported = false
        var virtSupported = false
        val presetNames = mutableListOf<String>()
        val bandInfoList = mutableListOf<BandInfo>()

        // 1. Equalizer
        try {
            val eq = Equalizer(0, audioSessionId)
            eq.enabled = initialSettings.equalizerEnabled
            equalizer = eq
            eqSupported = true

            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange // ShortArray [min, max] in millibels
            val minRange = range.getOrNull(0) ?: -1500
            val maxRange = range.getOrNull(1) ?: 1500

            for (i in 0 until numBands) {
                val centerFreq = eq.getCenterFreq(i.toShort()) / 1000 // mHz to Hz
                val currentLevel = initialSettings.bandLevels[i] ?: eq.getBandLevel(i.toShort())
                try {
                    eq.setBandLevel(i.toShort(), currentLevel)
                } catch (ignored: Exception) {}

                bandInfoList.add(
                    BandInfo(
                        index = i,
                        centerFreqHz = centerFreq,
                        minLevelMb = minRange,
                        maxLevelMb = maxRange,
                        currentLevelMb = currentLevel
                    )
                )
            }

            val numPresets = eq.numberOfPresets.toInt()
            for (p in 0 until numPresets) {
                try {
                    presetNames.add(eq.getPresetName(p.toShort()))
                } catch (ignored: Exception) {
                    presetNames.add("Preset $p")
                }
            }

            if (initialSettings.presetIndex in 0 until numPresets) {
                try {
                    eq.usePreset(initialSettings.presetIndex)
                } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            Log.w("AudioEffectController", "Hardware Equalizer not supported on this session", e)
        }

        // 2. Bass Boost
        try {
            val bb = BassBoost(0, audioSessionId)
            if (bb.strengthSupported) {
                bb.enabled = initialSettings.equalizerEnabled
                bb.setStrength(initialSettings.bassBoostStrength)
                bassBoost = bb
                bbSupported = true
            } else {
                bb.release()
            }
        } catch (e: Exception) {
            Log.w("AudioEffectController", "Hardware BassBoost not supported", e)
        }

        // 3. Virtualizer
        try {
            val virt = Virtualizer(0, audioSessionId)
            if (virt.strengthSupported) {
                virt.enabled = initialSettings.equalizerEnabled
                virt.setStrength(initialSettings.virtualizerStrength)
                virtualizer = virt
                virtSupported = true
            } else {
                virt.release()
            }
        } catch (e: Exception) {
            Log.w("AudioEffectController", "Hardware Virtualizer not supported", e)
        }

        _capabilities.value = EqualizerCapabilities(
            isSupported = eqSupported,
            isBassBoostSupported = bbSupported,
            isVirtualizerSupported = virtSupported,
            numberOfBands = bandInfoList.size,
            presets = presetNames,
            bands = bandInfoList
        )
    }

    fun applySettings(settings: AudioEffectSettings) {
        try {
            equalizer?.let { eq ->
                eq.enabled = settings.equalizerEnabled
                if (settings.presetIndex >= 0 && settings.presetIndex < eq.numberOfPresets) {
                    eq.usePreset(settings.presetIndex)
                } else {
                    settings.bandLevels.forEach { (band, level) ->
                        if (band < eq.numberOfBands) {
                            eq.setBandLevel(band.toShort(), level)
                        }
                    }
                }
            }

            bassBoost?.let { bb ->
                bb.enabled = settings.equalizerEnabled
                if (bb.strengthSupported) {
                    bb.setStrength(settings.bassBoostStrength)
                }
            }

            virtualizer?.let { virt ->
                virt.enabled = settings.equalizerEnabled
                if (virt.strengthSupported) {
                    virt.setStrength(settings.virtualizerStrength)
                }
            }

            // Update capability snapshot with new levels
            val current = _capabilities.value
            val updatedBands = current.bands.map { band ->
                val level = settings.bandLevels[band.index] ?: band.currentLevelMb
                band.copy(currentLevelMb = level)
            }
            _capabilities.value = current.copy(bands = updatedBands)
        } catch (e: Exception) {
            Log.e("AudioEffectController", "Error applying effect settings", e)
        }
    }

    fun setBandLevel(bandIndex: Int, levelMb: Short) {
        try {
            equalizer?.let { eq ->
                if (bandIndex < eq.numberOfBands) {
                    eq.setBandLevel(bandIndex.toShort(), levelMb)
                }
            }
            val current = _capabilities.value
            val updated = current.bands.map {
                if (it.index == bandIndex) it.copy(currentLevelMb = levelMb) else it
            }
            _capabilities.value = current.copy(bands = updated)
        } catch (e: Exception) {
            Log.e("AudioEffectController", "Failed to set band level", e)
        }
    }

    fun usePreset(presetIndex: Short) {
        try {
            equalizer?.let { eq ->
                if (presetIndex in 0 until eq.numberOfPresets) {
                    eq.usePreset(presetIndex)
                    val current = _capabilities.value
                    val updated = current.bands.map {
                        it.copy(currentLevelMb = eq.getBandLevel(it.index.toShort()))
                    }
                    _capabilities.value = current.copy(bands = updated)
                }
            }
        } catch (e: Exception) {
            Log.e("AudioEffectController", "Failed to use preset", e)
        }
    }

    fun release() {
        try { equalizer?.release() } catch (ignored: Exception) {}
        try { bassBoost?.release() } catch (ignored: Exception) {}
        try { virtualizer?.release() } catch (ignored: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        currentSessionId = 0
    }
}
