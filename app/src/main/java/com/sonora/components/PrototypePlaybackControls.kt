package com.sonora.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography

/**
 * Detail playback actions. Play and Shuffle open Now Playing (real audio
 * for library albums, simulated in demo); Queue stays honestly disabled
 * with a caption saying so.
 */
@Composable
fun PrototypePlaybackControls(
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {
            PrototypeAction(
                icon = Icons.Filled.PlayArrow,
                label = "Play",
                primary = true,
                enabled = true,
                onClick = onPlay,
                modifier = Modifier.weight(1f),
            )
            PrototypeAction(
                icon = Icons.Filled.Shuffle,
                label = "Shuffle",
                primary = false,
                enabled = true,
                onClick = onShuffle,
                modifier = Modifier.weight(1f),
            )
            PrototypeAction(
                icon = Icons.Filled.QueueMusic,
                label = "Queue",
                primary = false,
                enabled = false,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Play and Shuffle are live — the full queue is in Now Playing.",
            style = type.caption,
            color = colors.ivoryFaint,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PrototypeAction(
    icon: ImageVector,
    label: String,
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    // Tighter tracking and padding than display caps so icon + label
    // fit a third-width button.
    val buttonLabel = type.labelCaps.copy(letterSpacing = 0.5.sp)
    val buttonPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    if (primary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            contentPadding = buttonPadding,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.ivory,
                contentColor = Color.Black,
                disabledContainerColor = colors.ivory.copy(alpha = 0.28f),
                disabledContentColor = Color.Black.copy(alpha = 0.75f),
            ),
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(label, style = buttonLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            contentPadding = buttonPadding,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = colors.graphite,
                contentColor = colors.ivory,
                disabledContentColor = colors.ivoryDim.copy(alpha = 0.7f),
            ),
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(label, style = buttonLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
