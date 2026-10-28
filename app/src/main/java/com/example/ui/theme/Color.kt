package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.AccentColor

// Base Dark Backgrounds
val DarkBackground = Color(0xFF0D0F18)
val DarkSurface = Color(0xFF151824)
val DarkSurfaceVariant = Color(0xFF1F2336)
val DarkOnBackground = Color(0xFFE2E8F0)
val DarkOnSurface = Color(0xFFE2E8F0)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)

// Base AMOLED Backgrounds
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF0A0C10)
val AmoledSurfaceVariant = Color(0xFF141720)

// Base Light Backgrounds
val LightBackground = Color(0xFFF6F8FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE8EDF5)
val LightOnBackground = Color(0xFF0F172A)
val LightOnSurface = Color(0xFF0F172A)
val LightOnSurfaceVariant = Color(0xFF64748B)

// Accent Colors
fun getAccentPrimary(accent: AccentColor): Color = Color(accent.primaryHex)
fun getAccentSecondary(accent: AccentColor): Color = Color(accent.secondaryHex)
