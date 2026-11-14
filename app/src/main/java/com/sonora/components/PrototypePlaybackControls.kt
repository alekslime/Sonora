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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography

/**
 * M1 playback controls. The playback engine arrives in M5, so these render
 * the specified actions (Play, Shuffle, queue) in their real layout but
 * honestly disabled, with a caption that says exactly that. No fake
 * playback state is presented.
 */
@Composable
fun PrototypePlaybackControls(modifier: Modifier = Modifier) {
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
                modifier = Modifier.weight(1f),
            )
            PrototypeAction(
                icon = Icons.Filled.Shuffle,
                label = "Shuffle",
                primary = false,
                modifier = Modifier.weight(1f),
            )
            PrototypeAction(
                icon = Icons.Filled.QueueMusic,
                label = "Queue",
                primary = false,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Playback arrives in M5 — controls shown for layout.",
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
            onClick = {},
            enabled = false,
            modifier = modifier,
            contentPadding = buttonPadding,
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = colors.amber.copy(alpha = 0.28f),
                disabledContentColor = colors.ivory.copy(alpha = 0.75f),
            ),
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(label, style = buttonLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = modifier,
            contentPadding = buttonPadding,
            colors = ButtonDefaults.outlinedButtonColors(
                disabledContentColor = colors.ivoryDim.copy(alpha = 0.7f),
            ),
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(label, style = buttonLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
