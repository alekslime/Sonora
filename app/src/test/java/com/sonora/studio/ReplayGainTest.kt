package com.sonora.studio

import org.junit.Assert.assertEquals
import org.junit.Test

class ReplayGainTest {

    // --- Builders ----------------------------------------------------------

    private fun synchsafe(value: Int) = byteArrayOf(
        (value shr 21 and 0x7F).toByte(),
        (value shr 14 and 0x7F).toByte(),
        (value shr 7 and 0x7F).toByte(),
        (value and 0x7F).toByte(),
    )

    private fun int32be(value: Int) = byteArrayOf(
        (value shr 24 and 0xFF).toByte(),
        (value shr 16 and 0xFF).toByte(),
        (value shr 8 and 0xFF).toByte(),
        (value and 0xFF).toByte(),
    )

    private fun txxx(description: String, value: String, v24: Boolean): ByteArray {
        val payload = byteArrayOf(0) +
            description.toByteArray(Charsets.ISO_8859_1) + byteArrayOf(0) +
            value.toByteArray(Charsets.ISO_8859_1)
        val size = if (v24) synchsafe(payload.size) else int32be(payload.size)
        return "TXXX".toByteArray(Charsets.US_ASCII) + size + byteArrayOf(0, 0) + payload
    }

    private fun id3(frames: ByteArray, v24: Boolean): ByteArray {
        val header = "ID3".toByteArray(Charsets.US_ASCII) +
            byteArrayOf(if (v24) 4 else 3, 0, 0) + synchsafe(frames.size)
        return header + frames
    }

    private fun le32(value: Int) = byteArrayOf(
        (value and 0xFF).toByte(),
        (value shr 8 and 0xFF).toByte(),
        (value shr 16 and 0xFF).toByte(),
        (value shr 24 and 0xFF).toByte(),
    )

    private fun vorbisComment(vararg entries: String): ByteArray {
        var out = le32(6) + "SONORA".toByteArray(Charsets.UTF_8) + le32(entries.size)
        entries.forEach { entry ->
            val bytes = entry.toByteArray(Charsets.UTF_8)
            out += le32(bytes.size) + bytes
        }
        return out
    }

    private fun flac(comment: ByteArray?): ByteArray {
        var out = "fLaC".toByteArray(Charsets.US_ASCII)
        // STREAMINFO block (type 0): 34 zero bytes, never last here.
        out += byteArrayOf(0x00) + byteArrayOf(0x00, 0x00, 0x22) + ByteArray(34)
        if (comment != null) {
            val length = comment.size
            out += byteArrayOf(0x84.toByte()) + // Last block, type 4.
                byteArrayOf((length shr 16).toByte(), (length shr 8).toByte(), length.toByte()) +
                comment
        } else {
            out[4] = 0x80.toByte() // STREAMINFO becomes last.
        }
        return out
    }

    // --- parseGainDb -------------------------------------------------------

    @Test
    fun parsesGainStrings() {
        assertEquals(-6.54f, ReplayGain.parseGainDb("-6.54 dB")!!, 0.001f)
        assertEquals(3f, ReplayGain.parseGainDb("+3 dB")!!, 0.001f)
        assertEquals(-1.5f, ReplayGain.parseGainDb("  -1.5  ")!!, 0.001f)
        assertEquals(0f, ReplayGain.parseGainDb("0 dB")!!, 0.001f)
    }

    @Test
    fun rejectsBadGains() {
        assertEquals(null, ReplayGain.parseGainDb(""))
        assertEquals(null, ReplayGain.parseGainDb("dB"))
        assertEquals(null, ReplayGain.parseGainDb("bogus"))
        assertEquals(null, ReplayGain.parseGainDb("dB -3"))
    }

    @Test
    fun clampsWildGains() {
        assertEquals(30f, ReplayGain.parseGainDb("+99 dB")!!, 0.001f)
        assertEquals(-30f, ReplayGain.parseGainDb("-99")!!, 0.001f)
    }

    // --- ID3v2 -------------------------------------------------------------

    @Test
    fun readsId3v23TrackGain() {
        val tag = id3(
            txxx("replaygain_track_gain", "-5.00 dB", v24 = false) +
                txxx("replaygain_track_peak", "0.98", v24 = false),
            v24 = false,
        )
        val tags = ReplayGain.parseId3v2(tag)!!
        assertEquals(-5f, tags.trackGainDb!!, 0.001f)
        assertEquals(0.98f, tags.trackPeak!!, 0.001f)
        assertEquals(null, tags.albumGainDb)
    }

    @Test
    fun readsId3v24AlbumGain() {
        val tag = id3(
            txxx("REPLAYGAIN_ALBUM_GAIN", "-7.25 dB", v24 = true),
            v24 = true,
        )
        val tags = ReplayGain.parseId3v2(tag)!!
        assertEquals(-7.25f, tags.albumGainDb!!, 0.001f)
        assertEquals(null, tags.trackGainDb)
    }

