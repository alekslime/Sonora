package com.sonora.studio

import android.content.ContentResolver
import android.net.Uri
import kotlin.math.pow

/** Parsed ReplayGain tags. Null fields mean the tag was absent. */
data class ReplayGainTags(
    val trackGainDb: Float? = null,
    val albumGainDb: Float? = null,
    val trackPeak: Float? = null,
    val albumPeak: Float? = null,
) {
    val isEmpty: Boolean
        get() = trackGainDb == null && albumGainDb == null && trackPeak == null && albumPeak == null
}

/**
 * M6 ReplayGain: reads gain/peak tags from local files and turns them into
 * a linear volume factor. Supports ID3v2 TXXX frames (MP3) and Vorbis
 * comments (FLAC); MP4 SoundCheck and other schemes are out of scope and
 * read as absent — never invented. Only the file head is ever read.
 */
object ReplayGain {

    /** Head bytes read per file; tags live at the start of MP3/FLAC. */
    const val MAX_SCAN_BYTES = 262_144

    /**
     * Reads ReplayGain tags from [uriString]. Blocking file IO — callers
     * must run it off the main thread. Null when absent or unreadable.
     * Never throws.
     */
    fun readTags(resolver: ContentResolver, uriString: String): ReplayGainTags? {
        return try {
            val head = readHead(resolver, uriString) ?: return null
            val tags = when {
                head.startsWith("ID3".toAscii()) -> parseId3v2(head)
                head.startsWith("fLaC".toAscii()) -> parseFlac(head)
                else -> null
            }
            tags?.takeUnless { it.isEmpty }
        } catch (_: Exception) {
            null
        }
    }

    private fun readHead(resolver: ContentResolver, uriString: String): ByteArray? {
        val stream = resolver.openInputStream(Uri.parse(uriString)) ?: return null
        return try {
            val buffer = ByteArray(MAX_SCAN_BYTES)
            var filled = 0
            while (filled < buffer.size) {
                val read = stream.read(buffer, filled, buffer.size - filled)
                if (read <= 0) break
                filled += read
            }
            if (filled == 0) null else buffer.copyOf(filled)
        } finally {
            try {
                stream.close()
            } catch (_: Exception) {
                // Best effort; the read already finished.
            }
        }
    }

    /**
     * Parses an ID3v2 tag (v2.3 and v2.4) for TXXX ReplayGain frames.
     * Pure; null when the buffer holds no tag or no RG frames.
     */
    fun parseId3v2(head: ByteArray): ReplayGainTags? {
        if (head.size < 10 || !head.startsWith("ID3".toAscii())) return null
        val major = head[3].toInt() and 0xFF
        if (major != 3 && major != 4) return null
        val v24 = major == 4
        val flags = head[5].toInt() and 0xFF
        val tagSize = synchsafe(head, 6) ?: return null
        var body = head.copyOfRange(10, minOf(head.size, 10 + tagSize))
        if (flags and 0x80 != 0) body = deunsync(body)
        var offset = 0
        if (flags and 0x40 != 0) {
            // Extended header: v2.4 size is the synchsafe total; v2.3 is a
            // plain int excluding its own four size bytes.
            val extSize = if (v24) synchsafe(body, 0) else int32(body, 0)
            if (extSize == null || extSize < 0) return null
            offset = extSize + if (v24) 0 else 4
            if (offset > body.size) return null
        }
        var trackGain: Float? = null
        var albumGain: Float? = null
        var trackPeak: Float? = null
        var albumPeak: Float? = null
        while (offset + 10 <= body.size) {
            val id = body.copyOfRange(offset, offset + 4).toAsciiString()
            if (id[0] == '\u0000') break // Padding; end of frames.
            val frameSize = if (v24) synchsafe(body, offset + 4) else int32(body, offset + 4)
            if (frameSize == null || frameSize <= 0 || offset + 10 + frameSize > body.size) break
            if (id == "TXXX") {
                val frame = body.copyOfRange(offset + 10, offset + 10 + frameSize)
                readTxxx(frame)?.let { (description, value) ->
                    when (description.lowercase()) {
                        "replaygain_track_gain" -> trackGain = parseGainDb(value) ?: trackGain
                        "replaygain_album_gain" -> albumGain = parseGainDb(value) ?: albumGain
                        "replaygain_track_peak" -> trackPeak = value.toFloatOrNull() ?: trackPeak
                        "replaygain_album_peak" -> albumPeak = value.toFloatOrNull() ?: albumPeak
                    }
                }
            }
            offset += 10 + frameSize
        }
        return ReplayGainTags(trackGain, albumGain, trackPeak, albumPeak).takeUnless { it.isEmpty }
    }

