package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.FavoriteEntity
import com.example.data.local.entities.PlaylistEntity
import com.example.data.local.entities.PlaylistTrackEntity
import com.example.data.model.MediaTrack
import com.example.player.PlayerManager
import com.example.ui.components.AlbumArtThumbnail
import com.example.ui.components.MediaListItem
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistEntity>,
    favorites: List<FavoriteEntity>,
    allTracks: List<MediaTrack>,
    currentTrack: MediaTrack?,
    isPlaying: Boolean,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onTrackClick: (MediaTrack, List<MediaTrack>) -> Unit,
    onToggleFavorite: (MediaTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<PlaylistEntity?>(null) }
    var playlistToDelete by remember { mutableStateOf<PlaylistEntity?>(null) }

    var playlistTracks by remember { mutableStateOf<List<PlaylistTrackEntity>>(emptyList()) }

    // If viewing Favorites playlist (id = -1)
    var isViewingFavorites by remember { mutableStateOf(false) }

    if (selectedPlaylist != null || isViewingFavorites) {
        BackHandler {
            selectedPlaylist = null
            isViewingFavorites = false
        }
    }

    LaunchedEffect(selectedPlaylist) {
        val pl = selectedPlaylist
        if (pl != null) {
            PlayerManager.mediaRepository?.getPlaylistTracks(pl.id)?.collect {
                playlistTracks = it
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("playlists_screen")
    ) {
        if (selectedPlaylist != null) {
            // Viewing specific playlist
            val currentTracks = playlistTracks.map { pt ->
                allTracks.firstOrNull { it.uri == pt.mediaUri } ?: MediaTrack(
                    id = pt.id,
                    uri = pt.mediaUri,
                    title = pt.title,
                    artist = pt.artist,
                    album = pt.album,
                    durationMs = pt.durationMs,
                    sizeBytes = 0L,
                    path = "",
                    dateAdded = pt.addedAt,
                    isVideo = pt.isVideo,
                    albumArtUri = pt.albumArtUri
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedPlaylist = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedPlaylist!!.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${currentTracks.size} tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (currentTracks.isNotEmpty()) {
                        IconButton(onClick = {
                            onTrackClick(currentTracks.first(), currentTracks)
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play all", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (currentTracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No tracks in this playlist yet.\nAdd tracks using the track menu.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(currentTracks, key = { it.uri }) { track ->
                            val isCur = currentTrack?.uri == track.uri
                            val isFav = favorites.any { it.mediaUri == track.uri }

                            MediaListItem(
                                track = track,
                                isPlaying = isPlaying,
                                isCurrentTrack = isCur,
                                isFavorite = isFav,
                                onTrackClick = { onTrackClick(track, currentTracks) },
                                onToggleFavorite = { onToggleFavorite(track) },
                                onAddToPlaylistClick = {},
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        } else if (isViewingFavorites) {
            // Viewing Favorites
            val favoriteTracks = favorites.map { fav ->
                allTracks.firstOrNull { it.uri == fav.mediaUri } ?: MediaTrack(
                    id = fav.mediaUri.hashCode().toLong(),
                    uri = fav.mediaUri,
                    title = fav.title,
                    artist = fav.artist,
                    album = fav.album,
                    durationMs = fav.durationMs,
                    sizeBytes = 0L,
                    path = "",
                    dateAdded = fav.addedAt,
                    isVideo = fav.isVideo,
                    albumArtUri = fav.albumArtUri
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isViewingFavorites = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Favorites",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${favoriteTracks.size} favorite tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (favoriteTracks.isNotEmpty()) {
                        IconButton(onClick = {
                            onTrackClick(favoriteTracks.first(), favoriteTracks)
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play all", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (favoriteTracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No favorite tracks yet.\nTap the heart icon on any song to save it here.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(favoriteTracks, key = { it.uri }) { track ->
                            val isCur = currentTrack?.uri == track.uri

                            MediaListItem(
                                track = track,
                                isPlaying = isPlaying,
                                isCurrentTrack = isCur,
                                isFavorite = true,
                                onTrackClick = { onTrackClick(track, favoriteTracks) },
                                onToggleFavorite = { onToggleFavorite(track) },
                                onAddToPlaylistClick = {},
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Playlist Hub View
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playlists",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("create_playlist_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Playlist")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Built-in Favorites card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { isViewingFavorites = true }
                            .testTag("favorites_playlist_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Favorites",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${favorites.size} saved tracks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // User Playlists
                items(playlists, key = { it.id }) { playlist ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedPlaylist = playlist }
                            .testTag("playlist_card_${playlist.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Custom Playlist",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Edit / Rename
                            IconButton(onClick = { playlistToRename = playlist }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Rename", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // Delete
                            IconButton(onClick = { playlistToDelete = playlist }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Playlist Dialog
    if (showCreateDialog) {
        var playlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Playlist") },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    placeholder = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("playlist_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            onCreatePlaylist(playlistName.trim())
                            showCreateDialog = false
                        }
                    },
                    modifier = Modifier.testTag("create_playlist_confirm")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Rename Dialog
    playlistToRename?.let { pl ->
        var newName by remember { mutableStateOf(pl.name) }
        AlertDialog(
            onDismissRequest = { playlistToRename = null },
            title = { Text("Rename Playlist") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onRenamePlaylist(pl.id, newName.trim())
                            playlistToRename = null
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { playlistToRename = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Confirmation Dialog
    playlistToDelete?.let { pl ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = { Text("Delete Playlist") },
            text = { Text("Are you sure you want to delete \"${pl.name}\"? Tracks will not be deleted from storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePlaylist(pl)
                        playlistToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) { Text("Cancel") }
            }
        )
    }
}
