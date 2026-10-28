package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.model.AccentColor
import com.example.data.model.ThemeMode

@Composable
fun NovaPlayerTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentColor: AccentColor = AccentColor.VIOLET,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.AMOLED -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemDark
    }

    val primary = getAccentPrimary(accentColor)
    val secondary = getAccentSecondary(accentColor)

    val colorScheme = when {
        themeMode == ThemeMode.AMOLED -> {
            darkColorScheme(
                primary = primary,
                onPrimary = Color.White,
                secondary = secondary,
                onSecondary = Color.White,
                background = AmoledBackground,
                onBackground = DarkOnBackground,
                surface = AmoledSurface,
                onSurface = DarkOnSurface,
                surfaceVariant = AmoledSurfaceVariant,
                onSurfaceVariant = DarkOnSurfaceVariant
            )
        }
        isDark -> {
            darkColorScheme(
                primary = primary,
                onPrimary = Color.White,
                secondary = secondary,
                onSecondary = Color.White,
                background = DarkBackground,
                onBackground = DarkOnBackground,
                surface = DarkSurface,
                onSurface = DarkOnSurface,
                surfaceVariant = DarkSurfaceVariant,
                onSurfaceVariant = DarkOnSurfaceVariant
            )
        }
        else -> {
            lightColorScheme(
                primary = primary,
                onPrimary = Color.White,
                secondary = secondary,
                onSecondary = Color.White,
                background = LightBackground,
                onBackground = LightOnBackground,
                surface = LightSurface,
                onSurface = LightOnSurface,
                surfaceVariant = LightSurfaceVariant,
                onSurfaceVariant = LightOnSurfaceVariant
            )
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
