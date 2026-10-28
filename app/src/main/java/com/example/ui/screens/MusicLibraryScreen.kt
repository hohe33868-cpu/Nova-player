package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.FavoriteEntity
import com.example.data.model.MediaTrack
import com.example.data.model.SortOption
import com.example.ui.components.AlbumArtThumbnail
import com.example.ui.components.MediaListItem
import java.io.File

enum class LibraryTab(val title: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FOLDERS("Folders")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicLibraryScreen(
    tracks: List<MediaTrack>,
    favorites: List<FavoriteEntity>,
    currentTrack: MediaTrack?,
    isPlaying: Boolean,
    isCompact: Boolean,
    onTrackClick: (MediaTrack, List<MediaTrack>) -> Unit,
    onToggleFavorite: (MediaTrack) -> Unit,
    onAddToPlaylistClick: (MediaTrack) -> Unit,
    onImportUri: (Uri) -> Unit,
    onRefreshLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(LibraryTab.SONGS) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SortOption.TITLE) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Drill-down states
    var selectedAlbum by remember { mutableStateOf<String?>(null) }
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { onImportUri(it) }
    }

    // Handle back button when drilled down
    if (selectedAlbum != null) {
        BackHandler { selectedAlbum = null }
    } else if (selectedArtist != null) {
        BackHandler { selectedArtist = null }
    } else if (selectedFolder != null) {
        BackHandler { selectedFolder = null }
    }

    // Filter tracks by search query
    val filteredTracks = remember(tracks, searchQuery) {
        if (searchQuery.isBlank()) tracks
        else {
            val q = searchQuery.trim().lowercase()
            tracks.filter {
                it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
            }
        }
    }

    // Sort tracks
    val sortedTracks = remember(filteredTracks, sortOption) {
        when (sortOption) {
            SortOption.TITLE -> filteredTracks.sortedBy { it.title.lowercase() }
            SortOption.ARTIST -> filteredTracks.sortedBy { it.artist.lowercase() }
            SortOption.ALBUM -> filteredTracks.sortedBy { it.album.lowercase() }
            SortOption.DURATION -> filteredTracks.sortedByDescending { it.durationMs }
            SortOption.DATE_ADDED -> filteredTracks.sortedByDescending { it.dateAdded }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("music_library_screen")
    ) {
        // Search & Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("library_search_input"),
                placeholder = { Text("Search songs, artists, albums...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Sort Dropdown button
            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.testTag("library_sort_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort library",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            leadingIcon = {
                                if (sortOption == option) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            onClick = {
                                sortOption = option
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Tabs
        PrimaryTabRow(
            selectedTabIndex = selectedTab.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            LibraryTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = {
                        selectedTab = tab
                        selectedAlbum = null
                        selectedArtist = null
                        selectedFolder = null
                    },
                    text = {
                        Text(
                            text = tab.title,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Drilled-down views
        when {
            selectedAlbum != null -> {
                val albumTracks = tracks.filter { it.album == selectedAlbum }
                DrillDownTrackList(
                    title = selectedAlbum!!,
                    subtitle = "${albumTracks.size} tracks",
                    tracks = albumTracks,
                    favorites = favorites,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    isCompact = isCompact,
                    onBackClick = { selectedAlbum = null },
                    onTrackClick = onTrackClick,
                    onToggleFavorite = onToggleFavorite,
                    onAddToPlaylistClick = onAddToPlaylistClick
                )
            }
            selectedArtist != null -> {
                val artistTracks = tracks.filter { it.artist == selectedArtist }
                DrillDownTrackList(
                    title = selectedArtist!!,
                    subtitle = "${artistTracks.size} tracks",
                    tracks = artistTracks,
                    favorites = favorites,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    isCompact = isCompact,
                    onBackClick = { selectedArtist = null },
                    onTrackClick = onTrackClick,
                    onToggleFavorite = onToggleFavorite,
                    onAddToPlaylistClick = onAddToPlaylistClick
                )
            }
            selectedFolder != null -> {
                val folderTracks = tracks.filter {
                    val parent = File(it.path).parentFile?.name ?: "Root"
                    parent == selectedFolder
                }
                DrillDownTrackList(
                    title = selectedFolder!!,
                    subtitle = "${folderTracks.size} tracks",
                    tracks = folderTracks,
                    favorites = favorites,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    isCompact = isCompact,
                    onBackClick = { selectedFolder = null },
                    onTrackClick = onTrackClick,
                    onToggleFavorite = onToggleFavorite,
                    onAddToPlaylistClick = onAddToPlaylistClick
                )
            }
            else -> {
                // Tab content
                when (selectedTab) {
                    LibraryTab.SONGS -> {
                        if (sortedTracks.isEmpty()) {
                            EmptyLibraryState(
                                onImportClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
                                onRefreshClick = onRefreshLibrary
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 96.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${sortedTracks.size} tracks",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Sorted by ${sortOption.displayName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                items(sortedTracks, key = { it.uri }) { track ->
                                    val isCurrent = currentTrack?.uri == track.uri
                                    val isFav = favorites.any { it.mediaUri == track.uri }

                                    MediaListItem(
                                        track = track,
                                        isPlaying = isPlaying,
                                        isCurrentTrack = isCurrent,
                                        isFavorite = isFav,
                                        isCompact = isCompact,
                                        onTrackClick = { onTrackClick(track, sortedTracks) },
                                        onToggleFavorite = { onToggleFavorite(track) },
                                        onAddToPlaylistClick = { onAddToPlaylistClick(track) },
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    LibraryTab.ALBUMS -> {
                        val albumGroups = remember(filteredTracks) {
                            filteredTracks.groupBy { it.album }
                                .filterKeys { it.isNotBlank() }
                        }

                        if (albumGroups.isEmpty()) {
                            EmptyLibraryState(
                                message = "No albums found",
                                onImportClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
                                onRefreshClick = onRefreshLibrary
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 140.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(albumGroups.keys.toList()) { albumName ->
                                    val albumTracks = albumGroups[albumName] ?: emptyList()
                                    val representative = albumTracks.firstOrNull()

                                    Card(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { selectedAlbum = albumName }
                                            .testTag("album_card_$albumName"),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            AlbumArtThumbnail(
                                                artworkUri = representative?.albumArtUri,
                                                title = albumName,
                                                size = 130.dp,
                                                shapeRadius = 12.dp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = albumName,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${albumTracks.size} songs",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    LibraryTab.ARTISTS -> {
                        val artistGroups = remember(filteredTracks) {
                            filteredTracks.groupBy { it.artist }
                                .filterKeys { it.isNotBlank() }
                        }

                        if (artistGroups.isEmpty()) {
                            EmptyLibraryState(
                                message = "No artists found",
                                onImportClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
                                onRefreshClick = onRefreshLibrary
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(artistGroups.keys.toList()) { artistName ->
                                    val artistTracks = artistGroups[artistName] ?: emptyList()

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { selectedArtist = artistName }
                                            .testTag("artist_card_$artistName"),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                modifier = Modifier.size(44.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(16.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = artistName,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${artistTracks.size} tracks",
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
                                }
                            }
                        }
                    }

                    LibraryTab.FOLDERS -> {
                        val folderGroups = remember(filteredTracks) {
                            filteredTracks.groupBy {
                                val file = File(it.path)
                                file.parentFile?.name ?: "Storage"
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(folderGroups.keys.toList()) { folderName ->
                                val folderTracks = folderGroups[folderName] ?: emptyList()

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { selectedFolder = folderName }
                                        .testTag("folder_card_$folderName"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(44.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Folder,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = folderName,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${folderTracks.size} files",
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
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrillDownTrackList(
    title: String,
    subtitle: String,
    tracks: List<MediaTrack>,
    favorites: List<FavoriteEntity>,
    currentTrack: MediaTrack?,
    isPlaying: Boolean,
    isCompact: Boolean,
    onBackClick: () -> Unit,
    onTrackClick: (MediaTrack, List<MediaTrack>) -> Unit,
    onToggleFavorite: (MediaTrack) -> Unit,
    onAddToPlaylistClick: (MediaTrack) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = {
                    if (tracks.isNotEmpty()) {
                        val shuffled = tracks.shuffled()
                        onTrackClick(shuffled.first(), shuffled)
                    }
                }
            ) {
                Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Shuffle")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(tracks, key = { it.uri }) { track ->
                val isCurrent = currentTrack?.uri == track.uri
                val isFav = favorites.any { it.mediaUri == track.uri }

                MediaListItem(
                    track = track,
                    isPlaying = isPlaying,
                    isCurrentTrack = isCurrent,
                    isFavorite = isFav,
                    isCompact = isCompact,
                    onTrackClick = { onTrackClick(track, tracks) },
                    onToggleFavorite = { onToggleFavorite(track) },
                    onAddToPlaylistClick = { onAddToPlaylistClick(track) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyLibraryState(
    message: String = "No music tracks found on device",
    onImportClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MusicOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Use the file picker to open audio files, or rescan the media library.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onImportClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick Files")
                }

                OutlinedButton(
                    onClick = onRefreshClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rescan")
                }
            }
        }
    }
}
