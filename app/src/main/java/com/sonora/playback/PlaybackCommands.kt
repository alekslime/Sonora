package com.sonora.playback

/**
 * M6 custom commands from UI to [SonoraPlaybackService], sent through the
 * MediaController. Standard transport (play/pause/seek) uses the platform
 * controller calls; everything SONORA-specific goes through these.
 */
object PlaybackCommands {
    /** Cue one track: [EXTRA_INDEX] + [EXTRA_URI]. Keeps playing state. */
    const val CUE = "sonora.cue"

    /** Load a queue: [EXTRA_URIS] + [EXTRA_INDEX] + [EXTRA_SINGLE] + [EXTRA_POSITION]. */
    const val QUEUE = "sonora.queue"

    /** Full Studio config snapshot (EQ, preamp, RG, gapless, crossfade). */
    const val STUDIO = "sonora.studio"

    /** Mirror repeat-one into the engine: [EXTRA_ENABLED]. */
    const val REPEAT_ONE = "sonora.repeat_one"

    const val EXTRA_INDEX = "index"
    const val EXTRA_URI = "uri"
    const val EXTRA_URIS = "uris"
    const val EXTRA_TITLES = "titles"
    const val EXTRA_ARTISTS = "artists"
    const val EXTRA_SINGLE = "single"
    const val EXTRA_POSITION = "position"
    const val EXTRA_ENABLED = "enabled"
    const val EXTRA_EQ_ENABLED = "eq_enabled"
    const val EXTRA_BANDS = "bands"
    const val EXTRA_PREAMP = "preamp"
    const val EXTRA_RG = "rg"
    const val EXTRA_GAPLESS = "gapless"
    const val EXTRA_XFADE_ENABLED = "xfade_enabled"
    const val EXTRA_XFADE_SECS = "xfade_secs"
}
