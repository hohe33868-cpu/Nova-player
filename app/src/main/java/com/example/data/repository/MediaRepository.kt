package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.local.NovaDatabase
import com.example.data.local.entities.FavoriteEntity
import com.example.data.local.entities.PlaybackHistoryEntity
import com.example.data.local.entities.PlaylistEntity
import com.example.data.local.entities.PlaylistTrackEntity
import com.example.data.model.MediaTrack
import com.example.data.model.SortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepository(
    private val context: Context,
    private val database: NovaDatabase
) {
    private val _allAudioTracks = MutableStateFlow<List<MediaTrack>>(emptyList())
    val allAudioTracks: StateFlow<List<MediaTrack>> = _allAudioTracks.asStateFlow()

    private val _allVideoTracks = MutableStateFlow<List<MediaTrack>>(emptyList())
    val allVideoTracks: StateFlow<List<MediaTrack>> = _allVideoTracks.asStateFlow()

    private val _importedTracks = MutableStateFlow<List<MediaTrack>>(emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Favorites Flow from Room
    val favorites: Flow<List<FavoriteEntity>> = database.favoriteDao().getAllFavorites()

    // History Flow from Room
    val playbackHistory: Flow<List<PlaybackHistoryEntity>> = database.historyDao().getRecentHistory()

    // Playlists Flow from Room
    val playlists: Flow<List<PlaylistEntity>> = database.playlistDao().getAllPlaylists()

    suspend fun refreshLibrary() = withContext(Dispatchers.IO) {
        _isLoading.value = true
        try {
            val audioList = mutableListOf<MediaTrack>()
            val videoList = mutableListOf<MediaTrack>()

            // 1. Query MediaStore Audio
            queryMediaStoreAudio(audioList)

            // 2. Query MediaStore Video
            queryMediaStoreVideo(videoList)

            // 3. Add bundled demo audio tracks if empty or as guaranteed starter tracks
            val demoTracks = loadBundledDemoTracks()
            val combinedAudio = (demoTracks + _importedTracks.value + audioList).distinctBy { it.uri }
            val combinedVideo = (videoList + _importedTracks.value.filter { it.isVideo }).distinctBy { it.uri }

            _allAudioTracks.value = combinedAudio
            _allVideoTracks.value = combinedVideo
        } catch (e: Exception) {
            Log.e("MediaRepository", "Error refreshing library", e)
            if (_allAudioTracks.value.isEmpty()) {
                _allAudioTracks.value = loadBundledDemoTracks()
            }
        } finally {
            _isLoading.value = false
        }
    }

    private fun queryMediaStoreAudio(dest: MutableList<MediaTrack>) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Unknown Track"
                val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                val album = cursor.getString(albumCol) ?: "Unknown Album"
                val duration = cursor.getLong(durationCol)
                val size = cursor.getLong(sizeCol)
                val path = cursor.getString(dataCol) ?: ""
                val dateAdded = cursor.getLong(dateCol)
                val albumId = cursor.getLong(albumIdCol)

                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val artworkUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                dest.add(
                    MediaTrack(
                        id = id,
                        uri = contentUri.toString(),
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        sizeBytes = size,
                        path = path,
                        dateAdded = dateAdded,
                        isVideo = false,
                        albumArtUri = artworkUri
                    )
                )
            }
        }
    }

    private fun queryMediaStoreVideo(dest: MutableList<MediaTrack>) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.ARTIST,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Video.Media.TITLE} ASC"

        context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Unknown Video"
                val artist = cursor.getString(artistCol) ?: "Video"
                val duration = cursor.getLong(durationCol)
                val size = cursor.getLong(sizeCol)
                val path = cursor.getString(dataCol) ?: ""
                val dateAdded = cursor.getLong(dateCol)

                val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                dest.add(
                    MediaTrack(
                        id = id,
                        uri = contentUri.toString(),
                        title = title,
                        artist = artist,
                        album = "Videos",
                        durationMs = duration,
                        sizeBytes = size,
                        path = path,
                        dateAdded = dateAdded,
                        isVideo = true,
                        albumArtUri = null
                    )
                )
            }
        }
    }

    private fun loadBundledDemoTracks(): List<MediaTrack> {
        return listOf(
            MediaTrack(
                id = 1000001L,
                uri = "asset:///demo_audio/nova_starlight.wav",
                title = "Nova Starlight",
                artist = "NOVA Sound Lab",
                album = "Cosmic Horizons",
                durationMs = 40_000L,
                sizeBytes = 7_056_044L,
                path = "assets/demo_audio/nova_starlight.wav",
                dateAdded = System.currentTimeMillis() / 1000,
                isVideo = false
            ),
            MediaTrack(
                id = 1000002L,
                uri = "asset:///demo_audio/cyber_aurora.wav",
                title = "Cyber Aurora",
                artist = "NOVA Sound Lab",
                album = "Neon Drift",
                durationMs = 36_000L,
                sizeBytes = 6_350_444L,
                path = "assets/demo_audio/cyber_aurora.wav",
                dateAdded = System.currentTimeMillis() / 1000,
                isVideo = false
            ),
            MediaTrack(
                id = 1000003L,
                uri = "asset:///demo_audio/midnight_drift.wav",
                title = "Midnight Drift",
                artist = "NOVA Sound Lab",
                album = "Lo-Fi Odyssey",
                durationMs = 42_000L,
                sizeBytes = 7_408_844L,
                path = "assets/demo_audio/midnight_drift.wav",
                dateAdded = System.currentTimeMillis() / 1000,
                isVideo = false
            )
        )
    }

    suspend fun importFromUri(uri: Uri): MediaTrack? = withContext(Dispatchers.IO) {
        try {
            var displayName = "Imported File"
            var size = 0L
            val isVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx != -1) displayName = cursor.getString(nameIdx) ?: displayName
                    if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                }
            }

            var duration = 0L
            var artist = if (isVideo) "Video" else "Unknown Artist"
            var album = if (isVideo) "Imported Videos" else "Imported Tracks"

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                    duration = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                    if (it.isNotBlank()) artist = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                    if (it.isNotBlank()) album = it
                }
            } catch (ignored: Exception) {
            } finally {
                retriever.release()
            }

            val newTrack = MediaTrack(
                id = System.currentTimeMillis(),
                uri = uri.toString(),
                title = displayName.substringBeforeLast("."),
                artist = artist,
                album = album,
                durationMs = duration,
                sizeBytes = size,
                path = uri.toString(),
                dateAdded = System.currentTimeMillis() / 1000,
                isVideo = isVideo
            )

            _importedTracks.value = _importedTracks.value + newTrack
            if (isVideo) {
                _allVideoTracks.value = (_allVideoTracks.value + newTrack).distinctBy { it.uri }
            } else {
                _allAudioTracks.value = (_allAudioTracks.value + newTrack).distinctBy { it.uri }
            }
            newTrack
        } catch (e: Exception) {
            Log.e("MediaRepository", "Failed to import uri: $uri", e)
            null
        }
    }

    // Favorites
    suspend fun toggleFavorite(track: MediaTrack) = withContext(Dispatchers.IO) {
        val isFav = database.favoriteDao().isFavorite(track.uri).first()
        if (isFav) {
            database.favoriteDao().removeFavorite(track.uri)
        } else {
            database.favoriteDao().addFavorite(
                FavoriteEntity(
                    mediaUri = track.uri,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    durationMs = track.durationMs,
                    isVideo = track.isVideo,
                    albumArtUri = track.albumArtUri
                )
            )
        }
    }

    fun isFavorite(mediaUri: String): Flow<Boolean> = database.favoriteDao().isFavorite(mediaUri)

    // Playback History
    suspend fun recordPlayback(track: MediaTrack, positionMs: Long) = withContext(Dispatchers.IO) {
        database.historyDao().insertOrUpdate(
            PlaybackHistoryEntity(
                mediaUri = track.uri,
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationMs = track.durationMs,
                isVideo = track.isVideo,
                albumArtUri = track.albumArtUri,
                lastPlayedAt = System.currentTimeMillis(),
                lastPositionMs = positionMs
            )
        )
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        database.historyDao().clearHistory()
    }

    // Playlists
    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        database.playlistDao().insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        database.playlistDao().getPlaylistById(playlistId)?.let {
            database.playlistDao().updatePlaylist(it.copy(name = newName))
        }
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) = withContext(Dispatchers.IO) {
        database.playlistDao().deletePlaylist(playlist)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, track: MediaTrack) = withContext(Dispatchers.IO) {
        database.playlistDao().insertTrack(
            PlaylistTrackEntity(
                playlistId = playlistId,
                mediaUri = track.uri,
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationMs = track.durationMs,
                isVideo = track.isVideo,
                albumArtUri = track.albumArtUri
            )
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, mediaUri: String) = withContext(Dispatchers.IO) {
        database.playlistDao().removeTrackFromPlaylist(playlistId, mediaUri)
    }

    fun getPlaylistTracks(playlistId: Long): Flow<List<PlaylistTrackEntity>> {
        return database.playlistDao().getTracksForPlaylist(playlistId)
    }

    // Sorting helper
    fun sortTracks(tracks: List<MediaTrack>, sortOption: SortOption): List<MediaTrack> {
        return when (sortOption) {
            SortOption.TITLE -> tracks.sortedBy { it.title.lowercase() }
            SortOption.ARTIST -> tracks.sortedBy { it.artist.lowercase() }
            SortOption.ALBUM -> tracks.sortedBy { it.album.lowercase() }
            SortOption.DURATION -> tracks.sortedByDescending { it.durationMs }
            SortOption.DATE_ADDED -> tracks.sortedByDescending { it.dateAdded }
        }
    }
}
