package com.sonora.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

private fun SonoraColors.toMaterialScheme() = if (isLight) lightColorScheme(
    primary = amber,
    onPrimary = Color.White,
    primaryContainer = amberDeep,
    onPrimaryContainer = Color.White,
    secondary = coral,
    onSecondary = Color.White,
    background = obsidian,
    onBackground = ivory,
    surface = charcoal,
    onSurface = ivory,
    surfaceVariant = graphite,
    onSurfaceVariant = ivoryDim,
    outline = hairline,
    surfaceTint = amber,
) else darkColorScheme(
    primary = amber,
    onPrimary = ivory,
    primaryContainer = amberDeep,
    onPrimaryContainer = ivory,
    secondary = coral,
    onSecondary = ivory,
    background = obsidian,
    onBackground = ivory,
    surface = charcoal,
    onSurface = ivory,
    surfaceVariant = graphite,
    onSurfaceVariant = ivoryDim,
    outline = hairline,
    surfaceTint = amber,
)

private fun SonoraTypography.toMaterialType(): Typography {
    val default = Typography()
    return default.copy(
        displayLarge = display,
        displayMedium = display,
        displaySmall = headline,
        headlineLarge = headline,
        headlineMedium = headline,
        headlineSmall = title,
        titleLarge = title,
        titleMedium = title,
        titleSmall = labelCaps,
        bodyLarge = body,
        bodyMedium = body,
        bodySmall = caption,
        labelLarge = labelCaps,
        labelMedium = labelCaps,
        labelSmall = caption,
    )
}

/**
 * M0 root theme. SONORA tokens are the source of truth and are mapped
 * onto Material3 so stock components inherit the identity.
 */
@Composable
fun SonoraTheme(content: @Composable () -> Unit) {
    val dark = when (ThemeStore.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = remember(dark) { if (dark) SonoraColors() else SonoraColors.light() }
    val typography = remember { SonoraTypography() }
    val scheme = remember(colors) { colors.toMaterialScheme() }
    val materialType = remember(typography) { typography.toMaterialType() }
    CompositionLocalProvider(
        LocalSonoraColors provides colors,
        LocalSonoraTypography provides typography,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = materialType,
            shapes = SonoraShapes,
            content = content,
        )
    }
}
