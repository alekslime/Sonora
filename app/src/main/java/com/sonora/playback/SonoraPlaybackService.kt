package com.sonora.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.compose.runtime.snapshotFlow
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.sonora.MainActivity
import com.sonora.R
import com.sonora.data.PlaybackSnapshot
import com.sonora.data.SonoraPrefs
import com.sonora.liquid.SpectrumAnalyzer
import com.sonora.nowplaying.EngineHandle
import com.sonora.nowplaying.NowPlayingState
import com.sonora.nowplaying.RepeatMode
import com.sonora.studio.AudioStudioConfig
import com.sonora.studio.ReplayGain
import com.sonora.studio.ReplayGainTags
import com.sonora.studio.StudioEffects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * M6 background playback. Owns both players (front for output, back as the
 * crossfade partner), the media session behind the notification and
 * lock-screen, the Studio EQ on each audio session, the FFT analyzer, and
 * ReplayGain application. Transport policy stays in the orchestrator's
 * [NowPlayingState]: the service executes cue/queue commands and reports
 * engine truth through [PlaybackHub].
 */
class SonoraPlaybackService : MediaSessionService() {

    companion object {
        const val ACTION_START = "sonora.action.START"
        private const val POLL_MS = 100L
        private const val SNAPSHOT_TICKS = 100
        private const val ART_TIMEOUT_MS = 400L
        private const val RG_CACHE_CAP = 32
        private const val ART_CACHE_CAP = 8
    }

    private lateinit var session: MediaSession
    private lateinit var front: ExoPlayer
    private lateinit var back: ExoPlayer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var studio = AudioStudioConfig()
    private var queueUris: List<String> = emptyList()
    private var queueTitles: List<String> = emptyList()
    private var queueArtists: List<String> = emptyList()
    private var singleQueue = false
    private var queueGeneration = 0

    private var rgFront = 1f
    private var rgBack = 1f
    private var frontUri: String? = null
    private var backUri: String? = null
    private val rgCache = LinkedHashMap<String, ReplayGainTags?>(RG_CACHE_CAP, 0.75f, true)
    private val artCache = LinkedHashMap<String, ByteArray>(ART_CACHE_CAP, 0.75f, true)

    private var effectsFront = StudioEffects()
    private var effectsBack = StudioEffects()
    private val analyzer = SpectrumAnalyzer()

    private var fade: FadeRun? = null

    private data class FadeRun(val targetIndex: Int, val targetUri: String, val fadeMs: Long)