    /** Splits a TXXX frame into (description, value) per its encoding byte. */
    private fun readTxxx(frame: ByteArray): Pair<String, String>? {
        if (frame.isEmpty()) return null
        val encoding = frame[0].toInt() and 0xFF
        val (charset, wide) = when (encoding) {
            0 -> Pair(Charsets.ISO_8859_1, false)
            1 -> Pair(Charsets.UTF_16, true)
            2 -> Pair(Charsets.UTF_16BE, true)
            3 -> Pair(Charsets.UTF_8, false)
            else -> return null
        }
        val text = frame.copyOfRange(1, frame.size)
        val split = if (wide) {
            // UTF-16 strings terminate on a double zero.
            var i = 0
            while (i + 1 < text.size && !(text[i] == 0.toByte() && text[i + 1] == 0.toByte())) i += 2
            if (i + 1 >= text.size) return null
            i
        } else {
            val i = text.indexOf(0.toByte())
            if (i < 0) return null
            i
        }
        val terminator = if (wide) 2 else 1
        val description = text.copyOfRange(0, split).toString(charset).trim { it <= ' ' || it == '\uFEFF' }
        val value = text.copyOfRange(split + terminator, text.size).toString(charset)
            .trim { it <= ' ' || it == '\u0000' || it == '\uFEFF' }
        return Pair(description, value)
    }

    /**
     * Parses a FLAC stream's metadata blocks for the Vorbis comment block.
     * Pure; null when the buffer holds no stream or comment block.
     */
    fun parseFlac(head: ByteArray): ReplayGainTags? {
        if (head.size < 4 || !head.startsWith("fLaC".toAscii())) return null
        var offset = 4
        while (offset + 4 <= head.size) {
            val header = head[offset].toInt() and 0xFF
            val last = header and 0x80 != 0
            val type = header and 0x7F
            val length = int24(head, offset + 1) ?: return null
            if (offset + 4 + length > head.size) return null
            if (type == 4) {
                return parseVorbisComment(head.copyOfRange(offset + 4, offset + 4 + length))
            }
            offset += 4 + length
            if (last) break
        }
        return null
    }

    /**
     * Parses a Vorbis comment payload (FLAC block type 4 / Ogg comments).
     * Pure; null when malformed or holding no RG entries.
     */
    fun parseVorbisComment(payload: ByteArray): ReplayGainTags? {
        var offset = 0
        fun readChunk(): ByteArray? {
            if (offset + 4 > payload.size) return null
            val length = int32le(payload, offset) ?: return null
            if (length < 0 || offset + 4 + length > payload.size) return null
            offset += 4
            val chunk = payload.copyOfRange(offset, offset + length)
            offset += length
            return chunk
        }
        readChunk() ?: return null // Vendor string; skipped.
        if (offset + 4 > payload.size) return null
        val count = int32le(payload, offset) ?: return null
        if (count < 0 || count > 4096) return null
        offset += 4
        var trackGain: Float? = null
        var albumGain: Float? = null
        var trackPeak: Float? = null
        var albumPeak: Float? = null
        repeat(count) {
            val entry = readChunk()?.toString(Charsets.UTF_8) ?: return null
            val split = entry.indexOf('=')
            if (split > 0) {
                val key = entry.substring(0, split).lowercase()
                val value = entry.substring(split + 1)
                when (key) {
                    "replaygain_track_gain" -> trackGain = parseGainDb(value) ?: trackGain
                    "replaygain_album_gain" -> albumGain = parseGainDb(value) ?: albumGain
                    "replaygain_track_peak" -> trackPeak = value.toFloatOrNull() ?: trackPeak
                    "replaygain_album_peak" -> albumPeak = value.toFloatOrNull() ?: albumPeak
                }
            }
        }
        return ReplayGainTags(trackGain, albumGain, trackPeak, albumPeak).takeUnless { it.isEmpty }
    }

