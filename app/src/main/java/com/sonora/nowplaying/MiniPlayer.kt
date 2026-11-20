package com.sonora.nowplaying

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.sonora.components.CoverArt
import com.sonora.components.rememberTapTick
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonora.data.rememberAlbumArtwork
import com.sonora.theme.LocalSonoraTypography

private val MiniShape = RoundedCornerShape(20.dp)

/**
 * M11 floating mini-player. Lives above the tab bar on every screen
 * but Now Playing, driven by the app-scoped session (real or demo):
 * cover, identity, a white play disc and a progress hairline. The
 * pill expands to Now Playing; the disc toggles without leaving.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MiniPlayer(
    session: NowPlayingState,
    artistLine: String,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val type = LocalSonoraTypography.current
    val art = rememberAlbumArtwork(session.album)
    val tick = rememberTapTick()
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
    Surface(
        onClick = onExpand,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Open Now Playing" },
        shape = MiniShape,
        color = Color.White.copy(alpha = 0.10f),
    ) {
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverArt(
                    art = art,
                    fallbackLetter = session.album.title.firstOrNull()?.uppercase() ?: "",
                    modifier = heroModifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        session.track.title,
                        style = type.title,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        artistLine,
                        style = type.caption,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    onClick = { tick(); session.toggle() },
                    modifier = Modifier
                        .size(44.dp)
                        .semantics {
                            contentDescription = if (session.isPlaying) "Pause" else "Play"
                        },
                    shape = CircleShape,
                    color = Color.White,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (session.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(session.fraction.coerceIn(0f, 1f))
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f)),
            )
        }
    }
}
