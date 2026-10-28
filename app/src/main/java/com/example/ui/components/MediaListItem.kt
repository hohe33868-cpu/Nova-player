package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MediaTrack
import com.example.player.PlayerManager

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaListItem(
    track: MediaTrack,
    isPlaying: Boolean,
    isCurrentTrack: Boolean,
    isFavorite: Boolean,
    onTrackClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val itemHeight = if (isCompact) 56.dp else 72.dp
    val thumbSize = if (isCompact) 42.dp else 52.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onTrackClick,
                onLongClick = { showMenu = true }
            )
            .testTag("media_item_${track.id}"),
        color = if (isCurrentTrack) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            AlbumArtThumbnail(
                artworkUri = track.albumArtUri,
                title = track.title,
                isVideo = track.isVideo,
                size = thumbSize,
                shapeRadius = 8.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrentTrack) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Equalizer else Icons.Default.Pause,
                            contentDescription = "Currently playing",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isCurrentTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                val subtitle = buildString {
                    append(track.artist)
                    if (track.album.isNotBlank() && track.album != "Unknown Album") {
                        append(" • ")
                        append(track.album)
                    }
                    if (track.durationMs > 0) {
                        append(" • ")
                        append(track.formattedDuration)
                    }
                    if (track.sizeBytes > 0 && track.isVideo) {
                        append(" • ")
                        append(track.formattedSize)
                    }
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick favorite button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("fav_btn_${track.id}")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // More Options Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("more_btn_${track.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options for ${track.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Play Next") },
                        leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                        onClick = {
                            PlayerManager.playNext(track)
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Queue") },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            PlayerManager.addToQueue(track)
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        leadingIcon = { Icon(Icons.Default.LibraryAdd, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onAddToPlaylistClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isFavorite) "Remove Favorite" else "Add to Favorites") },
                        leadingIcon = {
                            Icon(
                                if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onToggleFavorite()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share Track") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            shareTrack(context, track)
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Track Details") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            showInfoDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showInfoDialog) {
        TrackDetailsDialog(track = track, onDismiss = { showInfoDialog = false })
    }
}

private fun shareTrack(context: Context, track: MediaTrack) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (track.isVideo) "video/*" else "audio/*"
            putExtra(Intent.EXTRA_TITLE, track.title)
            putExtra(Intent.EXTRA_TEXT, "Listening to \"${track.title}\" by ${track.artist} on NOVA Player")
            if (track.uri.startsWith("content://")) {
                putExtra(Intent.EXTRA_STREAM, Uri.parse(track.uri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share \"${track.title}\""))
    } catch (e: Exception) {
        // Fallback text share
        val textIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Listening to \"${track.title}\" by ${track.artist} on NOVA Player")
        }
        context.startActivity(Intent.createChooser(textIntent, "Share Track"))
    }
}

@Composable
fun TrackDetailsDialog(track: MediaTrack, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (track.isVideo) "Video Information" else "Song Information")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(label = "Title", value = track.title)
                DetailRow(label = "Artist", value = track.artist)
                DetailRow(label = "Album", value = track.album)
                DetailRow(label = "Duration", value = track.formattedDuration)
                if (track.sizeBytes > 0) {
                    DetailRow(label = "Size", value = track.formattedSize)
                }
                if (track.path.isNotBlank()) {
                    DetailRow(label = "Location", value = track.path)
                }
                DetailRow(label = "Format", value = track.mimeType)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