    /** Parses "-6.54 dB" (suffix optional) into dB, clamped to ±30. */
    fun parseGainDb(raw: String): Float? {
        val cleaned = raw.trim().removeSuffix("dB").removeSuffix("db").trim()
        if (cleaned.isEmpty()) return null
        return cleaned.toFloatOrNull()?.coerceIn(-30f, 30f)
    }

    /**
     * Linear gain for [mode]. Missing tags mean unity — nothing is ever
     * invented. Gains that would clip against the stored peak are pulled
     * back to the peak ceiling. Pure.
     */
    fun gainFor(tags: ReplayGainTags?, mode: ReplayGainMode): Float {
        if (tags == null || mode == ReplayGainMode.OFF) return 1f
        val gainDb = if (mode == ReplayGainMode.TRACK) tags.trackGainDb else tags.albumGainDb
        val peak = if (mode == ReplayGainMode.TRACK) tags.trackPeak else tags.albumPeak
        if (gainDb == null) return 1f
        var gain = 10f.pow(gainDb / 20f)
        val ceiling = peak?.takeIf { it > 0f }?.let { 1f / it }
        if (ceiling != null && gain > ceiling) gain = ceiling
        return gain.coerceIn(0f, 4f)
    }

    /** Preamp × ReplayGain × fade level as one player volume. Pure. */
    fun combinedGain(preampDb: Float, replayGain: Float, fadeLevel: Float): Float {
        return (StudioEffects.preampToGain(preampDb) * replayGain * fadeLevel).coerceIn(0f, 4f)
    }

    // --- Byte helpers (all bounds-checked, null on overrun) ------------------

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        return prefix.indices.all { this[it] == prefix[it] }
    }

    private fun String.toAscii(): ByteArray = toByteArray(Charsets.US_ASCII)

    private fun ByteArray.toAsciiString(): String = toString(Charsets.US_ASCII)

    /** 4-byte synchsafe int (ID3v2.4): 7 bits per byte, MSB must be zero. */
    private fun synchsafe(data: ByteArray, offset: Int): Int? {
        if (offset + 4 > data.size) return null
        var value = 0
        for (i in 0 until 4) {
            val byte = data[offset + i].toInt() and 0xFF
            if (byte and 0x80 != 0) return null
            value = (value shl 7) or byte
        }
        return value
    }

    private fun int32(data: ByteArray, offset: Int): Int? {
        if (offset + 4 > data.size) return null
        return ((data[offset].toInt() and 0xFF) shl 24) or
            ((data[offset + 1].toInt() and 0xFF) shl 16) or
            ((data[offset + 2].toInt() and 0xFF) shl 8) or
            (data[offset + 3].toInt() and 0xFF)
    }

    private fun int24(data: ByteArray, offset: Int): Int? {
        if (offset + 3 > data.size) return null
        return ((data[offset].toInt() and 0xFF) shl 16) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            (data[offset + 2].toInt() and 0xFF)
    }

    private fun int32le(data: ByteArray, offset: Int): Int? {
        if (offset + 4 > data.size) return null
        return (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            ((data[offset + 2].toInt() and 0xFF) shl 16) or
            ((data[offset + 3].toInt() and 0xFF) shl 24)
    }

    /** Reverses ID3 unsynchronisation (0xFF 0x00 -> 0xFF). */
    private fun deunsync(body: ByteArray): ByteArray {
        val out = ByteArray(body.size)
        var read = 0
        var written = 0
        while (read < body.size) {
            val byte = body[read++]
            out[written++] = byte
            if (byte == 0xFF.toByte() && read < body.size && body[read] == 0.toByte()) read++
        }
        return out.copyOf(written)
    }
}