    // --- Lifecycle ----------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        front = buildPlayer()
        back = buildPlayer()
        front.addListener(playerListener(front))
        back.addListener(playerListener(back))
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, front)
            .setId("sonora")
            .setCallback(sessionCallback)
            .setSessionActivity(contentIntent)
            .build()
        val provider = DefaultMediaNotificationProvider(this)
        provider.setSmallIcon(R.drawable.ic_notification)
        setMediaNotificationProvider(provider)
        scope.launch { ensureStudio() }
        scope.launch {
            snapshotFlow { analyzer.bands }.collect { PlaybackHub.publishBands(it) }
        }
        scope.launch {
            snapshotFlow { analyzer.signal }.collect { it?.let(PlaybackHub::publishVizSignal) }
        }
        scope.launch {
            snapshotFlow { analyzer.active }.collect { PlaybackHub.publishVizActive(it) }
        }
        scope.launch {
            while (isActive) {
                delay(POLL_MS)
                poll()
            }
        }
    }

    private fun buildPlayer(): ExoPlayer {
        return ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
                shuffleModeEnabled = false
                volume = 1f
            }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = session

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // A null intent means the system restarted us: resume only when
        // listening was actually interrupted, otherwise stand down.
        if (intent == null) scope.launch { restoreSnapshot() }
        return START_STICKY
    }

    private var studioLoaded = false

    private suspend fun ensureStudio() {
        if (studioLoaded) return
        studioLoaded = true
        studio = SonoraPrefs.loadStudio(applicationContext)
        applyStudioToPlayers()
        applyVolumes()
    }

    /** M7 sticky resume: rebuilds the interrupted session headlessly. */
    private suspend fun restoreSnapshot() {
        ensureStudio()
        val snapshot = SonoraPrefs.loadSnapshot(applicationContext)
        val album = snapshot?.toAlbum()
        if (snapshot == null || album == null || !album.isReal || !snapshot.wasPlaying) {
            stopSelf()
            return
        }
        val restored = NowPlayingState(album, snapshot.trackIndex).apply {
            restore(
                snapshot.trackIndex,
                snapshot.positionMs,
                playing = true,
                snapshot.shuffle,
                if (snapshot.repeatOne) RepeatMode.ONE else RepeatMode.OFF,
            )
            engine = directEngine
        }
        PlaybackOrchestrator.attachRestored(restored)
        handleQueueLoad(
            uris = snapshot.trackUris,
            titles = snapshot.trackTitles,
            artists = List(snapshot.trackUris.size) { album.artist },
            index = restored.trackIndex,
            singleQueue = snapshot.shuffle || !studio.gapless,
            positionMs = restored.positionMs,
        )
        front.repeatMode = if (snapshot.repeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        restored.play()
    }

    /** Direct in-process engine for restored sessions (no binder hop). */
    private val directEngine = object : EngineHandle {
        override fun enginePlay(index: Int, uri: String) = handleCueTrack(index, uri)

        override fun engineResume() {
            if (front.playbackState == Player.STATE_ENDED) {
                front.seekTo(front.currentMediaItemIndex, 0L)
            }
            front.play()
        }

        override fun enginePause() = front.pause()

        override fun engineSeekTo(positionMs: Long) {
            front.seekTo(positionMs.coerceAtLeast(0L))
            PlaybackHub.publishPosition(positionMs.coerceAtLeast(0L))
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!front.isPlaying && !back.isPlaying) stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        analyzer.release()
        effectsFront.release()
        effectsBack.release()
        session.release()
        front.release()
        back.release()
        super.onDestroy()
    }

    // --- Session callbacks --------------------------------------------------

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
                .buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .build()
            val sessionCommands = SessionCommands.EMPTY
                .buildUpon()
                .add(SessionCommand(PlaybackCommands.CUE, Bundle.EMPTY))
                .add(SessionCommand(PlaybackCommands.QUEUE, Bundle.EMPTY))
                .add(SessionCommand(PlaybackCommands.STUDIO, Bundle.EMPTY))
                .add(SessionCommand(PlaybackCommands.REPEAT_ONE, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.accept(sessionCommands, playerCommands)
        }

        // Interception (not gating): next/prev execute state-machine
        // policy instead of raw player seeks. The return is a result code
        // (SUCCESS forwards, anything else drops); the deprecation points
        // at setAvailableCommands, which can only remove buttons, not
        // redirect them.
        @Suppress("DEPRECATION")
        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int,
        ): Int {
            // Notification/lock-screen transport runs through the state
            // machine, never around it.
            return when (playerCommand) {
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                -> {
                    PlaybackOrchestrator.state?.next()
                    SessionResult.RESULT_ERROR_NOT_SUPPORTED
                }
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                -> {
                    PlaybackOrchestrator.state?.prev()
                    SessionResult.RESULT_ERROR_NOT_SUPPORTED
                }
                else -> SessionResult.RESULT_SUCCESS
            }
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            command: SessionCommand,
            args: Bundle,
        ): com.google.common.util.concurrent.ListenableFuture<SessionResult> {
            when (command.customAction) {
                PlaybackCommands.CUE -> handleCue(args)
                PlaybackCommands.QUEUE -> handleQueue(args)
                PlaybackCommands.STUDIO -> handleStudio(args)
                PlaybackCommands.REPEAT_ONE -> {
                    front.repeatMode = if (args.getBoolean(PlaybackCommands.EXTRA_ENABLED)) {
                        Player.REPEAT_MODE_ONE
                    } else {
                        Player.REPEAT_MODE_OFF
                    }
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    // --- Commands -----------------------------------------------------------

    private fun handleQueue(args: Bundle) {
        val uris = args.getStringArrayList(PlaybackCommands.EXTRA_URIS)?.toList()
        if (uris.isNullOrEmpty()) return
        handleQueueLoad(
            uris = uris,
            titles = args.getStringArrayList(PlaybackCommands.EXTRA_TITLES)?.toList() ?: emptyList(),
            artists = args.getStringArrayList(PlaybackCommands.EXTRA_ARTISTS)?.toList() ?: emptyList(),
            index = args.getInt(PlaybackCommands.EXTRA_INDEX),
            singleQueue = args.getBoolean(PlaybackCommands.EXTRA_SINGLE),
            positionMs = args.getLong(PlaybackCommands.EXTRA_POSITION),
        )
    }

    private fun handleQueueLoad(
        uris: List<String>,
        titles: List<String>,
        artists: List<String>,
        index: Int,
        singleQueue: Boolean,
        positionMs: Long,
    ) {
        cancelFade()
        queueUris = uris
        queueTitles = titles.takeIf { it.size == uris.size } ?: List(uris.size) { "" }
        queueArtists = artists.takeIf { it.size == uris.size } ?: List(uris.size) { "" }
        this.singleQueue = singleQueue
        val safeIndex = index.coerceIn(uris.indices)
        val position = positionMs.coerceAtLeast(0L)
        val generation = ++queueGeneration
        PlaybackHub.reset()
        if (singleQueue) {
            front.setMediaItem(buildItem(uris[safeIndex], safeIndex))
        } else {
            front.setMediaItems(uris.mapIndexed { i, uri -> buildItem(uri, i) })
        }
        front.seekToDefaultPosition(if (singleQueue) 0 else safeIndex)
        if (position > 0L) front.seekTo(position)
        frontUri = uris[safeIndex]
        PlaybackHub.publishPosition(position)
        applyVolumes()
        // Prepare waits briefly for cover art so the notification is
        // complete from the first frame; playback never waits longer.
        scope.launch(Dispatchers.IO) {
            val bytes = artFor(uris[safeIndex])
            withContext(Dispatchers.Main.immediate) {
                if (generation != queueGeneration) return@withContext
                // Never enrich a playing item: replacing it would restart
                // the track. Slow art simply misses this track's artwork.
                if (bytes != null && !front.isPlaying) enrichItem(safeIndex, bytes)
                front.prepare()
            }
        }
        scope.launch(Dispatchers.Main.immediate) {
            delay(ART_TIMEOUT_MS)
            if (generation == queueGeneration) front.prepare()
        }
        resolveReplayGain(uris[safeIndex]) { gain ->
            rgFront = gain
            applyVolumes()
        }
        publishFormat()
        prefetchNextArt(safeIndex)
        persistSnapshot()
    }

    private fun handleCue(args: Bundle) {
        val index = args.getInt(PlaybackCommands.EXTRA_INDEX)
        val uri = args.getString(PlaybackCommands.EXTRA_URI) ?: return
        handleCueTrack(index, uri)
    }

    private fun handleCueTrack(index: Int, uri: String) {
        cancelFade()
        val generation = ++queueGeneration
        if (singleQueue) {
            val playing = front.playWhenReady
            front.setMediaItem(buildItem(uri, index))
            frontUri = uri
            scope.launch(Dispatchers.IO) {
                val bytes = artFor(uri)
                withContext(Dispatchers.Main.immediate) {
                    if (generation != queueGeneration) return@withContext
                    if (bytes != null && !front.isPlaying) enrichItem(index, bytes)
                    front.prepare()
                    if (playing) front.play()
                }
            }
            scope.launch(Dispatchers.Main.immediate) {
                delay(ART_TIMEOUT_MS)
                if (generation == queueGeneration) {
                    front.prepare()
                    if (playing) front.play()
                }
            }
        } else {
            front.seekToDefaultPosition(index.coerceIn(0, (front.mediaItemCount - 1).coerceAtLeast(0)))
            frontUri = queueUris.getOrNull(index) ?: uri
        }
        applyVolumes()
        PlaybackHub.publishPosition(0L)
        resolveReplayGain(uri) { gain ->
            rgFront = gain
            applyVolumes()
        }
        publishFormat()
        prefetchNextArt(index)
        persistSnapshot()
    }

    /** Captures the resumable session; skipped when nothing is playing. */
    private fun persistSnapshot() {
        val state = PlaybackOrchestrator.state ?: return
        val snapshot = PlaybackSnapshot.from(
            state.album,
            state.trackIndex,
            PlaybackHub.positionMs.value,
            state.isPlaying,
            state.shuffle,
            state.repeatMode == RepeatMode.ONE,
        ) ?: return
        scope.launch { SonoraPrefs.saveSnapshot(applicationContext, snapshot) }
    }

    private fun handleStudio(args: Bundle) {
        val bands = args.getFloatArray(PlaybackCommands.EXTRA_BANDS)?.toList()
        studio = AudioStudioConfig(
            eqEnabled = args.getBoolean(PlaybackCommands.EXTRA_EQ_ENABLED, studio.eqEnabled),
            bands = bands?.takeIf { it.size == AudioStudioConfig.BAND_COUNT } ?: studio.bands,
            presetName = studio.presetName,
            preampDb = args.getFloat(PlaybackCommands.EXTRA_PREAMP, studio.preampDb),
            replayGain = SonoraPrefs.decodeReplayGain(args.getString(PlaybackCommands.EXTRA_RG)),
            gapless = args.getBoolean(PlaybackCommands.EXTRA_GAPLESS, studio.gapless),
            crossfadeEnabled = args.getBoolean(
                PlaybackCommands.EXTRA_XFADE_ENABLED, studio.crossfadeEnabled,
            ),
            crossfadeSecs = args.getInt(PlaybackCommands.EXTRA_XFADE_SECS, studio.crossfadeSecs)
                .coerceIn(AudioStudioConfig.MIN_CROSSFADE_SECS, AudioStudioConfig.MAX_CROSSFADE_SECS),
        )
        applyStudioToPlayers()
        frontUri?.let { uri -> resolveReplayGain(uri) { rgFront = it; applyVolumes() } }
        backUri?.let { uri -> resolveReplayGain(uri) { rgBack = it; applyVolumes() } }
        applyVolumes()
    }

    // --- Player events --------------------------------------------------------
    //
    // Each player gets its own listener bound to its instance; the
    // front/back role is resolved per callback because fades swap roles.

    private fun playerListener(self: ExoPlayer) = object : Player.Listener {
        private fun isFront() = self === front

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            publishPlaying()
            if (isFront()) {
                if (fade != null) {
                    if (isPlaying) back.play() else back.pause()
                }
                persistSnapshot()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (!isFront()) {
                cancelFade()
                return
            }
            // Unplayable track: let policy advance past it. A fully dead
            // library walks to the tail and stops on its own.
            cancelFade()
            val deadIndex = if (singleQueue) currentAlbumIndex() else front.currentMediaItemIndex
            PlaybackHub.emitSkipped(queueTitles.getOrNull(deadIndex) ?: "Track")
            PlaybackHub.emitEnded()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (!isFront() || fade != null) return
            val queueIndex = front.currentMediaItemIndex
            val albumIndex = if (singleQueue) currentAlbumIndex() else queueIndex
            frontUri = queueUris.getOrNull(albumIndex)
            PlaybackHub.publishPosition(0L)
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                PlaybackHub.emitTransition(albumIndex)
            }
            frontUri?.let { uri -> resolveReplayGain(uri) { rgFront = it; applyVolumes() } }
            publishFormat()
            prefetchNextArt(albumIndex)
            persistSnapshot()
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            if (isFront()) {
                effectsFront.attach(audioSessionId)
                effectsFront.apply(studio)
                PlaybackHub.publishAudioSession(audioSessionId)
                analyzer.attach(audioSessionId)
            } else {
                effectsBack.attach(audioSessionId)
                effectsBack.apply(studio)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_ENDED) return
            if (!isFront() && fade != null) {
                finishFade()
            } else if (isFront() && fade == null) {
                PlaybackHub.emitEnded()
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            if (isFront()) publishFormat()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (isFront() && reason == Player.DISCONTINUITY_REASON_SEEK) {
                cancelFade()
            }
        }
    }

    private fun publishPlaying() {
        PlaybackHub.publishPlaying(front.isPlaying || (fade != null && back.isPlaying))
    }

    private fun currentAlbumIndex(): Int {
        return PlaybackOrchestrator.state?.trackIndex ?: 0
    }

    // --- Crossfade ------------------------------------------------------------

    private var pollTicks = 0

    private fun poll() {
        if (front.isPlaying) PlaybackHub.publishPosition(front.currentPosition)
        publishPlaying()
        maybeStartFade()
        tickFade()
        val tick = ++pollTicks
        // Analyzer watch: a failed attach or stalled captures retry here.
        if (tick % 10 == 0) analyzer.ensureAttached(front.audioSessionId, SystemClock.elapsedRealtime())
        // Position freshness for resume: a kill loses at most this window
        // on top of the event-driven saves.
        if (tick % SNAPSHOT_TICKS == 0 && front.isPlaying) persistSnapshot()
    }

    private fun maybeStartFade() {
        if (fade != null || !front.isPlaying) return
        if (front.repeatMode == Player.REPEAT_MODE_ONE) return
        val duration = front.duration
        if (duration <= 0L) return
        val state = PlaybackOrchestrator.state ?: return
        val target = state.previewNext() ?: return
        val gaplessBoundary = studio.gapless && !state.shuffle && target == state.trackIndex + 1
        val fadeMs = AudioStudioConfig.effectiveCrossfadeMs(studio, gaplessBoundary)
        if (fadeMs <= 0L) return
        val remaining = duration - front.currentPosition
        if (remaining > fadeMs || remaining <= 0L) return
        val uri = queueUris.getOrNull(target) ?: return
        fade = FadeRun(target, uri, fadeMs)
        backUri = uri
        back.setMediaItem(buildItem(uri, target))
        artCache[uri]?.let { enrichPlayerItem(back, 0, it) }
        back.seekTo(0L)
        // Silence before the first tick: prepare+play would otherwise
        // blip the incoming track at full volume for up to one poll.
        back.volume = 0f
        back.prepare()
        back.play()
        resolveReplayGain(uri) { gain ->
            rgBack = gain
            applyVolumes()
        }
    }

    private fun tickFade() {
        val run = fade ?: return
        val duration = front.duration
        val remaining = if (duration > 0L) duration - front.currentPosition else 0L
        val t = 1f - (remaining.toFloat() / run.fadeMs.toFloat())
        if (t >= 1f || remaining <= 0L || front.playbackState == Player.STATE_ENDED) {
            finishFade()
            return
        }
        val (outgoing, incoming) = Crossfade.fadeGains(t)
        front.volume = ReplayGain.combinedGain(studio.preampDb, rgFront, outgoing)
        back.volume = ReplayGain.combinedGain(studio.preampDb, rgBack, incoming)
    }

    private fun finishFade() {
        val run = fade ?: return
        fade = null
        backUri = null
        val outgoing = front
        val repeatOne = outgoing.repeatMode == Player.REPEAT_MODE_ONE
        front = back
        back = outgoing
        val outgoingEffects = effectsFront
        effectsFront = effectsBack
        effectsBack = outgoingEffects
        front.repeatMode = if (repeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        back.stop()
        back.volume = 0f
        session.setPlayer(front)
        frontUri = run.targetUri
        rgFront = rgBack
        rgBack = 1f
        applyVolumes()
        PlaybackHub.publishPosition(front.currentPosition)
        PlaybackHub.publishAudioSession(front.audioSessionId)
        analyzer.attach(front.audioSessionId)
        PlaybackHub.emitTransition(run.targetIndex)
        publishFormat()
        prefetchNextArt(run.targetIndex)
        persistSnapshot()
    }

    private fun cancelFade() {
        if (fade == null) return
        fade = null
        backUri = null
        back.stop()
        applyVolumes()
        publishPlaying()
    }

    // --- Gains, format, artwork -------------------------------------------------

    private fun applyStudioToPlayers() {
        effectsFront.apply(studio)
        effectsBack.apply(studio)
    }

    private fun applyVolumes() {
        if (fade == null) {
            front.volume = ReplayGain.combinedGain(studio.preampDb, rgFront, 1f)
            back.volume = 0f
        }
    }

    private fun resolveReplayGain(uri: String, apply: (Float) -> Unit) {
        if (rgCache.containsKey(uri)) {
            apply(ReplayGain.gainFor(rgCache[uri], studio.replayGain))
            return
        }
        scope.launch(Dispatchers.IO) {
            val tags = ReplayGain.readTags(applicationContext.contentResolver, uri)
            withContext(Dispatchers.Main.immediate) {
                rgCache[uri] = tags
                while (rgCache.size > RG_CACHE_CAP) {
                    rgCache.remove(rgCache.keys.first())
                }
                apply(ReplayGain.gainFor(tags, studio.replayGain))
            }
        }
    }

    private fun publishFormat() {
        PlaybackHub.publishFormat(audioFormatOf(front))
    }

    private fun audioFormatOf(player: ExoPlayer): TrackFormat? {
        return try {
            val group = player.currentTracks.groups
                .firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.length > 0 }
                ?: return null
            val format = group.getTrackFormat(0)
            TrackFormat.describe(
                format.sampleMimeType,
                format.bitrate,
                format.sampleRate,
                format.channelCount,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun buildItem(uri: String, queueIndex: Int): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(queueTitles.getOrNull(queueIndex)?.ifBlank { null } ?: "Unknown title")
            .setArtist(queueArtists.getOrNull(queueIndex)?.ifBlank { null } ?: "Unknown artist")
            .setDescription(
                // Lock-screen description reads better with album context.
                listOfNotNull(
                    queueArtists.getOrNull(queueIndex)?.ifBlank { null },
                    queueTitles.getOrNull(queueIndex)?.ifBlank { null },
                ).joinToString("  •  ").ifBlank { null },
            )
            .build()
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun enrichItem(queueIndex: Int, art: ByteArray) {
        val position = if (singleQueue) 0 else queueIndex
        if (position >= front.mediaItemCount) return
        enrichPlayerItem(front, position, art)
    }

    private fun enrichPlayerItem(player: ExoPlayer, position: Int, art: ByteArray) {
        try {
            val current = player.getMediaItemAt(position)
            val enriched = current.buildUpon()
                .setMediaMetadata(
                    current.mediaMetadata.buildUpon().setArtworkData(art).build(),
                )
                .build()
            player.replaceMediaItem(position, enriched)
        } catch (_: Exception) {
            // Art is decorative; playback continues without it.
        }
    }

    private fun artFor(uri: String): ByteArray? {
        artCache[uri]?.let { return it }
        val bytes = embeddedArt(uri) ?: return null
        artCache[uri] = bytes
        while (artCache.size > ART_CACHE_CAP) {
            artCache.remove(artCache.keys.first())
        }
        return bytes
    }

    private fun embeddedArt(uri: String): ByteArray? {
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            retriever.setDataSource(applicationContext, android.net.Uri.parse(uri))
            retriever.embeddedPicture
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
                // Best effort.
            }
        }
    }

    private fun prefetchNextArt(fromIndex: Int) {
        val state = PlaybackOrchestrator.state
        val next = (
            if (state != null && (state.shuffle || singleQueue)) {
                state.previewNext()
            } else if (!singleQueue && fromIndex + 1 < queueUris.size) {
                fromIndex + 1
            } else {
                state?.previewNext()
            }
            ) ?: return
        val uri = queueUris.getOrNull(next) ?: return
        if (artCache.containsKey(uri)) {
            if (!singleQueue && next != currentAlbumIndex()) enrichItem(next, artCache[uri]!!)
            return
        }
        scope.launch(Dispatchers.IO) {
            val bytes = artFor(uri) ?: return@launch
            withContext(Dispatchers.Main.immediate) {
                if (!singleQueue && next != currentAlbumIndex()) enrichItem(next, bytes)
            }
        }
    }
}