    @Test
    fun ignoresNonReplayGainFrames() {
        val tit2 = "TIT2".toByteArray(Charsets.US_ASCII) + int32be(6) + byteArrayOf(0, 0) +
            byteArrayOf(0) + "Title".toByteArray(Charsets.ISO_8859_1)
        assertEquals(null, ReplayGain.parseId3v2(id3(tit2, v24 = false)))
    }

    @Test
    fun rejectsTruncatedAndForeignTags() {
        assertEquals(null, ReplayGain.parseId3v2(ByteArray(0)))
        assertEquals(null, ReplayGain.parseId3v2("ID3".toByteArray()))
        assertEquals(null, ReplayGain.parseId3v2("fLaCjunkdata".toByteArray()))
        val tag = id3(txxx("replaygain_track_gain", "-5 dB", v24 = false), v24 = false)
        assertEquals(null, ReplayGain.parseId3v2(tag.copyOf(15)))
    }

    // --- FLAC / Vorbis -----------------------------------------------------

    @Test
    fun readsFlacComments() {
        val tags = ReplayGain.parseFlac(
            flac(
                vorbisComment(
                    "REPLAYGAIN_TRACK_GAIN=-4.20 dB",
                    "replaygain_album_gain=-6.00 dB",
                    "REPLAYGAIN_TRACK_PEAK=0.891251",
                    "TITLE=Something",
                ),
            ),
        )!!
        assertEquals(-4.2f, tags.trackGainDb!!, 0.001f)
        assertEquals(-6f, tags.albumGainDb!!, 0.001f)
        assertEquals(0.891f, tags.trackPeak!!, 0.001f)
    }

    @Test
    fun flacWithoutCommentsReadsNull() {
        assertEquals(null, ReplayGain.parseFlac(flac(null)))
        assertEquals(null, ReplayGain.parseFlac(flac(vorbisComment("TITLE=X"))))
    }

    @Test
    fun rejectsTruncatedFlac() {
        assertEquals(null, ReplayGain.parseFlac(ByteArray(0)))
        assertEquals(null, ReplayGain.parseFlac("fLaC".toByteArray()))
        val full = flac(vorbisComment("REPLAYGAIN_TRACK_GAIN=-1 dB"))
        assertEquals(null, ReplayGain.parseFlac(full.copyOf(20)))
    }

    @Test
    fun rejectsMalformedComments() {
        assertEquals(null, ReplayGain.parseVorbisComment(ByteArray(0)))
        assertEquals(null, ReplayGain.parseVorbisComment(le32(4) + "ab".toByteArray()))
        assertEquals(null, ReplayGain.parseVorbisComment(le32(1) + "x".toByteArray() + le32(99)))
    }

    // --- Gain math ---------------------------------------------------------

    @Test
    fun offModeIsUnity() {
        val tags = ReplayGainTags(trackGainDb = -10f, albumGainDb = -10f)
        assertEquals(1f, ReplayGain.gainFor(tags, ReplayGainMode.OFF), 0.001f)
        assertEquals(1f, ReplayGain.gainFor(tags, ReplayGainMode.OFF), 0.001f)
    }

    @Test
    fun missingTagsAreUnity() {
        assertEquals(1f, ReplayGain.gainFor(null, ReplayGainMode.TRACK), 0.001f)
        assertEquals(1f, ReplayGain.gainFor(ReplayGainTags(), ReplayGainMode.ALBUM), 0.001f)
        // Track mode never borrows the album value.
        val albumOnly = ReplayGainTags(albumGainDb = -8f)
        assertEquals(1f, ReplayGain.gainFor(albumOnly, ReplayGainMode.TRACK), 0.001f)
    }

    @Test
    fun appliesModeGain() {
        val tags = ReplayGainTags(trackGainDb = -6f, albumGainDb = -12f)
        assertEquals(0.501f, ReplayGain.gainFor(tags, ReplayGainMode.TRACK), 0.001f)
        assertEquals(0.251f, ReplayGain.gainFor(tags, ReplayGainMode.ALBUM), 0.001f)
    }

    @Test
    fun peakLimitsHotGains() {
        val tags = ReplayGainTags(trackGainDb = 6f, trackPeak = 0.9f)
        // 10^(6/20) = 1.995 would clip at peak 0.9 -> pulled to 1/0.9.
        assertEquals(1.111f, ReplayGain.gainFor(tags, ReplayGainMode.TRACK), 0.001f)
    }

    @Test
    fun lowPeaksPassThrough() {
        val tags = ReplayGainTags(trackGainDb = -6f, trackPeak = 0.9f)
        assertEquals(0.501f, ReplayGain.gainFor(tags, ReplayGainMode.TRACK), 0.001f)
    }

    @Test
    fun combinedGainMultipliesAndClamps() {
        assertEquals(1f, ReplayGain.combinedGain(0f, 1f, 1f), 0.001f)
        assertEquals(0.25f, ReplayGain.combinedGain(0f, 0.5f, 0.5f), 0.001f)
        assertEquals(1.995f, ReplayGain.combinedGain(6f, 1f, 1f), 0.001f)
        assertEquals(0f, ReplayGain.combinedGain(0f, 1f, 0f), 0.001f)
        assertEquals(4f, ReplayGain.combinedGain(12f, 4f, 1f), 0.001f)
    }
}
