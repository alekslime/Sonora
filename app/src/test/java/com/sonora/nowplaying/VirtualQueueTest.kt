package com.sonora.nowplaying

import com.sonora.data.Album
import com.sonora.data.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VirtualQueueTest {

    private fun albums() = listOf(
        Album(
            id = "a1", title = "T1", artist = "Ann", year = 2000, label = "L",
            tracks = listOf(
                Track(1, "One", 120_000L, uri = "content://media/audio/7"),
                Track(2, "Two", 180_000L),
            ),
            artSeed = 11L,
        ),
        Album(
            id = "a2", title = "T2", artist = "Bob", year = 2001, label = "L",
            tracks = listOf(Track(1, "Solo", 60_000L)),
            artSeed = 22L,
        ),
    )

    @Test
    fun queueAlbumCarriesTracksAndFirstCover() {
        val entries = listOf(LikedEntry(albums()[0], 1), LikedEntry(albums()[1], 0))
        val album = buildQueueAlbum(QueueSpecs.all(2), entries)
        assertEquals("queue/all", album.id)
        assertEquals("All Songs", album.title)
        assertEquals(listOf("Two", "Solo"), album.tracks.map { it.title })
        assertEquals(listOf(1, 2), album.tracks.map { it.number })
        assertEquals(11L, album.artSeed)
    }

    @Test
    fun queueTracksKeepCanonicalKeys() {
        val entries = listOf(LikedEntry(albums()[0], 1), LikedEntry(albums()[1], 0))
        val queue = buildQueueAlbum(QueueSpecs.all(2), entries)
        assertEquals("a1#1", LikedStore.keyFor(queue, 0))
        assertEquals("a2#0", LikedStore.keyFor(queue, 1))
        // Round-trip: keys minted in the queue resolve in the library.
        val resolved = LikedStore.resolveOrdered(
            listOf(LikedStore.keyFor(queue, 0), LikedStore.keyFor(queue, 1)),
            albums(),
        )
        assertEquals(entries, resolved)
    }

    @Test
    fun queueIdsCarryNoColons() {
        listOf(
            QueueIds.ALL,
            QueueIds.LIKED,
            QueueIds.playlist("abc-123"),
            QueueIds.artist("AC/DC: Live"),
        ).forEach { assertTrue(!it.contains(":")) }
        assertTrue(QueueIds.isQueue(QueueIds.playlist("x")))
    }

    @Test
    fun rebuildAllAndLiked() {
        val all = rebuildQueue(QueueIds.ALL, albums(), emptySet(), emptyList())!!
        assertEquals(3, all.tracks.size)
        // Titles sorted: One, Solo, Two.
        assertEquals(listOf("One", "Solo", "Two"), all.tracks.map { it.title })
        val liked = rebuildQueue(QueueIds.LIKED, albums(), setOf("a2#0"), emptyList())!!
        assertEquals(listOf("Solo"), liked.tracks.map { it.title })
        assertNull(rebuildQueue(QueueIds.LIKED, albums(), emptySet(), emptyList()))
        assertNull(rebuildQueue(QueueIds.ALL, emptyList(), emptySet(), emptyList()))
    }

    @Test
    fun rebuildPlaylistKeepsOrderAndName() {
        val playlists = listOf(Playlist("p1", "Gym", listOf("a2#0", "content://media/audio/7")))
        val album = rebuildQueue(QueueIds.playlist("p1"), albums(), emptySet(), playlists)!!
        assertEquals("Gym", album.title)
        assertEquals(listOf("Solo", "One"), album.tracks.map { it.title })
        assertNull(rebuildQueue(QueueIds.playlist("gone"), albums(), emptySet(), playlists))
        assertNull(
            rebuildQueue(
                QueueIds.playlist("p1"), albums(), emptySet(),
                listOf(Playlist("p1", "Gym", listOf("ghost#0"))),
            ),
        )
    }

    @Test
    fun rebuildArtistMatchesDecodedName() {
        val album = rebuildQueue(QueueIds.artist("Bob"), albums(), emptySet(), emptyList())!!
        assertEquals("Bob", album.title)
        assertEquals(listOf("Solo"), album.tracks.map { it.title })
        assertNull(rebuildQueue(QueueIds.artist("Nobody"), albums(), emptySet(), emptyList()))
    }

    @Test
    fun artistSlugRoundTripsUnsafeChars() {
        val name = "AC/DC: Live #1 (100%)"
        assertEquals(name, unslugArtist(slugArtist(name)))
        assertTrue(!QueueIds.artist(name).contains(":"))
        val tricky = albums() + Album(
            id = "a9", title = "T9", artist = name, year = 2020, label = "L",
            tracks = listOf(Track(1, "Hit", 60_000L)),
            artSeed = 9L,
        )
        val album = rebuildQueue(QueueIds.artist(name), tricky, emptySet(), emptyList())!!
        assertEquals(listOf("Hit"), album.tracks.map { it.title })
    }

    @Test
    fun rebuildRejectsUnknownIds() {
        assertNull(rebuildQueue("a1", albums(), emptySet(), emptyList()))
        assertNull(rebuildQueue("queue/bogus", albums(), emptySet(), emptyList()))
    }

    @Test
    fun resolveOrderedKeepsGivenOrder() {
        val entries = LikedStore.resolveOrdered(
            listOf("a2#0", "content://media/audio/7", "ghost#0"),
            albums(),
        )
        assertEquals(2, entries.size)
        assertEquals("a2", entries[0].album.id)
        assertEquals("a1", entries[1].album.id)
        assertTrue(LikedStore.resolveOrdered(emptyList(), albums()).isEmpty())
    }
}
