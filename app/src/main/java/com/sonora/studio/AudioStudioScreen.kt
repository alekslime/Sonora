package com.sonora.studio

import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sonora.gesture.GestureConfig
import com.sonora.gesture.GestureConfigStore
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.SonoraShapes

/**
 * M4 Audio Studio overlay: ten-band EQ, presets, preamp, ReplayGain,
 * gapless/crossfade, output-route readout and gesture preferences.
 * Every setting drives the M6 background engine live.
 */
@Composable
fun AudioStudioScreen(
    onClose: () -> Unit,
    sourceLabel: String,
    formatLine: String?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    BackHandler(onBack = onClose)
    Column(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.obsidian, colors.abyss)))
            .systemBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Close Audio Studio", tint = colors.ivory)
            }
            Spacer(Modifier.width(4.dp))
            Text("Audio Studio".uppercase(), style = LocalSonoraTypography.current.labelCaps, color = colors.ivory)
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            EqualizerSection()
            Spacer(Modifier.height(20.dp))
            PreampSection()
            Spacer(Modifier.height(20.dp))
            ReplayGainSection()
            Spacer(Modifier.height(20.dp))
            PlaybackSection()
            Spacer(Modifier.height(20.dp))
            OutputSection(sourceLabel = sourceLabel, formatLine = formatLine)
            Spacer(Modifier.height(20.dp))
            GestureSection()
            Spacer(Modifier.height(16.dp))
            Text(
                "Every setting shapes the playing audio live",
                style = LocalSonoraTypography.current.labelCaps,
                color = colors.ivory,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SonoraShapes.large)
                    .background(colors.coral)
                    .padding(vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Text(text.uppercase(), style = type.labelCaps, color = colors.ivory)
    Spacer(Modifier.height(4.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EqualizerSection(modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val config = AudioStudioStore.config
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Equalizer".uppercase(), style = type.labelCaps, color = colors.ivory)
            Spacer(Modifier.weight(1f))
            Text(
                if (config.eqEnabled) "On" else "Off",
                style = type.caption,
                color = colors.ivoryDim,
            )
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = config.eqEnabled,
                onCheckedChange = { AudioStudioStore.setEqEnabled(it) },
                modifier = Modifier.semantics { contentDescription = "Equalizer" },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.obsidian,
                    checkedTrackColor = colors.amber,
                    uncheckedThumbColor = colors.ivoryDim,
                    uncheckedTrackColor = colors.graphite,
                ),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            config.bands.forEachIndexed { index, value ->
                EqBand(
                    valueDb = value,
                    hz = AudioStudioConfig.BAND_HZ[index],
                    enabled = config.eqEnabled,
                    onChange = { AudioStudioStore.setBand(index, it) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Preset: ${config.presetName}",
            style = type.caption,
            color = colors.ivoryDim,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AudioStudioConfig.BUILT_IN_PRESETS.forEach { preset ->
                FilterChip(
                    selected = config.presetName == preset.name,
                    onClick = { AudioStudioStore.applyPreset(preset) },
                    label = { Text(preset.name, style = type.caption, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = colors.charcoal,
                        labelColor = colors.ivoryDim,
                        selectedContainerColor = colors.amber.copy(alpha = 0.22f),
                        selectedLabelColor = colors.ivory,
                    ),
                )
            }
        }
    }
}

@Composable
private fun EqBand(
    valueDb: Float,
    hz: Int,
    enabled: Boolean,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val range = AudioStudioConfig.MAX_DB - AudioStudioConfig.MIN_DB
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "%+.0f".format(valueDb),
            style = type.mono,
            color = if (enabled) colors.ivory else colors.ivoryFaint,
        )
        Spacer(Modifier.height(6.dp))
        BoxWithDrag(
            onPosition = { fraction -> onChange(AudioStudioConfig.MAX_DB - fraction * range) },
            enabled = enabled,
            contentDescription = "$hz hertz band, ${"%+.0f".format(valueDb)} decibels",
            modifier = Modifier.height(150.dp),
        ) {
            // Track.
            Box(
                Modifier
                    .width(4.dp)
                    .height(150.dp)
                    .clip(CircleShape)
                    .background(colors.graphite),
            )
            // Fill from center detent to value.
            val centerFraction = (AudioStudioConfig.MAX_DB / range)
            val valueFraction = (AudioStudioConfig.MAX_DB - valueDb) / range
            val top = minOf(centerFraction, valueFraction)
            val fillHeight = kotlin.math.abs(centerFraction - valueFraction).coerceAtLeast(0.02f)
            Box(
                Modifier
                    .padding(top = (top * 150f).dp, bottom = ((1f - top - fillHeight) * 150f).dp.coerceAtLeast(0.dp))
                    .width(4.dp)
                    .clip(CircleShape)
                    .background(if (enabled) colors.amber else colors.ivoryFaint),
            )
            // Knob.
            Box(
                Modifier
                    .padding(top = ((valueFraction * 150f) - 5f).dp.coerceAtLeast(0.dp))
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (enabled) colors.amber else colors.ivoryFaint),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (hz >= 1000) "${hz / 1000}k" else "$hz",
            style = type.caption,
            color = colors.ivoryFaint,
        )
    }
}

@Composable
private fun BoxWithDrag(
    onPosition: (Float) -> Unit,
    enabled: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .pointerInput("tap", enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    onPosition((offset.y / size.height).coerceIn(0f, 1f))
                }
            }
            .pointerInput("drag", enabled) {
                if (!enabled) return@pointerInput
                detectVerticalDragGestures { change, _ ->
                    onPosition((change.position.y / size.height).coerceIn(0f, 1f))
                    change.consume()
                }
            }
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription },
        contentAlignment = Alignment.TopCenter,
    ) {
        content()
    }
}

