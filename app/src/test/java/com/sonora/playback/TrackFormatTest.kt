package com.sonora.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackFormatTest {

    @Test
    fun mapsKnownCodecs() {
        assertEquals("FLAC", TrackFormat.describe("audio/flac", -1, -1, -1).codec)
        assertEquals("MP3", TrackFormat.describe("audio/mpeg", -1, -1, -1).codec)
        assertEquals("AAC", TrackFormat.describe("audio/mp4a-latm", -1, -1, -1).codec)
        assertEquals("ALAC", TrackFormat.describe("audio/alac", -1, -1, -1).codec)
        assertEquals("Vorbis", TrackFormat.describe("audio/vorbis", -1, -1, -1).codec)
        assertEquals("Opus", TrackFormat.describe("audio/opus", -1, -1, -1).codec)
        assertEquals("WAV", TrackFormat.describe("audio/x-wav", -1, -1, -1).codec)
    }

    @Test
    fun fallsBackToMimeSubtype() {
        assertEquals("X-XYZ", TrackFormat.describe("audio/x-xyz", -1, -1, -1).codec)
        assertEquals("Audio", TrackFormat.describe(null, -1, -1, -1).codec)
    }

    @Test
    fun mapsNumbersAndChannels() {
        val format = TrackFormat.describe("audio/flac", 912_000, 44100, 2)
        assertEquals(912, format.bitrateKbps)
        assertEquals(44.1f, format.sampleRateKhz!!, 0.001f)
        assertEquals("Stereo", format.channels)
    }

    @Test
    fun unknownNumbersStayNull() {
        val format = TrackFormat.describe("audio/mpeg", -1, 0, 0)
        assertEquals(null, format.bitrateKbps)
        assertEquals(null, format.sampleRateKhz)
        assertEquals(null, format.channels)
    }

    @Test
    fun mapsSurroundChannels() {
        assertEquals("Mono", TrackFormat.describe(null, -1, -1, 1).channels)
        assertEquals("5.1", TrackFormat.describe(null, -1, -1, 6).channels)
        assertEquals("7.1", TrackFormat.describe(null, -1, -1, 8).channels)
        assertEquals("4 ch", TrackFormat.describe(null, -1, -1, 4).channels)
    }

    @Test
    fun displayLineSkipsUnknowns() {
        assertEquals(
            "FLAC · 44.1 kHz · Stereo · 912 kbps",
            TrackFormat.describe("audio/flac", 912_000, 44100, 2).displayLine,
        )
        assertEquals(
            "MP3 · 48 kHz",
            TrackFormat.describe("audio/mpeg", -1, 48000, -1).displayLine,
        )
        assertEquals("Audio", TrackFormat.describe(null, -1, -1, -1).displayLine)
    }
}
