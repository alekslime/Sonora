package com.sonora.playback

/**
 * M6 format readout for the Studio output section: codec, sample rate,
 * channels and bitrate of the playing track. Unknown fields stay null and
 * are skipped in [displayLine].
 */
data class TrackFormat(
    val codec: String,
    val bitrateKbps: Int? = null,
    val sampleRateKhz: Float? = null,
    val channels: String? = null,
) {
    val displayLine: String
        get() = listOfNotNull(
            codec,
            sampleRateKhz?.let { "${trimFloat(it)} kHz" },
            channels,
            bitrateKbps?.let { "$it kbps" },
        ).joinToString(" · ")

    companion object {
        /**
         * Builds a readout from ExoPlayer track values. Non-positive
         * numbers mean unknown (Format.NO_VALUE). Pure and tested.
         */
        fun describe(
            mime: String?,
            bitrateBps: Int,
            sampleRateHz: Int,
            channelCount: Int,
        ): TrackFormat {
            return TrackFormat(
                codec = codecName(mime),
                bitrateKbps = bitrateBps.takeIf { it > 0 }?.div(1000),
                sampleRateKhz = sampleRateHz.takeIf { it > 0 }?.div(1000f),
                channels = channelName(channelCount),
            )
        }

        private fun codecName(mime: String?): String {
            return when (mime?.lowercase()) {
                "audio/flac" -> "FLAC"
                "audio/mpeg", "audio/mp3" -> "MP3"
                "audio/mp4a-latm", "audio/aac", "audio/mp4" -> "AAC"
                "audio/alac" -> "ALAC"
                "audio/vorbis" -> "Vorbis"
                "audio/opus" -> "Opus"
                "audio/wav", "audio/x-wav", "audio/raw" -> "WAV"
                null -> "Audio"
                else -> mime.substringAfter("/").uppercase().ifBlank { "Audio" }
            }
        }

        private fun channelName(count: Int): String? {
            return when (count) {
                1 -> "Mono"
                2 -> "Stereo"
                6 -> "5.1"
                8 -> "7.1"
                else -> if (count > 0) "$count ch" else null
            }
        }

        private fun trimFloat(value: Float): String {
            return if (value == value.toInt().toFloat()) {
                value.toInt().toString()
            } else {
                "%.1f".format(value)
            }
        }
    }
}