@Composable
private fun PreampSection(modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val preamp = AudioStudioStore.config.preampDb
    Column(modifier.fillMaxWidth()) {
        SectionTitle("Preamp")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("%+.0f dB".format(preamp), style = type.mono, color = colors.ivory)
            Spacer(Modifier.weight(1f))
            Text("Boost or trim the whole signal", style = type.caption, color = colors.ivoryFaint)
        }
        Slider(
            value = preamp,
            onValueChange = { AudioStudioStore.setPreamp(it) },
            valueRange = AudioStudioConfig.MIN_DB..AudioStudioConfig.MAX_DB,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Preamp" },
            colors = SliderDefaults.colors(
                thumbColor = colors.amber,
                activeTrackColor = colors.amber,
                inactiveTrackColor = colors.graphite,
            ),
        )
    }
}

@Composable
private fun ReplayGainSection(modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val mode = AudioStudioStore.config.replayGain
    Column(modifier.fillMaxWidth()) {
        SectionTitle("Volume normalization")
        Text(
            "Reads ReplayGain tags (MP3, FLAC, M4A); files without tags play untouched.",
            style = type.caption,
            color = colors.ivoryFaint,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReplayGainMode.entries.forEach { entry ->
                val active = mode == entry
                Text(
                    entry.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = type.labelCaps,
                    color = if (active) colors.obsidian else colors.ivoryDim,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(SonoraShapes.small)
                        .background(if (active) colors.amber else colors.charcoal)
                        .clickable(role = Role.RadioButton) { AudioStudioStore.setReplayGain(entry) }
                        .padding(vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun PlaybackSection(modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val config = AudioStudioStore.config
    Column(modifier.fillMaxWidth()) {
        SectionTitle("Playback")
        ToggleRow(
            label = "Gapless playback",
            caption = "No gaps between consecutive album tracks",
            checked = config.gapless,
            onChecked = { AudioStudioStore.setGapless(it) },
        )
        Spacer(Modifier.height(4.dp))
        ToggleRow(
            label = "Crossfade",
            caption = "Fades skips and shuffle; gapless album flow stays intact",
            checked = config.crossfadeEnabled,
            onChecked = { AudioStudioStore.setCrossfadeEnabled(it) },
        )
        if (config.crossfadeEnabled) {
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${config.crossfadeSecs}s overlap", style = type.mono, color = colors.ivory)
                Spacer(Modifier.weight(1f))
                Slider(
                    value = config.crossfadeSecs.toFloat(),
                    onValueChange = { AudioStudioStore.setCrossfadeSecs(it.toInt()) },
                    valueRange = AudioStudioConfig.MIN_CROSSFADE_SECS.toFloat()..AudioStudioConfig.MAX_CROSSFADE_SECS.toFloat(),
                    steps = AudioStudioConfig.MAX_CROSSFADE_SECS - AudioStudioConfig.MIN_CROSSFADE_SECS - 1,
                    modifier = Modifier
                        .width(180.dp)
                        .semantics { contentDescription = "Crossfade seconds" },
                    colors = SliderDefaults.colors(
                        thumbColor = colors.amber,
                        activeTrackColor = colors.amber,
                        inactiveTrackColor = colors.graphite,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    caption: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = type.title, color = colors.ivory)
            Text(caption, style = type.caption, color = colors.ivoryFaint)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            modifier = Modifier.semantics { contentDescription = label },
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.obsidian,
                checkedTrackColor = colors.amber,
                uncheckedThumbColor = colors.ivoryDim,
                uncheckedTrackColor = colors.graphite,
            ),
        )
    }
}

@Composable
private fun OutputSection(sourceLabel: String, formatLine: String?, modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val context = LocalContext.current
    val routes = remember {
        val manager = context.applicationContext.getSystemService(AudioManager::class.java)
        if (manager != null) OutputRoutes.current(manager) else listOf("Unknown output")
    }
    Column(modifier.fillMaxWidth()) {
        SectionTitle("Output")
        routes.forEach { route ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.amber),
                )
                Spacer(Modifier.width(10.dp))
                Text(route, style = type.body, color = colors.ivory)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Source: $sourceLabel.",
            style = type.caption,
            color = colors.ivoryFaint,
        )
        if (formatLine != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                "Playing: $formatLine.",
                style = type.mono,
                color = colors.ivoryDim,
            )
        }
    }
}

@Composable
private fun GestureSection(modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val config = GestureConfigStore.config
    Column(modifier.fillMaxWidth()) {
        SectionTitle("Gestures")
        Text(
            "Three fingers: hold for Liquid, swipe up for Studio, swipe down to go back. " +
                "Visible controls always work; accessibility services take precedence.",
            style = type.caption,
            color = colors.ivoryFaint,
        )
        Spacer(Modifier.height(8.dp))
        ToggleRow(
            label = "Three-finger gestures",
            caption = "Hold and swipe detection",
            checked = config.enabled,
            onChecked = { GestureConfigStore.setEnabled(it) },
        )
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Hold to open Liquid", style = type.title, color = colors.ivory)
            Spacer(Modifier.weight(1f))
            Text("${config.holdDurationMs} ms", style = type.mono, color = colors.ivoryDim)
        }
        Slider(
            value = config.holdDurationMs.toFloat(),
            onValueChange = { GestureConfigStore.setHoldDuration(it.toLong()) },
            valueRange = GestureConfig.MIN_HOLD_MS.toFloat()..GestureConfig.MAX_HOLD_MS.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Hold duration" },
            colors = SliderDefaults.colors(
                thumbColor = colors.amber,
                activeTrackColor = colors.amber,
                inactiveTrackColor = colors.graphite,
            ),
        )
        ToggleRow(
            label = "Haptics",
            caption = "Confirm gesture activation",
            checked = config.haptics,
            onChecked = { GestureConfigStore.setHaptics(it) },
        )
        Spacer(Modifier.height(4.dp))
        ToggleRow(
            label = "Reduced motion",
            caption = "Still Liquid frame instead of animation",
            checked = config.reducedMotion,
            onChecked = { GestureConfigStore.setReducedMotion(it) },
        )
    }
}
