package com.sonora.nowplaying

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sonora.components.CoverArt
import com.sonora.components.rememberTapTick
import com.sonora.data.Album
import com.sonora.data.averageColor
import com.sonora.data.rememberAlbumArtwork
import com.sonora.data.rememberBackdropArtwork
import com.sonora.data.toMinSec
import com.sonora.liquid.LiquidSettingsSheet
import com.sonora.playback.PlaybackHub
import com.sonora.playback.PlaybackOrchestrator
import com.sonora.playback.ServiceEngine
import com.sonora.playback.TrackFormat
import com.sonora.studio.AudioStudioStore
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.content
import com.sonora.theme.contentInverse
import com.sonora.theme.frost
import com.sonora.theme.rememberSonoraScale
import com.sonora.theme.scaled

private val ArtShape = RoundedCornerShape(28.dp)

/**
 * Now Playing in the Obsidian glass voice: the artwork washes a blurred
 * full-bleed backdrop, transport sits on frosted-glass circles, and the
 * play control is one solid white disc. Shuffle/repeat live in the
 * options sheet, the queue lists the album, and the heart/add pair
 * share the persisted liked-songs state. Real albums play through the
 * M6 background service via the app-scoped orchestrator, so
 * notification and lock-screen stay in sync; demo albums simulate
 * playback with a screen-local state ticker.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun NowPlayingScreen(
    album: Album,
    startTrackIndex: Int,
    autoplay: Boolean,
    onBack: () -> Unit,
    onOpenLiquid: () -> Unit,
    onOpenStudio: () -> Unit,
    onSession: (NowPlayingState) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Live app-scoped session to adopt (mini-player expand). For demo
     * albums with a matching session the position is kept and no
     * restore runs; otherwise a fresh local state starts the visit.
     */
    adoptSession: NowPlayingState? = null,
    /** Canonical artist per track; virtual queues resolve the library. */
    artistFor: (Int) -> String = { album.artist },
    /** Origin album per track; artwork, backdrop and tint follow it. */
    artAlbumFor: (Int) -> Album = { album },
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val colors = LocalSonoraColors.current
    val context = LocalContext.current
    BackHandler(onBack = onBack)

    var persistedTrack by rememberSaveable(album.id, startTrackIndex) { mutableIntStateOf(startTrackIndex) }
    var persistedPosition by rememberSaveable(album.id, startTrackIndex) { mutableLongStateOf(0L) }
    var persistedPlaying by rememberSaveable(album.id, startTrackIndex) { mutableStateOf(autoplay) }
    var persistedShuffle by rememberSaveable(album.id, startTrackIndex) { mutableStateOf(false) }
    var persistedRepeat by rememberSaveable(album.id, startTrackIndex) { mutableStateOf(RepeatMode.OFF) }

    // Real playback joins the orchestrator session (started from Detail,
    // or here after process death); demo keeps screen-local ticker state.
    LaunchedEffect(album.id) {
        if (album.isReal && PlaybackOrchestrator.state?.album?.id != album.id) {
            PlaybackOrchestrator.startAlbum(
                context = context,
                album = album,
                startIndex = persistedTrack,
                positionMs = persistedPosition,
                playing = persistedPlaying,
                shuffle = persistedShuffle,
            )
        }
    }
    val localState = remember(album.id, startTrackIndex) { NowPlayingState(album, startTrackIndex) }
    val orchestrated = PlaybackOrchestrator.state
    val adopted = adoptSession?.takeIf { !album.isReal && it.album.id == album.id }
    val state = if (album.isReal && orchestrated?.album?.id == album.id) {
        orchestrated
    } else {
        adopted ?: localState
    }
    val artAlbum = remember(album.id, state.trackIndex) { artAlbumFor(state.trackIndex) }
    val heroArt = rememberAlbumArtwork(artAlbum)
    val glassTint = remember(heroArt) { heroArt?.let(::averageColor) }
    val engine = remember(album.id) { if (album.isReal) ServiceEngine.get(context) else null }
    // Service stopped alone (swiped away while paused) across a relaunch:
    // restart the session where it stood instead of commanding a ghost.
    val connected = if (engine != null) engine.connected.collectAsState().value else true
    val hadConnection = if (engine != null) engine.hadConnection.collectAsState().value else false
    LaunchedEffect(connected, hadConnection) {
        val current = PlaybackOrchestrator.state
        if (album.isReal && current?.album?.id == album.id && hadConnection && !connected) {
            PlaybackOrchestrator.startAlbum(
                context = context,
                album = album,
                startIndex = current.trackIndex,
                positionMs = current.positionMs,
                playing = current.isPlaying,
                shuffle = current.shuffle,
            )
        }
    }

    LaunchedEffect(state) {
        onSession(state)
        // Fresh demo visits restore the persisted slot; an adopted live
        // session keeps its clock. The demo ticker runs app-scoped so
        // playback survives navigation to the mini-player.
        if (!album.isReal && state === localState) {
            state.restore(
                persistedTrack, persistedPosition, persistedPlaying,
                persistedShuffle, persistedRepeat,
            )
        }
    }
    LaunchedEffect(state.trackIndex, state.positionMs, state.isPlaying, state.shuffle, state.repeatMode) {
        persistedTrack = state.trackIndex
        persistedPosition = state.positionMs
        persistedPlaying = state.isPlaying
        persistedShuffle = state.shuffle
        persistedRepeat = state.repeatMode
    }
    // Queue strategy follows shuffle and gapless; startAlbum owns the
    // initial queue, so the first run only records what was applied.
    val uris = remember(album.id) { album.tracks.mapNotNull { it.uri } }
    val titles = remember(album.id) { album.tracks.map { it.title } }
    val artists = remember(album.id) { album.tracks.map { it.artist ?: album.artist } }
    val studioConfig = AudioStudioStore.config
    val singleQueue = state.shuffle || !studioConfig.gapless
    var appliedSingleQueue by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(singleQueue) {
        if (appliedSingleQueue == null) {
            appliedSingleQueue = singleQueue
        } else if (appliedSingleQueue != singleQueue) {
            appliedSingleQueue = singleQueue
            engine?.sendQueue(uris, titles, artists, state.trackIndex, singleQueue, state.positionMs)
        }
    }
    LaunchedEffect(state.repeatMode) {
        engine?.sendRepeatOne(state.repeatMode == RepeatMode.ONE)
    }
    // The full Studio snapshot rides to the service, which applies EQ,
    // preamp, ReplayGain and crossfade on its players.
    LaunchedEffect(studioConfig) {
        engine?.sendStudio(studioConfig)
    }
    // Background controls need the notification runtime permission.
    val notifPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(album.id) {
        if (album.isReal && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var liquidSheetOpen by rememberSaveable { mutableStateOf(false) }
    var queueOpen by rememberSaveable { mutableStateOf(false) }
    var optionsOpen by rememberSaveable { mutableStateOf(false) }
    var plusOpen by rememberSaveable { mutableStateOf(false) }
    var lyricsOpen by rememberSaveable { mutableStateOf(false) }
    val format by PlaybackHub.format.collectAsState()
    val likedKey = remember(album.id, state.trackIndex) {
        LikedStore.keyFor(album, state.trackIndex)
    }
    val liked = LikedStore.isLiked(likedKey)

    fun playQueueIndex(index: Int) {
        queueOpen = false
        if (album.isReal) {
            PlaybackOrchestrator.startAlbum(
                context, album, index, playing = true, shuffle = state.shuffle,
            )
        } else {
            state.playTrack(index)
        }
    }

    fun shareTrack() {
        val uri = state.track.uri ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, "${state.track.title} — ${state.album.artist}")
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .semantics { contentDescription = "Now playing" },
    ) {
        ObsidianBackdrop(album = artAlbum)
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ObsidianArtwork(
                        album = artAlbum,
                        onOpenLiquid = onOpenLiquid,
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f),
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .navigationBarsPadding()
                        .padding(end = 28.dp, top = 4.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    ObsidianTopBar(
                        liked = liked,
                        onBack = onBack,
                        onToggleLike = { LikedStore.toggle(likedKey) },
                        tint = glassTint,
                    )
                    Spacer(Modifier.height(12.dp))
                    PlayerStack(
                        state = state,
                        format = format,
                        liked = liked,
                        demo = !album.isReal,
                        onOpenOptions = { optionsOpen = true },
                        onOpenLiquid = onOpenLiquid,
                        onOpenQueue = { queueOpen = true },
                        onOpenStudio = onOpenStudio,
                        onOpenPlus = { plusOpen = true },
                        onOpenLyrics = { lyricsOpen = true },
                        artistFor = artistFor,
                        tint = glassTint,
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize().navigationBarsPadding()) {
                ObsidianTopBar(
                    liked = liked,
                    onBack = onBack,
                    onToggleLike = { LikedStore.toggle(likedKey) },
                    tint = glassTint,
                )
                Spacer(Modifier.height(8.dp))
                ObsidianArtwork(
                    album = artAlbum,
                    onOpenLiquid = onOpenLiquid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .aspectRatio(1f),
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
                Spacer(Modifier.height(22.dp))
                PlayerStack(
                    state = state,
                    format = format,
                    liked = liked,
                    demo = !album.isReal,
                    fillTransportGap = true,
                    onOpenOptions = { optionsOpen = true },
                    onOpenLiquid = onOpenLiquid,
                    onOpenQueue = { queueOpen = true },
                    onOpenStudio = onOpenStudio,
                    onOpenPlus = { plusOpen = true },
                    onOpenLyrics = { lyricsOpen = true },
                    artistFor = artistFor,
                    tint = glassTint,
                    modifier = Modifier.padding(horizontal = 28.dp),
                )
                Spacer(Modifier.height(26.dp))
            }
        }
        if (queueOpen) {
            ModalBottomSheet(
                onDismissRequest = { queueOpen = false },
                containerColor = colors.graphite,
                contentColor = colors.ivory,
                modifier = Modifier.border(0.8f.dp, colors.hairline, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            ) {
                QueueSheet(
                    album = album,
                    currentIndex = state.trackIndex,
                    onPlayTrack = ::playQueueIndex,
                )
            }
        }
        if (optionsOpen) {
            ModalBottomSheet(
                onDismissRequest = { optionsOpen = false },
                containerColor = colors.graphite,
                contentColor = colors.ivory,
                modifier = Modifier.border(0.8f.dp, colors.hairline, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            ) {
                PlayerOptionsSheet(
                    state = state,
                    formatLine = format?.displayLine,
                    onOpenLiquid = {
                        optionsOpen = false
                        onOpenLiquid()
                    },
                    onOpenLiquidSettings = {
                        optionsOpen = false
                        liquidSheetOpen = true
                    },
                    onOpenPlus = {
                        optionsOpen = false
                        plusOpen = true
                    },
                    onShareTrack = ::shareTrack,
                )
            }
        }
        if (plusOpen) {
            ModalBottomSheet(
                onDismissRequest = { plusOpen = false },
                containerColor = colors.graphite,
                contentColor = colors.ivory,
                modifier = Modifier.border(0.8f.dp, colors.hairline, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            ) {
                PlusSheet(
                    trackKey = likedKey,
                    isLiked = liked,
                    onToggleLike = { LikedStore.toggle(likedKey) },
                )
            }
        }
        if (liquidSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { liquidSheetOpen = false },
                containerColor = colors.graphite,
                contentColor = colors.ivory,
                modifier = Modifier.border(0.8f.dp, colors.hairline, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            ) {
                LiquidSettingsSheet()
            }
        }
        if (lyricsOpen) {
            LyricsView(
                album = artAlbum,
                trackIndex = state.trackIndex,
                positionMs = state.positionMs,
                onDismiss = { lyricsOpen = false },
            )
        }
    }
}

/**
 * Full-bleed artwork wash: the cover blurred past recognition under a
 * legibility scrim. Below API 31 blur is a no-op, so the art dims into
 * the hue gradient instead of rendering sharp behind the controls.
 */
@Composable
private fun ObsidianBackdrop(album: Album, modifier: Modifier = Modifier) {
    // Pre-blurred once per album off-thread: no GPU blur in the frame path.
    val art = rememberBackdropArtwork(album)
    val colors = LocalSonoraColors.current
    val light = colors.isLight
    val tintTarget = remember(album.artSeed, light) {
        val hue = ((album.artSeed % 360 + 360) % 360).toFloat()
        if (light) Color.hsv(hue, 0.30f, 0.92f) else Color.hsv(hue, 0.55f, 0.20f)
    }
    val tint by animateColorAsState(tintTarget, animationSpec = tween(600), label = "backdrop-tint")
    val scrim = if (light) Color.White else Color.Black
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to tint,
                    0.5f to if (light) Color(0xFFF8F4EC) else Color(0xFF0B0B10),
                    1f to if (light) colors.abyss else Color.Black,
                ),
            ),
    ) {
        Crossfade(targetState = art, animationSpec = tween(600), label = "backdrop-art") { current ->
            if (current != null) {
                Image(
                    bitmap = current,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.25f
                            scaleY = 1.25f
                        },
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to scrim.copy(alpha = 0.28f),
                        0.45f to scrim.copy(alpha = 0.42f),
                        1f to scrim.copy(alpha = 0.72f),
                    ),
                ),
        )
    }
}

