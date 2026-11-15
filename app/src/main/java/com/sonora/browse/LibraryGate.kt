package com.sonora.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.SonoraShapes

/**
 * M5 gate shown before library access is granted. The demo stays one tap
 * away so the app is fully explorable without the permission.
 */
@Composable
fun LibraryGateScreen(
    onAllowAccess: () -> Unit,
    onTryDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GateScaffold(
        kicker = "Your library",
        title = "Hear your own records",
        body = "SONORA plays the music already on this device. " +
            "Allow audio access to scan it — everything stays offline, " +
            "on this phone.",
        primaryLabel = "Allow access",
        onPrimary = onAllowAccess,
        secondaryLabel = "Try the demo",
        onSecondary = onTryDemo,
        modifier = modifier,
    )
}

/**
 * M5 empty state: permission granted, but no music files were found on
 * shared storage.
 */
@Composable
fun EmptyLibraryScreen(
    onRescan: () -> Unit,
    onTryDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GateScaffold(
        kicker = "No music found",
        title = "The shelf is empty",
        body = "No playable audio turned up on this device. Add some music " +
            "files, then rescan — or explore the demo meanwhile.",
        primaryLabel = "Rescan",
        onPrimary = onRescan,
        secondaryLabel = "Try the demo",
        onSecondary = onTryDemo,
        modifier = modifier,
    )
}

@Composable
private fun GateScaffold(
    kicker: String,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Column(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.obsidian, colors.abyss)))
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(kicker.uppercase(), style = type.labelCaps, color = colors.amber)
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            style = type.display,
            color = colors.ivory,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            body,
            style = type.body,
            color = colors.ivoryDim,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            primaryLabel.uppercase(),
            style = type.labelCaps,
            color = colors.obsidian,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(SonoraShapes.small)
                .background(colors.amber)
                .clickable(role = Role.Button, onClick = onPrimary)
                .padding(vertical = 14.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            secondaryLabel.uppercase(),
            style = type.labelCaps,
            color = colors.ivoryDim,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(SonoraShapes.small)
                .clickable(role = Role.Button, onClick = onSecondary)
                .padding(vertical = 14.dp),
        )
    }
}
