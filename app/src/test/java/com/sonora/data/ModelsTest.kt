package com.sonora.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {

    private fun track(number: Int, uri: String? = null) =
        Track(number = number, title = "T$number", durationMs = 180_000L, uri = uri)

    private fun albumOf(vararg uris: String?) = Album(
        id = "a",
        title = "T",
        artist = "A",
        year = 1973,
        label = "L",
        tracks = uris.mapIndexed { index, uri -> track(index + 1, uri) },
        artSeed = 1L,
    )

    @Test
    fun realNeedsEveryTrackUri() {
        assertTrue(albumOf("u1", "u2").isReal)
        assertFalse(albumOf("u1", null).isReal)
        assertFalse(albumOf(null, null).isReal)
    }

    @Test
    fun emptyAlbumIsNotReal() {
        assertFalse(albumOf().isReal)
    }

    @Test
    fun displayYearHidesUnknown() {
        assertEquals("1973", albumOf().displayYear)
        assertEquals("----", albumOf().copy(year = 0).displayYear)
        assertEquals("----", albumOf().copy(year = -4).displayYear)
    }

    @Test
    fun mockTracksDefaultToNoUri() {
        val track = Track(number = 1, title = "T", durationMs = 1000L)
        assertEquals(null, track.uri)
    }
}
