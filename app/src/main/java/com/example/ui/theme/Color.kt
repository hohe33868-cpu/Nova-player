package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.AccentColor

// JARVIS Sci-Fi Hologram Palette
val JarvisCyan = Color(0xFF00E5FF)
val JarvisCyanMuted = Color(0xFF00B4D8)
val JarvisCyanGlow = Color(0x3300E5FF)
val JarvisGold = Color(0xFFFFD700)
val JarvisGoldGlow = Color(0x33FFD700)
val JarvisEmerald = Color(0xFF00E676)
val JarvisCrimson = Color(0xFFFF5252)
val JarvisAmber = Color(0xFFFFB300)

val JarvisDarkBg = Color(0xFF060913)
val JarvisDarkSurface = Color(0xFF0D1524)
val JarvisDarkSurfaceVariant = Color(0xFF142138)
val JarvisBorder = Color(0xFF1C2D4A)
val JarvisOnSurface = Color(0xFFE2E8F0)
val JarvisOnSurfaceVariant = Color(0xFF8BA2C4)

// Base Dark Backgrounds
val DarkBackground = Color(0xFF060913)
val DarkSurface = Color(0xFF0D1524)
val DarkSurfaceVariant = Color(0xFF142138)
val DarkOnBackground = Color(0xFFE2E8F0)
val DarkOnSurface = Color(0xFFE2E8F0)
val DarkOnSurfaceVariant = Color(0xFF8BA2C4)

// Base AMOLED Backgrounds
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF070B12)
val AmoledSurfaceVariant = Color(0xFF0E1624)

// Base Light Backgrounds
val LightBackground = Color(0xFFF0F4F8)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE2E8F0)
val LightOnBackground = Color(0xFF0A111C)
val LightOnSurface = Color(0xFF0A111C)
val LightOnSurfaceVariant = Color(0xFF475569)

// Accent Colors
fun getAccentPrimary(accent: AccentColor): Color = JarvisCyan
fun getAccentSecondary(accent: AccentColor): Color = JarvisGold
