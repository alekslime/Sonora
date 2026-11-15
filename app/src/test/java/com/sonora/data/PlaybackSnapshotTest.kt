package com.sonora.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSnapshotTest {

    private fun album() = Album(
        id = "media-9",
        title = "Real Things",
        artist = "Artist",
        year = 1973,
        label = "Local files",
        tracks = listOf(
            Track(1, "One", 200_000L, "uri:1"),
            Track(2, "Two", 180_000L, "uri:2"),
            Track(4, "Four", 220_000L, "uri:3"),
        ),
        artSeed = 99L,
        artUri = "uri:1",
    )

    @Test
    fun roundTrips() {
        val snapshot = PlaybackSnapshot.from(album(), 2, 42_000L, true, shuffle = true, repeatOne = false)!!
        val decoded = PlaybackSnapshot.decode(PlaybackSnapshot.encode(snapshot))!!
        assertEquals(snapshot, decoded)
    }

    @Test
    fun capturesFlags() {
        val snapshot = PlaybackSnapshot.from(album(), 0, 0L, false, shuffle = false, repeatOne = true)!!
        assertEquals(false, snapshot.wasPlaying)
        assertEquals(true, snapshot.repeatOne)
        val decoded = PlaybackSnapshot.decode(PlaybackSnapshot.encode(snapshot))!!
        assertEquals(false, decoded.wasPlaying)
        assertEquals(true, decoded.repeatOne)
    }

    @Test
    fun clampsIndexAndPosition() {
        val snapshot = PlaybackSnapshot.from(album(), 99, -5L, true, false, false)!!
        assertEquals(2, snapshot.trackIndex)
        assertEquals(0L, snapshot.positionMs)
    }

    @Test
    fun rejectsDemoAlbums() {
        val demo = album().copy(tracks = album().tracks.map { it.copy(uri = null) })
        assertEquals(null, PlaybackSnapshot.from(demo, 0, 0L, true, false, false))
    }

    @Test
    fun rebuildsARealAlbum() {
        val snapshot = PlaybackSnapshot.from(album(), 1, 1000L, true, false, false)!!
        val rebuilt = snapshot.toAlbum()!!
        assertEquals(album(), rebuilt)
        assertTrue(rebuilt.isReal)
    }

    @Test
    fun scrubsControlCharacters() {
        val tricky = album().copy(
            title = "A\u001FB",
            tracks = listOf(Track(1, "X\u001EY", 1000L, "uri:1")),
        )
        val decoded = PlaybackSnapshot.decode(
            PlaybackSnapshot.encode(PlaybackSnapshot.from(tricky, 0, 0L, true, false, false)!!),
        )!!
        assertEquals("A B", decoded.albumTitle)
        assertEquals("X Y", decoded.trackTitles.single())
    }

    @Test
    fun rejectsMalformed() {
        assertEquals(null, PlaybackSnapshot.decode(null))
        assertEquals(null, PlaybackSnapshot.decode(""))
        assertEquals(null, PlaybackSnapshot.decode("a\u001Fb\u001Fc"))
        val good = PlaybackSnapshot.encode(PlaybackSnapshot.from(album(), 0, 0L, true, false, false)!!)
        // Truncated field count.
        assertEquals(null, PlaybackSnapshot.decode(good.substringBeforeLast("\u001F")))
        // Mismatched list lengths.
        val fields = good.split("\u001F").toMutableList()
        fields[8] = fields[8] + "\u001EExtra"
        assertEquals(null, PlaybackSnapshot.decode(fields.joinToString("\u001F")))
        // Bad numbers.
        fields[8] = album().tracks.joinToString("\u001E") { it.title }
        fields[3] = "not-a-year"
        assertEquals(null, PlaybackSnapshot.decode(fields.joinToString("\u001F")))
        fields[3] = "1973"
        fields[12] = "not-a-position"
        assertEquals(null, PlaybackSnapshot.decode(fields.joinToString("\u001F")))
    }
}
