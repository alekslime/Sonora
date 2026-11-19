package com.sonora.nowplaying

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.sonora.data.Album
import com.sonora.data.rememberAlbumArtwork
import com.sonora.data.toMinSec
import com.sonora.liquid.LiquidSettingsSheet
import com.sonora.playback.PlaybackHub
import com.sonora.playback.PlaybackOrchestrator
import com.sonora.playback.ServiceEngine
import com.sonora.playback.TrackFormat
import com.sonora.studio.AudioStudioStore
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography

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
@OptIn(ExperimentalMaterial3Api::class)
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
    val state = if (album.isReal && orchestrated?.album?.id == album.id) orchestrated else localState
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
        if (!album.isReal) {
            state.restore(
                persistedTrack, persistedPosition, persistedPlaying,
                persistedShuffle, persistedRepeat,
            )
            state.ticker()
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
    val artists = remember(album.id) { List(album.tracks.size) { album.artist } }
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

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .semantics { contentDescription = "Now playing" },
    ) {
        ObsidianBackdrop(album = album)
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
                        album = album,
                        onOpenLiquid = onOpenLiquid,
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f),
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
                    )
                    Spacer(Modifier.height(12.dp))
                    PlayerStack(
                        state = state,
                        format = format,
                        liked = liked,
                        likedKey = likedKey,
                        demo = !album.isReal,
                        onOpenOptions = { optionsOpen = true },
                        onOpenLiquid = onOpenLiquid,
                        onOpenQueue = { queueOpen = true },
                        onOpenStudio = onOpenStudio,
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize().navigationBarsPadding()) {
                ObsidianTopBar(
                    liked = liked,
                    onBack = onBack,
                    onToggleLike = { LikedStore.toggle(likedKey) },
                )
                Spacer(Modifier.height(8.dp))
                ObsidianArtwork(
                    album = album,
                    onOpenLiquid = onOpenLiquid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .aspectRatio(1f),
                )
                Spacer(Modifier.height(22.dp))
                PlayerStack(
                    state = state,
                    format = format,
                    liked = liked,
                    likedKey = likedKey,
                    demo = !album.isReal,
                    fillTransportGap = true,
                    onOpenOptions = { optionsOpen = true },
                    onOpenLiquid = onOpenLiquid,
                    onOpenQueue = { queueOpen = true },
                    onOpenStudio = onOpenStudio,
                    modifier = Modifier.padding(horizontal = 28.dp),
                )
                Spacer(Modifier.height(26.dp))
            }
        }
        if (queueOpen) {
            ModalBottomSheet(
                onDismissRequest = { queueOpen = false },
                containerColor = colors.charcoal,
                contentColor = colors.ivory,
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
                containerColor = colors.charcoal,
                contentColor = colors.ivory,
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
                )
            }
        }
        if (liquidSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { liquidSheetOpen = false },
                containerColor = colors.charcoal,
                contentColor = colors.ivory,
            ) {
                LiquidSettingsSheet()
            }
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
    val art = rememberAlbumArtwork(album)
    val tint = remember(album.artSeed) {
        Color.hsv(((album.artSeed % 360 + 360) % 360).toFloat(), 0.55f, 0.20f)
    }
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to tint,
                    0.5f to Color(0xFF0B0B10),
                    1f to Color.Black,
                ),
            ),
    ) {
        if (art != null) {
            Image(
                bitmap = art,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.25f
                        scaleY = 1.25f
                    }
                    .blur(80.dp)
                    .alpha(if (Build.VERSION.SDK_INT >= 31) 1f else 0.45f),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.28f),
                        0.45f to Color.Black.copy(alpha = 0.42f),
                        1f to Color.Black.copy(alpha = 0.72f),
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
) {
    val type = LocalSonoraTypography.current
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassCircleButton(onClick = onBack, description = "Close player", size = 52.dp) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                "Now Playing",
                style = type.caption.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                ),
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        GlassCircleButton(
            onClick = onToggleLike,
            description = if (liked) "Unlike this song" else "Like this song",
            size = 52.dp,
        ) {
            Icon(
                if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
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
    icon: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .semantics { contentDescription = description },
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.14f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
    }
}

