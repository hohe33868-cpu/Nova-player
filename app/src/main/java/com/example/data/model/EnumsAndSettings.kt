package com.example.data.model

enum class RepeatMode(val displayName: String) {
    OFF("Repeat Off"),
    ALL("Repeat All"),
    ONE("Repeat One");

    fun next(): RepeatMode = when (this) {
        OFF -> ALL
        ALL -> ONE
        ONE -> OFF
    }
}

enum class ThemeMode(val displayName: String) {
    DARK("Deep Charcoal (Dark)"),
    AMOLED("AMOLED Black"),
    LIGHT("Clean Light"),
    SYSTEM("System Default")
}

enum class AccentColor(val displayName: String, val primaryHex: Long, val secondaryHex: Long) {
    VIOLET("Nebula Violet", 0xFF8B5CF6, 0xFF06B6D4),
    CYAN("Electric Cyan", 0xFF06B6D4, 0xFF8B5CF6),
    GREEN("Aurora Emerald", 0xFF10B981, 0xFF06B6D4),
    PINK("Cyberpunk Rose", 0xFFEC4899, 0xFF8B5CF6),
    GOLD("Solar Amber", 0xFFF59E0B, 0xFFEF4444)
}

enum class SortOption(val displayName: String) {
    TITLE("Song Title"),
    ARTIST("Artist Name"),
    ALBUM("Album"),
    DURATION("Duration"),
    DATE_ADDED("Recently Added")
}

data class AudioEffectSettings(
    val equalizerEnabled: Boolean = true,
    val presetIndex: Short = -1, // -1 means custom
    val bandLevels: Map<Int, Short> = emptyMap(),
    val bassBoostStrength: Short = 300,
    val virtualizerStrength: Short = 200,
    val stereoBalance: Float = 0.0f // -1.0f (left) to 1.0f (right)
)

data class PlayerPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentColor: AccentColor = AccentColor.VIOLET,
    val compactLayout: Boolean = false,
    val resumeLastTrack: Boolean = true,
    val defaultRepeatMode: RepeatMode = RepeatMode.ALL,
    val shuffleEnabled: Boolean = false,
    val defaultSpeed: Float = 1.0f,
    val skipIntervalSeconds: Int = 10,
    val pauseOnHeadphonesDisconnected: Boolean = true
)
