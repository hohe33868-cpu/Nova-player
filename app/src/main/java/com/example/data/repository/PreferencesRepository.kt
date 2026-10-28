package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nova_player_preferences", Context.MODE_PRIVATE)

    private val _playerPreferences = MutableStateFlow(loadPlayerPreferences())
    val playerPreferences: StateFlow<PlayerPreferences> = _playerPreferences.asStateFlow()

    private val _audioEffectSettings = MutableStateFlow(loadAudioEffectSettings())
    val audioEffectSettings: StateFlow<AudioEffectSettings> = _audioEffectSettings.asStateFlow()

    private fun loadPlayerPreferences(): PlayerPreferences {
        val themeModeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        val accentColorStr = prefs.getString(KEY_ACCENT_COLOR, AccentColor.VIOLET.name) ?: AccentColor.VIOLET.name
        val repeatModeStr = prefs.getString(KEY_DEFAULT_REPEAT, RepeatMode.ALL.name) ?: RepeatMode.ALL.name

        return PlayerPreferences(
            themeMode = runCatching { ThemeMode.valueOf(themeModeStr) }.getOrDefault(ThemeMode.DARK),
            accentColor = runCatching { AccentColor.valueOf(accentColorStr) }.getOrDefault(AccentColor.VIOLET),
            compactLayout = prefs.getBoolean(KEY_COMPACT_LAYOUT, false),
            resumeLastTrack = prefs.getBoolean(KEY_RESUME_LAST_TRACK, true),
            defaultRepeatMode = runCatching { RepeatMode.valueOf(repeatModeStr) }.getOrDefault(RepeatMode.ALL),
            shuffleEnabled = prefs.getBoolean(KEY_SHUFFLE_ENABLED, false),
            defaultSpeed = prefs.getFloat(KEY_DEFAULT_SPEED, 1.0f),
            skipIntervalSeconds = prefs.getInt(KEY_SKIP_INTERVAL, 10),
            pauseOnHeadphonesDisconnected = prefs.getBoolean(KEY_PAUSE_ON_HEADPHONES_DISCONNECTED, true)
        )
    }

    private fun loadAudioEffectSettings(): AudioEffectSettings {
        val enabled = prefs.getBoolean(KEY_EQ_ENABLED, true)
        val preset = prefs.getInt(KEY_EQ_PRESET, -1).toShort()
        val bass = prefs.getInt(KEY_EQ_BASS, 300).toShort()
        val virt = prefs.getInt(KEY_EQ_VIRT, 200).toShort()
        val bal = prefs.getFloat(KEY_EQ_BALANCE, 0.0f)

        // Parse band levels
        val bandMap = mutableMapOf<Int, Short>()
        for (i in 0..4) {
            val key = "eq_band_$i"
            if (prefs.contains(key)) {
                bandMap[i] = prefs.getInt(key, 0).toShort()
            }
        }

        return AudioEffectSettings(
            equalizerEnabled = enabled,
            presetIndex = preset,
            bandLevels = bandMap,
            bassBoostStrength = bass,
            virtualizerStrength = virt,
            stereoBalance = bal
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _playerPreferences.value = _playerPreferences.value.copy(themeMode = mode)
    }

    fun setAccentColor(accent: AccentColor) {
        prefs.edit().putString(KEY_ACCENT_COLOR, accent.name).apply()
        _playerPreferences.value = _playerPreferences.value.copy(accentColor = accent)
    }

    fun setCompactLayout(compact: Boolean) {
        prefs.edit().putBoolean(KEY_COMPACT_LAYOUT, compact).apply()
        _playerPreferences.value = _playerPreferences.value.copy(compactLayout = compact)
    }

    fun setResumeLastTrack(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RESUME_LAST_TRACK, enabled).apply()
        _playerPreferences.value = _playerPreferences.value.copy(resumeLastTrack = enabled)
    }

    fun setDefaultRepeatMode(repeatMode: RepeatMode) {
        prefs.edit().putString(KEY_DEFAULT_REPEAT, repeatMode.name).apply()
        _playerPreferences.value = _playerPreferences.value.copy(defaultRepeatMode = repeatMode)
    }

    fun setShuffleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHUFFLE_ENABLED, enabled).apply()
        _playerPreferences.value = _playerPreferences.value.copy(shuffleEnabled = enabled)
    }

    fun setDefaultSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_DEFAULT_SPEED, speed).apply()
        _playerPreferences.value = _playerPreferences.value.copy(defaultSpeed = speed)
    }

    fun setSkipInterval(seconds: Int) {
        prefs.edit().putInt(KEY_SKIP_INTERVAL, seconds).apply()
        _playerPreferences.value = _playerPreferences.value.copy(skipIntervalSeconds = seconds)
    }

    fun setPauseOnHeadphonesDisconnected(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PAUSE_ON_HEADPHONES_DISCONNECTED, enabled).apply()
        _playerPreferences.value = _playerPreferences.value.copy(pauseOnHeadphonesDisconnected = enabled)
    }

    fun updateAudioEffectSettings(settings: AudioEffectSettings) {
        val editor = prefs.edit()
            .putBoolean(KEY_EQ_ENABLED, settings.equalizerEnabled)
            .putInt(KEY_EQ_PRESET, settings.presetIndex.toInt())
            .putInt(KEY_EQ_BASS, settings.bassBoostStrength.toInt())
            .putInt(KEY_EQ_VIRT, settings.virtualizerStrength.toInt())
            .putFloat(KEY_EQ_BALANCE, settings.stereoBalance)

        settings.bandLevels.forEach { (band, level) ->
            editor.putInt("eq_band_$band", level.toInt())
        }
        editor.apply()
        _audioEffectSettings.value = settings
    }

    fun saveLastPlayedTrack(uri: String, positionMs: Long) {
        prefs.edit()
            .putString(KEY_LAST_PLAYED_URI, uri)
            .putLong(KEY_LAST_PLAYED_POS, positionMs)
            .apply()
    }

    fun getLastPlayedUri(): String? = prefs.getString(KEY_LAST_PLAYED_URI, null)
    fun getLastPlayedPosition(): Long = prefs.getLong(KEY_LAST_PLAYED_POS, 0L)

    fun resetSettings() {
        prefs.edit().clear().apply()
        _playerPreferences.value = loadPlayerPreferences()
        _audioEffectSettings.value = loadAudioEffectSettings()
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_COMPACT_LAYOUT = "compact_layout"
        private const val KEY_RESUME_LAST_TRACK = "resume_last_track"
        private const val KEY_DEFAULT_REPEAT = "default_repeat_mode"
        private const val KEY_SHUFFLE_ENABLED = "shuffle_enabled"
        private const val KEY_DEFAULT_SPEED = "default_speed"
        private const val KEY_SKIP_INTERVAL = "skip_interval"
        private const val KEY_PAUSE_ON_HEADPHONES_DISCONNECTED = "pause_on_headphones_disconnected"

        private const val KEY_EQ_ENABLED = "eq_enabled"
        private const val KEY_EQ_PRESET = "eq_preset"
        private const val KEY_EQ_BASS = "eq_bass"
        private const val KEY_EQ_VIRT = "eq_virt"
        private const val KEY_EQ_BALANCE = "eq_balance"

        private const val KEY_LAST_PLAYED_URI = "last_played_uri"
        private const val KEY_LAST_PLAYED_POS = "last_played_pos"
    }
}
