package com.sonora.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockLibraryTest {

    @Test
    fun libraryHasTwentyAlbums() {
        assertEquals(20, MockLibrary.albums.size)
    }

    @Test
    fun albumsHaveIdentityAndTracks() {
        MockLibrary.albums.forEach { album ->
            assertTrue(album.title.isNotBlank())
            assertTrue(album.artist.isNotBlank())
            assertTrue(album.tracks.size in 7..12)
            assertTrue(album.totalDurationMs > 0)
            album.tracks.forEach { track ->
                assertTrue(track.title.isNotBlank())
                assertTrue(track.durationMs > 0)
            }
            // Track titles are unique within an album.
            assertEquals(album.tracks.size, album.tracks.map { it.title }.toSet().size)
        }
    }

    @Test
    fun albumByIdResolves() {
        val first = MockLibrary.albums.first()
        assertEquals(first, MockLibrary.albumById(first.id))
    }

    @Test
    fun durationFormatting() {
        assertEquals("3:04", 184_000L.toMinSec())
        assertEquals("0:00", 0L.toMinSec())
        assertNotNull(61_000L.toMinSec())
    }
}
