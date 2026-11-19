package com.sonora.nowplaying

import com.sonora.data.Album
import com.sonora.data.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayHistoryTest {

    private fun albums() = listOf(
        Album(
            id = "a1", title = "T1", artist = "A", year = 2000, label = "L",
            tracks = listOf(
                Track(1, "One", 120_000L, uri = "content://media/audio/7"),
                Track(2, "Two", 180_000L),
            ),
            artSeed = 1L,
        ),
        Album(
            id = "a2", title = "T2", artist = "A", year = 2001, label = "L",
            tracks = listOf(Track(1, "Solo", 60_000L)),
            artSeed = 2L,
        ),
    )

    @Test
    fun replayJumpsToFrontAndCaps() {
        PlayHistory.replace(emptyList())
        PlayHistory.record("a1#1")
        PlayHistory.record("content://media/audio/7")
        PlayHistory.record("a1#1")
        assertEquals(listOf("a1#1", "content://media/audio/7"), PlayHistory.entries)
        repeat(PlayHistory.MAX_ENTRIES + 5) { PlayHistory.record("k$it") }
        assertEquals(PlayHistory.MAX_ENTRIES, PlayHistory.entries.size)
        assertEquals("k${PlayHistory.MAX_ENTRIES + 4}", PlayHistory.entries.first())
        PlayHistory.replace(emptyList())
    }

    @Test
    fun resolveKeepsRecencyAndDropsMissing() {
        PlayHistory.replace(listOf("a2#0", "ghost#0", "content://media/audio/7"))
        val entries = PlayHistory.resolve(albums())
        assertEquals(2, entries.size)
        assertEquals("a2", entries[0].album.id)
        assertEquals("a1", entries[1].album.id)
        assertEquals(0, entries[1].trackIndex)
        PlayHistory.replace(emptyList())
    }

    @Test
    fun codecRoundTripsAndDegrades() {
        val keys = listOf("a2#0", "content://media/audio/7")
        assertEquals(keys, PlayHistory.decode(PlayHistory.encode(keys)))
        assertTrue(PlayHistory.decode(null).isEmpty())
        assertTrue(PlayHistory.decode("  \n ").isEmpty())
        assertEquals(listOf("x"), PlayHistory.decode("x\n\n"))
    }
}
