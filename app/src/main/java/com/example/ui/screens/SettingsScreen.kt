package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onNavigateToEqualizer: () -> Unit,
    onNavigateToPlayback: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToSleepTimer: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                SettingsNavigationItem(
                    title = "Equalizer & Audio Effects",
                    subtitle = "Presets, 5-band EQ, bass boost, virtualizer, balance",
                    icon = Icons.Outlined.GraphicEq,
                    onClick = onNavigateToEqualizer,
                    testTag = "settings_nav_eq"
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))

                SettingsNavigationItem(
                    title = "Playback & Library",
                    subtitle = "Resume tracks, repeat modes, skip intervals, cache",
                    icon = Icons.Outlined.PlayCircleOutline,
                    onClick = onNavigateToPlayback,
                    testTag = "settings_nav_playback"
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))

                SettingsNavigationItem(
                    title = "Appearance & Themes",
                    subtitle = "Dark, AMOLED, Light themes, accent colors, layout density",
                    icon = Icons.Outlined.Palette,
                    onClick = onNavigateToAppearance,
                    testTag = "settings_nav_appearance"
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))

                SettingsNavigationItem(
                    title = "Sleep Timer",
                    subtitle = "Automatically pause music after duration",
                    icon = Icons.Outlined.Bedtime,
                    onClick = onNavigateToSleepTimer,
                    testTag = "settings_nav_sleep"
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))

                SettingsNavigationItem(
                    title = "About & Permissions",
                    subtitle = "NOVA Player v1.0.0, storage access info, zero-tracking privacy",
                    icon = Icons.Outlined.Info,
                    onClick = onNavigateToAbout,
                    testTag = "settings_nav_about"
                )
            }
        }
    }
}

@Composable
private fun SettingsNavigationItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
