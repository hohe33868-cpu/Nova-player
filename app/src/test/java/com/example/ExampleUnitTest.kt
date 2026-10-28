package com.example

import com.example.data.model.MediaTrack
import com.example.data.model.RepeatMode
import com.example.data.model.SortOption
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun formattedDuration_formatsCorrectly() {
        val shortTrack = MediaTrack(
            id = 1,
            uri = "test://1",
            title = "Track 1",
            artist = "Artist",
            album = "Album",
            durationMs = 40_000L,
            sizeBytes = 1000L,
            path = "",
            dateAdded = 0L
        )
        assertEquals("0:40", shortTrack.formattedDuration)

        val longTrack = shortTrack.copy(durationMs = 125_000L)
        assertEquals("2:05", longTrack.formattedDuration)

        val hourTrack = shortTrack.copy(durationMs = 3_665_000L)
        assertEquals("1:01:05", hourTrack.formattedDuration)
    }

    @Test
    fun repeatMode_cyclesCorrectly() {
        assertEquals(RepeatMode.ALL, RepeatMode.OFF.next())
        assertEquals(RepeatMode.ONE, RepeatMode.ALL.next())
        assertEquals(RepeatMode.OFF, RepeatMode.ONE.next())
    }

    @Test
    fun formattedSize_displaysAppropriateUnits() {
        val track = MediaTrack(
            id = 2,
            uri = "test://2",
            title = "Track 2",
            artist = "Artist",
            album = "Album",
            durationMs = 0L,
            sizeBytes = 15_000_000L,
            path = "",
            dateAdded = 0L
        )
        assertEquals("14.3 MB", track.formattedSize)
    }
}