@Composable
private fun ObsidianArtwork(
    album: Album,
    onOpenLiquid: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = LocalSonoraTypography.current
    val art = rememberAlbumArtwork(album)
    Box(
        modifier
            .shadow(12.dp, ArtShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(ArtShape)
            .background(Color(0xFF1A1A1A))
            .clickable(
                role = Role.Button,
                onClickLabel = "Open Liquid visualizer",
                onClick = onOpenLiquid,
            )
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
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
        if (art != null) {
            Image(
                bitmap = art,
                contentDescription = "Cover art for ${album.title}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                album.title.firstOrNull()?.uppercase() ?: "",
                style = type.display,
                color = Color.White.copy(alpha = 0.25f),
            )
        }
    }
}

@Composable
private fun PlayerStack(
    state: NowPlayingState,
    format: TrackFormat?,
    liked: Boolean,
    likedKey: String,
    demo: Boolean,
    onOpenOptions: () -> Unit,
    onOpenLiquid: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenStudio: () -> Unit,
    modifier: Modifier = Modifier,
    fillTransportGap: Boolean = false,
) {
    val type = LocalSonoraTypography.current
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    state.track.title,
                    style = type.title.copy(
                        fontSize = 26.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    state.album.artist,
                    style = type.body.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            GlassCircleButton(onClick = onOpenOptions, description = "More options", size = 52.dp) {
                Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        ObsidianSeekBar(
            fraction = state.fraction,
            onSeek = { state.seekTo((it * state.durationMs).toLong()) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                state.positionMs.toMinSec(),
                style = type.caption.copy(fontSize = 12.sp),
                color = Color.White.copy(alpha = 0.6f),
            )
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                formatBadge(format),
                style = type.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                color = Color.White.copy(alpha = 0.6f),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "-${(state.durationMs - state.positionMs).toMinSec()}",
                style = type.caption.copy(fontSize = 12.sp),
                color = Color.White.copy(alpha = 0.6f),
            )
        }
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
            onToggleLike = { LikedStore.toggle(likedKey) },
            onOpenQueue = onOpenQueue,
            onOpenStudio = onOpenStudio,
        )
        if (demo) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Demo playback — allow library access to hear your music.",
                style = type.caption.copy(fontSize = 12.sp),
                color = Color.White.copy(alpha = 0.45f),
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
private fun ObsidianSeekBar(fraction: Float, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.height(32.dp)) {
        val widthPx = constraints.maxWidth.toFloat()
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput("tap", widthPx) {
                    detectTapGestures { offset ->
                        onSeek((offset.x / widthPx).coerceIn(0f, 1f))
                    }
                }
                .pointerInput("drag", widthPx) {
                    detectHorizontalDragGestures { change, _ ->
                        onSeek((change.position.x / widthPx).coerceIn(0f, 1f))
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
                    .background(Color.White.copy(alpha = 0.22f)),
            )
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(4.dp)
                    .shadow(6.dp, CircleShape, ambientColor = Color.White.copy(alpha = 0.5f))
                    .clip(CircleShape)
                    .background(Color.White),
            )
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
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Surface(
            onClick = onPrev,
            modifier = Modifier
                .size(64.dp)
                .semantics { contentDescription = "Previous track" },
            shape = CircleShape,
            color = Color.Transparent,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size(46.dp))
            }
        }
        Surface(
            onClick = onToggle,
            modifier = Modifier
                .size(92.dp)
                .semantics { contentDescription = if (isPlaying) "Pause" else "Play" },
            shape = CircleShape,
            color = Color.White,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
        Surface(
            onClick = onNext,
            modifier = Modifier
                .size(64.dp)
                .semantics { contentDescription = "Next track" },
            shape = CircleShape,
            color = Color.Transparent,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(46.dp))
            }
        }
    }
}

@Composable
private fun ObsidianActionRow(
    liked: Boolean,
    onOpenLiquid: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenStudio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        GlassCircleButton(onClick = onOpenLiquid, description = "Open Liquid visualizer", size = 64.dp) {
            Icon(Icons.Filled.FormatQuote, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        GlassCircleButton(
            onClick = onToggleLike,
            description = if (liked) "Liked" else "Add to liked songs",
            size = 64.dp,
        ) {
            Icon(
                if (liked) Icons.Filled.Check else Icons.Filled.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
        GlassCircleButton(onClick = onOpenQueue, description = "Open queue", size = 64.dp) {
            Icon(Icons.Filled.List, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        GlassCircleButton(onClick = onOpenStudio, description = "Open Audio Studio", size = 64.dp) {
            Icon(Icons.Filled.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
    }
}
