package com.sonora.nowplaying

import com.sonora.data.Album
import com.sonora.data.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LikedSongsTest {

    private fun album() = Album(
        id = "a1",
        title = "T",
        artist = "A",
        year = 2000,
        label = "L",
        tracks = listOf(
            Track(1, "Real", 120_000L, uri = "content://media/audio/7"),
            Track(2, "Demo", 180_000L),
        ),
        artSeed = 1L,
    )

    @Test
    fun keyPrefersStableUri() {
        assertEquals("content://media/audio/7", LikedStore.keyFor(album(), 0))
    }

    @Test
    fun keyFallsBackToAlbumAndIndex() {
        assertEquals("a1#1", LikedStore.keyFor(album(), 1))
        assertEquals("a1#9", LikedStore.keyFor(album(), 9))
    }

    @Test
    fun codecRoundTrips() {
        val keys = setOf("content://media/audio/7", "a1#1", "b2#0")
        assertEquals(keys, LikedStore.decode(LikedStore.encode(keys)))
    }

    @Test
    fun decodeDegradesToEmpty() {
        assertTrue(LikedStore.decode(null).isEmpty())
        assertTrue(LikedStore.decode("").isEmpty())
        assertTrue(LikedStore.decode("  \n \n").isEmpty())
    }

    @Test
    fun decodeSkipsBlankLines() {
        assertEquals(setOf("x", "y"), LikedStore.decode("x\n\n  \ny\n"))
    }

    @Test
    fun toggleAddsThenRemoves() {
        LikedStore.replace(emptySet())
        LikedStore.toggle("k")
        assertTrue(LikedStore.isLiked("k"))
        LikedStore.toggle("k")
        assertFalse(LikedStore.isLiked("k"))
        LikedStore.replace(emptySet())
    }

    @Test
    fun resolveFindsUriAndFallbackKeysInOrder() {
        val second = album().copy(id = "a2", tracks = listOf(Track(1, "Solo", 60_000L)))
        val entries = LikedStore.resolve(
            setOf("a2#0", "content://media/audio/7", "ghost#9"),
            listOf(album(), second),
        )
        assertEquals(2, entries.size)
        assertEquals("a1", entries[0].album.id)
        assertEquals(0, entries[0].trackIndex)
        assertEquals("a2", entries[1].album.id)
        assertEquals(0, entries[1].trackIndex)
    }

    @Test
    fun resolveEmptyWhenNothingMatches() {
        assertTrue(LikedStore.resolve(emptySet(), listOf(album())).isEmpty())
        assertTrue(LikedStore.resolve(setOf("x#0"), listOf(album())).isEmpty())
        assertTrue(LikedStore.resolve(setOf("a1#1"), emptyList()).isEmpty())
    }

    @Test
    fun keyPrefersOriginOverQueueScope() {
        val queued = album().copy(
            id = "queue/all",
            tracks = listOf(Track(1, "Demo", 180_000L, originKey = "a1#1")),
        )
        assertEquals("a1#1", LikedStore.keyFor(queued, 0))
    }
}
