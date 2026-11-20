package com.sonora.nowplaying

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistsTest {

    @Test
    fun createRejectsBlankNames() {
        PlaylistsStore.replace(emptyList())
        assertNull(PlaylistsStore.create("   "))
        assertTrue(PlaylistsStore.lists.isEmpty())
        val created = PlaylistsStore.create("  Gym  ")!!
        assertEquals("Gym", created.name)
        assertTrue(created.id.isNotBlank())
        PlaylistsStore.replace(emptyList())
    }

    @Test
    fun membershipTogglesAndDeletes() {
        PlaylistsStore.replace(emptyList())
        val id = PlaylistsStore.create("Gym")!!.id
        assertTrue(PlaylistsStore.toggleTrack(id, "k1"))
        assertTrue(PlaylistsStore.contains(id, "k1"))
        PlaylistsStore.addTrack(id, "k1")
        assertEquals(1, PlaylistsStore.lists.single().keys.size)
        assertFalse(PlaylistsStore.toggleTrack(id, "k1"))
        assertFalse(PlaylistsStore.contains(id, "k1"))
        PlaylistsStore.removeTrack(id, "missing")
        PlaylistsStore.delete(id)
        assertTrue(PlaylistsStore.lists.isEmpty())
    }

    @Test
    fun codecRoundTripsAndSkipsMalformed() {
        val lists = listOf(
            Playlist("p1", "Gym", listOf("k1", "k2")),
            Playlist("p2", "Empty", emptyList()),
        )
        assertEquals(lists, PlaylistsStore.decode(PlaylistsStore.encode(lists)))
        assertTrue(PlaylistsStore.decode(null).isEmpty())
        assertTrue(PlaylistsStore.decode("  ").isEmpty())
        val mixed = PlaylistsStore.decode("p1\u001FGym\u001Fk1\nbroken\n\u001Fnoname\u001Fk")
        assertEquals(1, mixed.size)
        assertEquals("Gym", mixed.single().name)
    }
}
