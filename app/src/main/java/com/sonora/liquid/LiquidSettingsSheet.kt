package com.sonora.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.selectable
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
import android.os.SystemClock
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonora.playback.PlaybackHub
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.SonoraShapes

/**
 * M3 Liquid settings content (hosted in a bottom sheet from Now Playing):
 * opening equation, auto-cycle, palette preset, response intensity and
 * render quality.
 */
@Composable
fun LiquidSettingsSheet(modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val config = LiquidConfigStore.config
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text("Liquid".uppercase(), style = type.labelCaps, color = colors.ivory)
        Spacer(Modifier.height(4.dp))
        Text(
            "Equations, palette and performance",
            style = type.caption,
            color = colors.ivoryDim,
        )
        Spacer(Modifier.height(16.dp))
        SignalReadout()
        Spacer(Modifier.height(16.dp))
        Text("Equation".uppercase(), style = type.labelCaps, color = colors.ivoryFaint)
        Spacer(Modifier.height(8.dp))
        VizMode.entries.chunked(2).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { mode ->
                    ModeChip(
                        mode = mode,
                        selected = config.mode == mode,
                        onSelect = { LiquidConfigStore.setMode(mode) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(SonoraShapes.small)
                .background(colors.charcoal)
                .clickable(
                    role = Role.Switch,
                    onClickLabel = if (config.autoCycle) "Turn auto-cycle off" else "Turn auto-cycle on",
                    onClick = { LiquidConfigStore.setAutoCycle(!config.autoCycle) },
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Auto-cycle equations",
                style = type.body,
                color = colors.ivory,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = config.autoCycle,
                onCheckedChange = { LiquidConfigStore.setAutoCycle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.ivory,
                    checkedTrackColor = colors.amber,
                    uncheckedThumbColor = colors.ivoryDim,
                    uncheckedTrackColor = colors.graphite,
                ),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Preset".uppercase(), style = type.labelCaps, color = colors.ivoryFaint)
        Spacer(Modifier.height(8.dp))
        LiquidPreset.entries.chunked(2).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { preset ->
                    PresetChip(
                        preset = preset,
                        selected = config.preset == preset,
                        onSelect = { LiquidConfigStore.setPreset(preset) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text("Intensity".uppercase(), style = type.labelCaps, color = colors.ivoryFaint)
        Slider(
            value = config.intensity,
            onValueChange = { LiquidConfigStore.setIntensity(it) },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = colors.amber,
                activeTrackColor = colors.amber,
                inactiveTrackColor = colors.graphite,
            ),
        )
        Spacer(Modifier.height(6.dp))
        Text("Quality".uppercase(), style = type.labelCaps, color = colors.ivoryFaint)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LiquidQuality.entries.forEach { quality ->
                val active = config.quality == quality
                Text(
                    quality.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = type.labelCaps,
                    color = if (active) colors.obsidian else colors.ivoryDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(SonoraShapes.small)
                        .background(if (active) colors.amber else colors.charcoal)
                        .selectable(
                            selected = active,
                            role = Role.RadioButton,
                            onClick = { LiquidConfigStore.setQuality(quality) },
                        )
                        .padding(vertical = 10.dp),
                )
            }
        }
    }
}

/**
 * M10 analyzer readout: whether the visualizer is hearing live audio
 * or running on the mock, with the session and capture age that
 * prove it. Recomposes on every capture while live.
 */
@Composable
private fun SignalReadout(modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val live by PlaybackHub.vizSignal.collectAsState()
    val active by PlaybackHub.vizActive.collectAsState()
    val sessionId by PlaybackHub.audioSessionId.collectAsState()
    val ageMs = live?.let { SystemClock.elapsedRealtime() - it.frame.atMs }
    val (status, tint) = when {
        live != null && ageMs != null && ageMs < 500L -> "Live" to colors.mint
        live != null -> "Stale" to colors.sun
        else -> "Mock" to colors.ivoryDim
    }
    Column(modifier.fillMaxWidth()) {
        Text("Signal".uppercase(), style = type.labelCaps, color = colors.ivoryFaint)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(SonoraShapes.small)
                .background(colors.charcoal)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(tint),
            )
            Spacer(Modifier.width(10.dp))
            Text(status, style = type.body, color = colors.ivory)
            Spacer(Modifier.weight(1f))
            Text(
                "session $sessionId · analyzer ${if (active) "on" else "off"}" +
                    (ageMs?.let { " · ${it}ms" } ?: ""),
                style = type.mono,
                color = colors.ivoryDim,
            )
        }
    }
}

@Composable
private fun ModeChip(
    mode: VizMode,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Text(
        mode.label,
        style = type.caption,
        color = if (selected) colors.ivory else colors.ivoryDim,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(SonoraShapes.small)
            .background(if (selected) colors.amber.copy(alpha = 0.18f) else colors.charcoal)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

@Composable
private fun PresetChip(
    preset: LiquidPreset,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Row(
        modifier
            .clip(SonoraShapes.small)
            .background(if (selected) colors.amber.copy(alpha = 0.18f) else colors.charcoal)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
            listOf(preset.deep, preset.body, preset.accent).forEach { swatch ->
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(swatch),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            preset.label,
            style = type.caption,
            color = if (selected) colors.ivory else colors.ivoryDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