@Composable
private fun ObsidianTopBar(
    liked: Boolean,
    onBack: () -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val type = LocalSonoraTypography.current
    val colors = LocalSonoraColors.current
    val pop = remember { Animatable(1f) }
    LaunchedEffect(liked) {
        if (liked) {
            pop.snapTo(1.35f)
            pop.animateTo(1f, spring(dampingRatio = 0.5f))
        }
    }
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassCircleButton(onClick = onBack, description = "Close player", size = 52.dp, tint = tint) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = colors.content, modifier = Modifier.size(28.dp))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                "Now Playing",
                style = type.caption.copy(
                    fontSize = (13.0f * rememberSonoraScale()).sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                ),
                color = colors.content.copy(alpha = 0.85f),
            )
        }
        GlassCircleButton(
            onClick = onToggleLike,
            description = if (liked) "Unlike this song" else "Like this song",
            size = 52.dp,
            tint = tint,
        ) {
            Icon(
                if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = colors.content,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    },
            )
        }
    }
}

/**
 * Frosted-glass circle. The backdrop behind it is pre-blurred artwork,
 * which is what makes the translucent fill read as liquid glass.
 */
@Composable
private fun GlassCircleButton(
    onClick: () -> Unit,
    description: String,
    size: Dp,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    icon: @Composable () -> Unit,
) {
    val tick = rememberTapTick()
    val colors = LocalSonoraColors.current
    val fill = tint?.let { lerp(colors.frost, it.copy(alpha = colors.frost.alpha), 0.45f) }
        ?: colors.frost
    Surface(
        onClick = { tick(); onClick() },
        modifier = modifier
            .size(size)
            .semantics { contentDescription = description },
        shape = CircleShape,
        color = fill,
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ObsidianArtwork(
    album: Album,
    onOpenLiquid: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val art = rememberAlbumArtwork(album)
    val heroModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                rememberSharedContentState(key = "now-playing-art"),
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    } else {
        Modifier
    }
    Box(
        heroModifier
            .then(modifier)
            .shadow(12.dp, ArtShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(ArtShape)
            .clickable(
                role = Role.Button,
                onClickLabel = "Open Liquid visualizer",
                onClick = onOpenLiquid,
            )
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .pointerInput("longpress") {
                detectTapGestures(onLongPress = { onOpenLiquid() })
            }
            .drawWithContent {
                drawContent()
                // True alpha dissolve: the card's own pixels fade out,
                // so the bottom corners melt into the backdrop instead
                // of just darkening. DstIn on an offscreen layer keeps
                // every pixel where the mask is opaque and erases where
                // it is transparent — no black overlay involved.
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.Black,
                        0.55f to Color.Black,
                        0.78f to Color.Black.copy(alpha = 0.5f),
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = art, animationSpec = tween(600), label = "hero-art") { current ->
            CoverArt(
                art = current,
                fallbackLetter = album.title.firstOrNull()?.uppercase() ?: "",
                contentDescription = "Cover art for ${album.title}",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerStack(
    state: NowPlayingState,
    format: TrackFormat?,
    liked: Boolean,
    demo: Boolean,
    onOpenOptions: () -> Unit,
    onOpenLiquid: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenStudio: () -> Unit,
    onOpenPlus: () -> Unit,
    onOpenLyrics: () -> Unit,
    artistFor: (Int) -> String,
    tint: Color? = null,
    modifier: Modifier = Modifier,
    fillTransportGap: Boolean = false,
) {
    val type = LocalSonoraTypography.current
    val colors = LocalSonoraColors.current
    val artistLine = remember(state.album.id, state.trackIndex) { artistFor(state.trackIndex) }
    val scale = rememberSonoraScale()
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    state.track.title,
                    style = type.headline
                        .copy(
                            fontSize = (26.0f * scale).sp,
                            lineHeight = (30.0f * scale).sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    color = colors.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    artistLine,
                    style = type.body.copy(fontSize = (15.0f * scale).sp, fontWeight = FontWeight.Medium),
                    color = colors.content.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            GlassCircleButton(onClick = onOpenOptions, description = "More options", size = 52.dp, tint = tint) {
                Icon(Icons.Filled.MoreVert, contentDescription = null, tint = colors.content, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        ObsidianSeekBar(
            state = state,
            onSeek = { state.seekTo((it * state.durationMs).toLong()) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        TimeRow(state = state, badge = formatBadge(format))
        TrackDetails(format = format, album = state.album)
        if (fillTransportGap) {
            Spacer(Modifier.weight(1f))
        } else {
            Spacer(Modifier.height(16.dp))
        }
        ObsidianTransport(
            isPlaying = state.isPlaying,
            onPrev = { state.prev() },
            onToggle = { state.toggle() },
            onNext = { state.next() },
        )
        Spacer(Modifier.height(24.dp))
        ObsidianActionRow(
            liked = liked,
            onOpenLiquid = onOpenLiquid,
            onOpenPlus = onOpenPlus,
            onOpenQueue = onOpenQueue,
            onOpenStudio = onOpenStudio,
            onOpenLyrics = onOpenLyrics,
            tint = tint,
        )
        if (demo) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Demo playback — allow library access to hear your music.",
                style = type.caption.copy(fontSize = 12.sp),
                color = colors.content.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun formatBadge(format: TrackFormat?): String {
    if (format == null) return "Lossless"
    return when (format.codec.uppercase()) {
        "FLAC", "ALAC", "WAV" -> "Lossless"
        else -> format.codec.uppercase()
    }
}

@Composable
private fun TrackDetails(
    format: TrackFormat?,
    album: Album,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val details = listOfNotNull(
        format?.displayLine,
        album.year.takeIf { it > 0 }?.toString(),
        album.label.takeIf { it.isNotBlank() },
    ).filter { !it.isNullOrBlank() }
    if (details.isEmpty()) return
    Text(
        details.joinToString(" · "),
        style = type.caption.copy(fontSize = (12.0f * rememberSonoraScale()).sp),
        color = colors.ivoryFaint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun TimeRow(state: NowPlayingState, badge: String, modifier: Modifier = Modifier) {
    val type = LocalSonoraTypography.current
    val colors = LocalSonoraColors.current
    val scale = rememberSonoraScale()
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            state.positionMs.toMinSec(),
            style = type.caption.copy(fontSize = (12.0f * scale).sp),
            color = colors.content.copy(alpha = 0.6f),
        )
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.Filled.GraphicEq,
            contentDescription = null,
            tint = colors.content.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            badge,
            style = type.caption.copy(fontSize = (12.0f * scale).sp, fontWeight = FontWeight.Medium),
            color = colors.content.copy(alpha = 0.6f),
        )
        Spacer(Modifier.weight(1f))
        Text(
            "-${(state.durationMs - state.positionMs).toMinSec()}",
            style = type.caption.copy(fontSize = 12.sp),
            color = colors.content.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun ObsidianSeekBar(state: NowPlayingState, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    // Reads the clock here so the player body skips per-tick recomposition.
    val fraction = state.fraction
    val tick = rememberTapTick()
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val density = LocalDensity.current
    var scrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(fraction) }
    Column(modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(26.dp)) {
            if (scrubbing) {
                val bubbleW = with(density) { 72.dp.toPx() }
                val x = (scrubFraction * constraints.maxWidth - bubbleW / 2)
                    .coerceIn(0f, (constraints.maxWidth - bubbleW).coerceAtLeast(0f))
                Box(
                    Modifier
                        .offset { IntOffset(x.toInt(), 0) }
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.frost)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        (scrubFraction * state.durationMs).toLong().toMinSec(),
                        style = type.mono,
                        color = colors.content,
                    )
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(32.dp)) {
            val widthPx = constraints.maxWidth.toFloat()
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput("tap", widthPx) {
                        detectTapGestures { offset ->
                            tick(); onSeek((offset.x / widthPx).coerceIn(0f, 1f))
                        }
                    }
                    .pointerInput("drag", widthPx) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                scrubbing = true
                                scrubFraction = fraction
                            },
                            onDragEnd = { scrubbing = false },
                            onDragCancel = { scrubbing = false },
                        ) { change, _ ->
                            val f = (change.position.x / widthPx).coerceIn(0f, 1f)
                            scrubFraction = f
                            onSeek(f)
                            change.consume()
                        }
                    }
                    .semantics { contentDescription = "Seek" },
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(colors.content.copy(alpha = 0.22f)),
                )
                Box(
                    Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(4.dp)
                        .shadow(6.dp, CircleShape, ambientColor = colors.content.copy(alpha = 0.5f))
                        .clip(CircleShape)
                        .background(colors.content),
                )
            }
        }
    }
}

@Composable
private fun ObsidianTransport(
    isPlaying: Boolean,
    onPrev: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tick = rememberTapTick()
    val colors = LocalSonoraColors.current
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Surface(
            onClick = { tick(); onPrev() },
            modifier = Modifier
                .size(64.dp)
                .semantics { contentDescription = "Previous track" },
            shape = CircleShape,
            color = Color.Transparent,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.FastRewind, contentDescription = null, tint = colors.content, modifier = Modifier.size(46.dp))
            }
        }
        Surface(
            onClick = { tick(); onToggle() },
            modifier = Modifier
                .size(92.dp)
                .semantics { contentDescription = if (isPlaying) "Pause" else "Play" },
            shape = CircleShape,
            color = colors.content,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = colors.contentInverse,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
        Surface(
            onClick = { tick(); onNext() },
            modifier = Modifier
                .size(64.dp)
                .semantics { contentDescription = "Next track" },
            shape = CircleShape,
            color = Color.Transparent,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.FastForward, contentDescription = null, tint = colors.content, modifier = Modifier.size(46.dp))
            }
        }
    }
}

@Composable
private fun ObsidianActionRow(
    liked: Boolean,
    onOpenLiquid: () -> Unit,
    onOpenPlus: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenStudio: () -> Unit,
    onOpenLyrics: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val colors = LocalSonoraColors.current
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        GlassCircleButton(onClick = onOpenLiquid, description = "Open Liquid visualizer", size = 64.dp, tint = tint) {
            Icon(Icons.Filled.FormatQuote, contentDescription = null, tint = colors.content, modifier = Modifier.size(26.dp))
        }
        GlassCircleButton(
            onClick = onOpenPlus,
            description = "Save options",
            size = 64.dp,
            tint = tint,
        ) {
            Icon(
                if (liked) Icons.Filled.Check else Icons.Filled.Add,
                contentDescription = null,
                tint = colors.content,
                modifier = Modifier.size(28.dp),
            )
        }
        GlassCircleButton(onClick = onOpenQueue, description = "Open queue", size = 64.dp, tint = tint) {
            Icon(Icons.Filled.List, contentDescription = null, tint = colors.content, modifier = Modifier.size(26.dp))
        }
        GlassCircleButton(onClick = onOpenStudio, description = "Open Audio Studio", size = 64.dp, tint = tint) {
            Icon(Icons.Filled.Tune, contentDescription = null, tint = colors.content, modifier = Modifier.size(26.dp))
        }
        GlassCircleButton(onClick = onOpenLyrics, description = "Open lyrics", size = 64.dp, tint = tint) {
            Icon(Icons.Filled.EventNote, contentDescription = null, tint = colors.content, modifier = Modifier.size(26.dp))
        }
    }
}
