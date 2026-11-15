package com.sonora.liquid

import androidx.compose.foundation.background
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.SonoraShapes

/**
 * M3 Liquid settings content (hosted in a bottom sheet from Now Playing):
 * visual preset, response intensity and render quality.
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
            "Visualizer preset and performance",
            style = type.caption,
            color = colors.ivoryDim,
        )
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
