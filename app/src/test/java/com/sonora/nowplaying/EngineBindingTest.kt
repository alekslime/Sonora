package com.sonora.nowplaying

import com.sonora.data.Album
import com.sonora.data.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class EngineBindingTest {

    private class FakeEngine : EngineHandle {
        val calls = mutableListOf<String>()
        override fun enginePlay(index: Int, uri: String) {
            calls += "play:$index:$uri"
        }
        override fun engineResume() {
            calls += "resume"
        }
        override fun enginePause() {
            calls += "pause"
        }
        override fun engineSeekTo(positionMs: Long) {
            calls += "seek:$positionMs"
        }
    }

    private fun realAlbum() = Album(
        id = "media-1",
        title = "Real",
        artist = "Artist",
        year = 1973,
        label = "Local files",
        tracks = listOf(
            Track(1, "One", 200_000L, "uri:1"),
            Track(2, "Two", 200_000L, "uri:2"),
            Track(3, "Three", 200_000L, "uri:3"),
        ),
        artSeed = 1L,
        artUri = "uri:1",
    )

    @Test
    fun nextForwardsToEngine() {
        val engine = FakeEngine()
        val state = NowPlayingState(realAlbum(), 0).apply { this.engine = engine }
        state.next()
        assertEquals(1, state.trackIndex)
        assertEquals(listOf("play:1:uri:2"), engine.calls)
    }

    @Test
    fun prevForwardsRestartToEngine() {
        val engine = FakeEngine()
        val state = NowPlayingState(realAlbum(), 1).apply { this.engine = engine }
        state.seekTo(60_000L)
        engine.calls.clear()
        state.prev()
        assertEquals(1, state.trackIndex)
        assertEquals(0L, state.positionMs)
        assertEquals(listOf("play:1:uri:2"), engine.calls)
    }

    @Test
    fun stopBranchPausesEngineAtEnd() {
        val engine = FakeEngine()
        val state = NowPlayingState(realAlbum(), 2).apply { this.engine = engine }
        state.next()
        assertEquals(false, state.isPlaying)
        assertEquals(listOf("pause", "seek:200000"), engine.calls)
    }

    @Test
    fun playPauseSeekForward() {
        val engine = FakeEngine()
        val state = NowPlayingState(realAlbum(), 0).apply { this.engine = engine }
        state.play()
        state.seekTo(5000L)
        state.pause()
        assertEquals(listOf("resume", "seek:5000", "pause"), engine.calls)
    }

    @Test
    fun syncTrackAdoptsEngineIndex() {
        val state = NowPlayingState(realAlbum(), 0).apply { engine = FakeEngine() }
        state.syncTrack(2)
        assertEquals(2, state.trackIndex)
        assertEquals(0L, state.positionMs)
    }

    @Test
    fun syncTrackIgnoresInvalidIndex() {
        val state = NowPlayingState(realAlbum(), 0).apply { engine = FakeEngine() }
        state.syncTrack(99)
        assertEquals(0, state.trackIndex)
    }

    @Test
    fun syncPlaybackAdoptsAndClamps() {
        val state = NowPlayingState(realAlbum(), 0).apply { engine = FakeEngine() }
        state.syncPlayback(12_000L, true)
        assertEquals(12_000L, state.positionMs)
        assertEquals(true, state.isPlaying)
        state.syncPlayback(999_999L, true)
        assertEquals(200_000L, state.positionMs)
        assertEquals(false, state.isPlaying)
    }

    @Test
    fun syncIsIgnoredWithoutEngine() {
        val state = NowPlayingState(realAlbum(), 0)
        state.syncTrack(2)
        state.syncPlayback(12_000L, true)
        state.onPlaylistEnded()
        assertEquals(0, state.trackIndex)
        assertEquals(0L, state.positionMs)
        assertEquals(false, state.isPlaying)
    }

    @Test
    fun playlistEndedWrapsWithRepeatAll() {
        val engine = FakeEngine()
        val state = NowPlayingState(realAlbum(), 2).apply { this.engine = engine }
        state.cycleRepeat() // OFF -> ALL
        state.onPlaylistEnded()
        assertEquals(0, state.trackIndex)
        assertEquals(listOf("play:0:uri:1"), engine.calls)
    }
}
