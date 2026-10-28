package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NovaDatabase
import com.example.data.local.entities.PlaylistEntity
import com.example.data.local.entities.PlaylistTrackEntity
import com.example.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: NovaDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, NovaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun readStringFromContext_matchesAppName() {
        val appName = context.getString(R.string.app_name)
        assertEquals("NOVA Player", appName)
    }

    @Test
    fun preferencesRepository_savesAndLoadsCorrectly() {
        val prefRepo = PreferencesRepository(context)
        assertEquals(true, prefRepo.playerPreferences.value.resumeLastTrack)
        prefRepo.setSkipInterval(15)
        assertEquals(15, prefRepo.playerPreferences.value.skipIntervalSeconds)
    }

    @Test
    fun playlistDatabase_insertAndRetrieveWorks() = runBlocking {
        val playlistId = database.playlistDao().insertPlaylist(PlaylistEntity(name = "Chill Vibes"))
        val playlists = database.playlistDao().getAllPlaylists().first()
        assertEquals(1, playlists.size)
        assertEquals("Chill Vibes", playlists[0].name)

        database.playlistDao().insertTrack(
            PlaylistTrackEntity(
                playlistId = playlistId,
                mediaUri = "content://audio/1",
                title = "Nova Track",
                artist = "Nova Artist",
                album = "Nova Album",
                durationMs = 60000L
            )
        )

        val tracks = database.playlistDao().getTracksForPlaylist(playlistId).first()
        assertEquals(1, tracks.size)
        assertEquals("Nova Track", tracks[0].title)
    }
}
