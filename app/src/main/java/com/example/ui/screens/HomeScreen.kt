package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.FavoriteEntity
import com.example.data.local.entities.PlaybackHistoryEntity
import com.example.data.model.MediaTrack
import com.example.player.PlayerManager
import com.example.ui.components.AlbumArtThumbnail
import com.example.ui.components.MediaListItem

@Composable
fun HomeScreen(
    audioTracks: List<MediaTrack>,
    videoTracks: List<MediaTrack>,
    playbackHistory: List<PlaybackHistoryEntity>,
    favorites: List<FavoriteEntity>,
    currentTrack: MediaTrack?,
    isPlaying: Boolean,
    isRefreshing: Boolean,
    onRefreshLibrary: () -> Unit,
    onImportUri: (Uri) -> Unit,
    onTrackClick: (MediaTrack, List<MediaTrack>) -> Unit,
    onToggleFavorite: (MediaTrack) -> Unit,
    onAddToPlaylistClick: (MediaTrack) -> Unit,
    onNavigateToMusic: () -> Unit,
    onNavigateToVideos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { onImportUri(it) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // App Header & Branding
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NOVA PLAYER",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Your Music. Your World.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row {
                        IconButton(
                            onClick = onRefreshLibrary,
                            modifier = Modifier.testTag("home_refresh_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Refresh Library",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("audio/*", "video/*"))
                            },
                            modifier = Modifier.testTag("home_import_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderOpen,
                                contentDescription = "Import Files",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        // Hero Banner Card with Quick Actions
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Ready to Play",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${audioTracks.size} audio tracks • ${videoTracks.size} videos available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (audioTracks.isNotEmpty()) {
                                        onTrackClick(audioTracks.first(), audioTracks)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("home_play_all_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play All")
                            }

                            FilledTonalButton(
                                onClick = {
                                    if (audioTracks.isNotEmpty()) {
                                        val shuffled = audioTracks.shuffled()
                                        onTrackClick(shuffled.first(), shuffled)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("home_shuffle_all_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shuffle")
                            }
                        }
                    }
                }
            }
        }

        // Recently Played Section (if any history)
        if (playbackHistory.isNotEmpty()) {
            item {
                SectionHeader(title = "Recently Played", actionText = "Clear", onActionClick = {
                    PlayerManager.mediaRepository?.let {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).run {
                            // Clear history handled via PlayerManager
                        }
                    }
                })

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(playbackHistory.take(10)) { historyItem ->
                        val matchingTrack = audioTracks.firstOrNull { it.uri == historyItem.mediaUri }
                            ?: MediaTrack(
                                id = historyItem.mediaUri.hashCode().toLong(),
                                uri = historyItem.mediaUri,
                                title = historyItem.title,
                                artist = historyItem.artist,
                                album = historyItem.album,
                                durationMs = historyItem.durationMs,
                                sizeBytes = 0L,
                                path = "",
                                dateAdded = historyItem.lastPlayedAt,
                                isVideo = historyItem.isVideo,
                                albumArtUri = historyItem.albumArtUri
                            )

                        RecentCard(
                            track = matchingTrack,
                            onClick = { onTrackClick(matchingTrack, audioTracks) }
                        )
                    }
                }
            }
        }

        // Quick Pick / Top Songs Section
        item {
            SectionHeader(
                title = "Songs & Music",
                actionText = "See All (${audioTracks.size})",
                onActionClick = onNavigateToMusic
            )
        }

        items(audioTracks.take(8)) { track ->
            val isCurrent = currentTrack?.uri == track.uri
            val isFav = favorites.any { it.mediaUri == track.uri }

            MediaListItem(
                track = track,
                isPlaying = isPlaying,
                isCurrentTrack = isCurrent,
                isFavorite = isFav,
                onTrackClick = { onTrackClick(track, audioTracks) },
                onToggleFavorite = { onToggleFavorite(track) },
                onAddToPlaylistClick = { onAddToPlaylistClick(track) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        // Videos Teaser Section (if videos exist)
        if (videoTracks.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Videos (${videoTracks.size})",
                    actionText = "View All",
                    onActionClick = onNavigateToVideos
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(videoTracks.take(6)) { video ->
                        VideoCard(
                            video = video,
                            onClick = { onTrackClick(video, videoTracks) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

@Composable
private fun RecentCard(
    track: MediaTrack,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("recent_card_${track.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            AlbumArtThumbnail(
                artworkUri = track.albumArtUri,
                title = track.title,
                isVideo = track.isVideo,
                size = 110.dp,
                shapeRadius = 12.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VideoCard(
    video: MediaTrack,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("video_card_${video.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircleFilled,
                    contentDescription = "Play video",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = video.formattedDuration,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
