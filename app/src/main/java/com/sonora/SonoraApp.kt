package com.sonora

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Temporary scaffold root. Replaced by the SONORA design system (M0)
 * and the Shelf experience (M1).
 */
@Composable
fun SonoraApp() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0B0D)),
            contentAlignment = Alignment.Center
        ) {
            Text("SONORA", color = Color(0xFFF2EDE3))
        }
    }
}
