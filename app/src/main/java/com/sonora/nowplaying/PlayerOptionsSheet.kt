package com.sonora.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sonora.liquid.LiquidConfigStore
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.content
import com.sonora.theme.SonoraShapes
import com.sonora.theme.ThemeStore

/**
 * M8 options sheet behind the player's "..." button: shuffle, repeat,
 * appearance, Liquid entry points and the playing track's format readout.
 */
@Composable
fun PlayerOptionsSheet(
    state: NowPlayingState,
    formatLine: String?,
    onOpenLiquid: () -> Unit,
    onOpenLiquidSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(SonoraShapes.small)
                .clickable(
                    role = Role.Switch,
                    onClickLabel = if (state.shuffle) "Turn shuffle off" else "Turn shuffle on",
                    onClick = { state.toggleShuffle() },
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Shuffle, contentDescription = null, tint = colors.ivory, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text("Shuffle", style = type.body, color = colors.ivory, modifier = Modifier.weight(1f))
            Switch(
                checked = state.shuffle,
                onCheckedChange = { state.toggleShuffle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.content,
                    checkedTrackColor = colors.content.copy(alpha = 0.4f),
                    uncheckedThumbColor = colors.content.copy(alpha = 0.6f),
                    uncheckedTrackColor = colors.content.copy(alpha = 0.18f),
                ),
            )
        }
        OptionRow(
            icon = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
            label = "Repeat",
            value = when (state.repeatMode) {
                RepeatMode.OFF -> "Off"
                RepeatMode.ALL -> "All"
                RepeatMode.ONE -> "One"
            },
            active = state.repeatMode != RepeatMode.OFF,
            onClick = { state.cycleRepeat() },
        )
        OptionRow(
            icon = Icons.Filled.Palette,
            label = "Appearance",
            value = ThemeStore.mode.label,
            onClick = { ThemeStore.cycle() },
        )
        OptionRow(
            icon = Icons.Filled.WaterDrop,
            label = "Liquid visualizer",
            value = LiquidConfigStore.config.mode.label,
            onClick = onOpenLiquid,
        )
        OptionRow(
            icon = Icons.Filled.Settings,
            label = "Liquid settings",
            onClick = onOpenLiquidSettings,
        )
        if (formatLine != null) {
            Text(
                formatLine,
                style = type.mono,
                color = colors.ivoryFaint,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    active: Boolean = false,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(SonoraShapes.small)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.ivory, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = type.body, color = colors.ivory, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(
                value,
                style = type.caption,
                color = if (active) colors.ivory else colors.ivoryDim,
            )
        }
    }
}
