package com.sonora.studio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonora.gesture.GestureConfig
import com.sonora.gesture.GestureConfigStore
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.SonoraShapes
import kotlin.math.roundToInt

/**
 * M4 Audio Studio: equalizer, preamp, playback behavior, output
 * information and gesture preferences. Settings are stored now and take
 * effect with the M5 audio engine; nothing here pretends otherwise.
 */
@Composable
fun AudioStudioScreen(
    onClose: () -> Unit,
    touchExplorationOn: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val config = AudioStudioStore.config
    BackHandler(onBack = onClose)

    Column(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.obsidian, colors.abyss)))
            .semantics { contentDescription = "Audio Studio" },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Audio Studio".uppercase(), style = type.labelCaps, color = colors.ivory)
                Spacer(Modifier.height(2.dp))
                Text("Shape your sound", style = type.caption, color = colors.ivoryFaint)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close Audio Studio", tint = colors.ivory)
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            SectionTitle("Equalizer")
            SwitchRow(
                label = "Equalizer",
                checked = config.eqEnabled,
                onCheckedChange = { AudioStudioStore.setEqEnabled(it) },
            )
            EqPresetRow(
                preset = config.preset,
                onSelect = { AudioStudioStore.setPreset(it) },
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                config.bandGainsDb.forEachIndexed { index, gain ->
                    EqFader(
                        valueDb = gain,
                        onValueChange = { AudioStudioStore.setBand(index, snapDb(it)) },
                        freqLabel = EQ_FREQUENCY_LABELS[index],
                        enabled = config.eqEnabled,
                    )
                }
            }
            Text(
                "Drag a fader, tap the track, or double-tap to reset a band.",
                style = type.caption,
                color = colors.ivoryFaint,
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Preamp".uppercase(), style = type.labelCaps, color = colors.ivoryDim)
                Spacer(Modifier.weight(1f))
                Text(formatDb(config.preampDb), style = type.mono, color = colors.amber)
            }
            Slider(
                value = config.preampDb,
                onValueChange = { AudioStudioStore.setPreamp(snapDb(it)) },
                valueRange = PREAMP_MIN_DB..PREAMP_MAX_DB,
                steps = 47,
                enabled = config.eqEnabled,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = colors.amber,
                    activeTrackColor = colors.amber,
                    inactiveTrackColor = colors.graphite,
                ),
            )

            SectionTitle("Playback")
            ReplayGainRow(mode = config.replayGain)
            Text(
                "ReplayGain follows file tags when present; nothing is invented when tags are absent.",
                style = type.caption,
                color = colors.ivoryFaint,
            )
            Spacer(Modifier.height(8.dp))
            SwitchRow(
                label = "Gapless playback",
                checked = config.gapless,
                onCheckedChange = { AudioStudioStore.setGapless(it) },
            )
            SwitchRow(
                label = "Crossfade",
                checked = config.crossfadeEnabled,
                onCheckedChange = { AudioStudioStore.setCrossfadeEnabled(it) },
            )
            if (config.crossfadeEnabled) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Length".uppercase(), style = type.labelCaps, color = colors.ivoryDim)
                    Spacer(Modifier.weight(1f))
                    Text("${config.crossfadeSec} s", style = type.mono, color = colors.amber)
                }
                Slider(
                    value = config.crossfadeSec.toFloat(),
                    onValueChange = { AudioStudioStore.setCrossfadeSec(it.roundToInt()) },
                    valueRange = CROSSFADE_MIN_SEC.toFloat()..CROSSFADE_MAX_SEC.toFloat(),
                    steps = CROSSFADE_MAX_SEC - CROSSFADE_MIN_SEC - 1,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = colors.amber,
                        activeTrackColor = colors.amber,
                        inactiveTrackColor = colors.graphite,
                    ),
                )
            }
            Text(
                "Crossfade applies between separate tracks; gapless albums stay seamless.",
                style = type.caption,
                color = colors.ivoryFaint,
            )

            SectionTitle("Output")
            OutputSection()

            SectionTitle("Gestures")
            GesturesSection(touchExplorationOn = touchExplorationOn)

            Spacer(Modifier.height(12.dp))
            Text(
                "Studio settings take effect with the M5 audio engine.",
                style = type.caption,
                color = colors.ivoryFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    val type = LocalSonoraTypography.current
    val colors = LocalSonoraColors.current
    Spacer(modifier.height(18.dp))
    Text(title.uppercase(), style = type.labelCaps, color = colors.ivoryFaint)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = type.body, color = colors.ivory, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
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
private fun EqPresetRow(preset: EqPreset, onSelect: (EqPreset) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EqPreset.entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { entry ->
                    val active = preset == entry
                    Text(
                        entry.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },
                        style = type.caption,
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
                                onClick = { onSelect(entry) },
                            )
                            .padding(vertical = 9.dp),
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** One tactile EQ fader: drag, tap-to-set, double-tap to reset. */
@Composable
private fun EqFader(
    valueDb: Float,
    onValueChange: (Float) -> Unit,
    freqLabel: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val latestValue by rememberUpdatedState(valueDb)
    val latestOnChange by rememberUpdatedState(onValueChange)
    Column(modifier.width(46.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            formatDb(valueDb),
            style = type.mono.copy(fontSize = 10.sp),
            color = if (enabled) colors.ivoryDim else colors.ivoryFaint,
            maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().height(150.dp)) {
            val trackH = constraints.maxHeight.toFloat()
            val pxPerDb = trackH / (EQ_MAX_DB - EQ_MIN_DB)
            fun yToDb(y: Float) = ((trackH / 2f - y) / pxPerDb).coerceIn(EQ_MIN_DB, EQ_MAX_DB)
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(enabled) {
                        detectTapGestures(
                            onDoubleTap = { if (enabled) latestOnChange(0f) },
                            onTap = { offset -> if (enabled) latestOnChange(yToDb(offset.y)) },
                        )
                    }
                    .pointerInput(enabled) {
                        detectVerticalDragGestures { change, dragAmount ->
                            if (enabled) {
                                latestOnChange((latestValue - dragAmount / pxPerDb).coerceIn(EQ_MIN_DB, EQ_MAX_DB))
                            }
                            change.consume()
                        }
                    }
                    .semantics {
                        contentDescription = "$freqLabel equalizer band"
                        progressBarRangeInfo = ProgressBarRangeInfo(valueDb, EQ_MIN_DB..EQ_MAX_DB, 47)
                        stateDescription = formatDb(valueDb)
                        if (enabled) {
                            setProgress { target ->
                                onValueChange(target.coerceIn(EQ_MIN_DB, EQ_MAX_DB))
                                true
                            }
                        } else {
                            disabled()
                        }
                    },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val trackColor = Color(0xFF2A2A30)
                    val active = if (enabled) Color(0xFFE0A458) else Color(0xFF55504A)
                    drawLine(trackColor, Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 4f, cap = StrokeCap.Round)
                    drawLine(
                        active.copy(alpha = 0.5f),
                        Offset(cx - 9f, size.height / 2f),
                        Offset(cx + 9f, size.height / 2f),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round,
                    )
                    val yVal = size.height / 2f - valueDb * pxPerDb
                    drawLine(active, Offset(cx, size.height / 2f), Offset(cx, yVal), strokeWidth = 4f, cap = StrokeCap.Round)
                    drawCircle(Color(0xFF0B0B0D), 11f, Offset(cx, yVal))
                    drawCircle(active, 11f, Offset(cx, yVal), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                    drawCircle(active, 3.5f, Offset(cx, yVal))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(freqLabel, style = type.mono.copy(fontSize = 10.sp), color = colors.ivoryFaint, maxLines = 1)
    }
}

@Composable
private fun ReplayGainRow(mode: ReplayGainMode, modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReplayGainMode.entries.forEach { entry ->
            val active = mode == entry
            val label = when (entry) {
                ReplayGainMode.OFF -> "Off"
                ReplayGainMode.TRACK -> "Track"
                ReplayGainMode.ALBUM -> "Album"
            }
            Text(
                label,
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
                        onClick = { AudioStudioStore.setReplayGain(entry) },
                    )
                    .padding(vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun OutputSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val info = remember { queryOutputDevices(context) }
    Column(modifier.fillMaxWidth()) {
        info.devices.forEach { device ->
            Text(device, style = type.body, color = colors.ivory, modifier = Modifier.padding(vertical = 3.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Source and format details arrive with the M5 playback engine.",
            style = type.caption,
            color = colors.ivoryFaint,
        )
    }
}

@Composable
private fun GesturesSection(touchExplorationOn: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val config = GestureConfigStore.config
    Column(modifier.fillMaxWidth()) {
        SwitchRow(
            label = "Three-finger gestures",
            checked = config.enabled,
            onCheckedChange = { GestureConfigStore.setEnabled(it) },
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Hold duration".uppercase(), style = type.labelCaps, color = colors.ivoryDim)
            Spacer(Modifier.weight(1f))
            Text(
                "%.1f s".format(config.holdDurationMs / 1000f),
                style = type.mono,
                color = colors.amber,
            )
        }
        Slider(
            value = config.holdDurationMs.toFloat(),
            onValueChange = { GestureConfigStore.setHoldDurationMs(it.toLong()) },
            valueRange = GestureConfig.MIN_HOLD_MS.toFloat()..GestureConfig.MAX_HOLD_MS.toFloat(),
            steps = 11,
            enabled = config.enabled,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = colors.amber,
                activeTrackColor = colors.amber,
                inactiveTrackColor = colors.graphite,
            ),
        )
        SwitchRow(
            label = "Haptic feedback",
            checked = config.haptics,
            onCheckedChange = { GestureConfigStore.setHaptics(it) },
        )
        SwitchRow(
            label = "Reduced motion",
            checked = config.reducedMotion,
            onCheckedChange = { GestureConfigStore.setReducedMotion(it) },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Hold three fingers to open Liquid • Swipe up for Studio • Swipe down to go back • Touch Liquid to exit.",
            style = type.caption,
            color = colors.ivoryDim,
        )
        Spacer(Modifier.height(4.dp))
        if (touchExplorationOn) {
            Text(
                "TalkBack is on: three-finger gestures are paused. Use the prism, the Studio button and Back instead.",
                style = type.caption,
                color = colors.amber,
            )
        } else {
            Text(
                "Every gesture has a visible equivalent: the prism opens Liquid, the Studio button opens this screen.",
                style = type.caption,
                color = colors.ivoryFaint,
            )
        }
    }
}

private fun snapDb(value: Float): Float = (value * 2f).roundToInt() / 2f

private fun formatDb(value: Float): String {
    val snapped = snapDb(value)
    return if (snapped == snapped.toInt().toFloat()) {
        "%+d dB".format(snapped.toInt())
    } else {
        "%+.1f dB".format(snapped)
    }
}
