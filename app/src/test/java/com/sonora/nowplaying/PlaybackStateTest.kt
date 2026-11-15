package com.sonora.nowplaying

import com.sonora.data.Album
import com.sonora.data.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStateTest {

    private fun album() = Album(
        id = "t",
        title = "T",
        artist = "A",
        year = 2000,
        label = "L",
        tracks = listOf(
            Track(1, "One", 120_000L),
            Track(2, "Two", 180_000L),
            Track(3, "Three", 60_000L),
        ),
        artSeed = 1L,
    )

    @Test
    fun startsPausedAtRequestedTrack() {
        val state = NowPlayingState(album(), 1)
        assertEquals(1, state.trackIndex)
        assertEquals(0L, state.positionMs)
        assertFalse(state.isPlaying)
    }

    @Test
    fun startTrackIndexIsClamped() {
        assertEquals(0, NowPlayingState(album(), -5).trackIndex)
        assertEquals(2, NowPlayingState(album(), 99).trackIndex)
    }

    @Test
    fun toggleFlipsPlayback() {
        val state = NowPlayingState(album(), 0)
        state.toggle()
        assertTrue(state.isPlaying)
        state.toggle()
        assertFalse(state.isPlaying)
    }

    @Test
    fun seekClampsToDuration() {
        val state = NowPlayingState(album(), 0)
        state.seekTo(-10L)
        assertEquals(0L, state.positionMs)
        state.seekTo(999_999L)
        assertEquals(120_000L, state.positionMs)
        state.seekTo(30_000L)
        assertEquals(30_000L, state.positionMs)
        assertEquals(0.25f, state.fraction, 0.001f)
    }

    @Test
    fun advanceProgressesWhilePlayingOnly() {
        val state = NowPlayingState(album(), 0)
        state.advanceBy(1000L)
        assertEquals(0L, state.positionMs)
        state.play()
        state.advanceBy(1000L)
        assertEquals(1000L, state.positionMs)
    }

    @Test
    fun advanceRollsToNextTrackAtEnd() {
        val state = NowPlayingState(album(), 0)
        state.play()
        state.seekTo(119_950L)
        state.advanceBy(100L)
        assertEquals(1, state.trackIndex)
        assertEquals(0L, state.positionMs)
        assertTrue(state.isPlaying)
    }

    @Test
    fun stopsAtEndOfAlbum() {
        val state = NowPlayingState(album(), 2)
        state.play()
        state.seekTo(59_950L)
        state.advanceBy(100L)
        assertEquals(2, state.trackIndex)
        assertEquals(60_000L, state.positionMs)
        assertFalse(state.isPlaying)
    }

    @Test
    fun restoreClampsAndResumes() {
        val state = NowPlayingState(album(), 0)
        state.restore(5, 999_999L, true)
        assertEquals(2, state.trackIndex)
        assertEquals(60_000L, state.positionMs)
        assertFalse(state.isPlaying)
        state.restore(1, 30_000L, true)
        assertEquals(1, state.trackIndex)
        assertEquals(30_000L, state.positionMs)
        assertTrue(state.isPlaying)
    }

    @Test
    fun shuffleNextPicksADifferentTrack() {
        // Deterministic: the mock always picks a different index.
        val state = NowPlayingState(album(), 1)
        state.toggleShuffle()
        assertTrue(state.shuffle)
        state.next()
        assertTrue(state.trackIndex in 0..2)
        assertTrue(state.trackIndex != 1)
        assertEquals(0L, state.positionMs)
    }

    @Test
    fun repeatCyclesOffAllOne() {
        val state = NowPlayingState(album(), 0)
        assertEquals(RepeatMode.OFF, state.repeatMode)
        state.cycleRepeat()
        assertEquals(RepeatMode.ALL, state.repeatMode)
        state.cycleRepeat()
        assertEquals(RepeatMode.ONE, state.repeatMode)
        state.cycleRepeat()
        assertEquals(RepeatMode.OFF, state.repeatMode)
    }

    @Test
    fun repeatAllWrapsAtAlbumEnd() {
        val state = NowPlayingState(album(), 2)
        state.cycleRepeat()
        state.play()
        state.next()
        assertEquals(0, state.trackIndex)
        assertEquals(0L, state.positionMs)
        assertTrue(state.isPlaying)
    }

    @Test
    fun repeatOneRestartsTrackOnAdvance() {
        val state = NowPlayingState(album(), 1)
        state.cycleRepeat()
        state.cycleRepeat()
        state.play()
        state.seekTo(179_950L)
        state.advanceBy(100L)
        assertEquals(1, state.trackIndex)
        assertEquals(0L, state.positionMs)
        assertTrue(state.isPlaying)
    }

    @Test
    fun manualNextStillAdvancesUnderRepeatOne() {
        val state = NowPlayingState(album(), 0)
        state.cycleRepeat()
        state.cycleRepeat()
        state.next()
        assertEquals(1, state.trackIndex)
    }

    @Test
    fun restoreKeepsShuffleAndRepeat() {
        val state = NowPlayingState(album(), 0)
        state.restore(2, 10_000L, true, shuffleOn = true, repeat = RepeatMode.ALL)
        assertTrue(state.shuffle)
        assertEquals(RepeatMode.ALL, state.repeatMode)
        assertTrue(state.isPlaying)
    }

    @Test
    fun prevRestartsWhenPlayedPastThreshold() {
        val state = NowPlayingState(album(), 1)
        state.seekTo(10_000L)
        state.prev()
        assertEquals(1, state.trackIndex)
        assertEquals(0L, state.positionMs)
        state.prev()
        assertEquals(0, state.trackIndex)
    }
}
