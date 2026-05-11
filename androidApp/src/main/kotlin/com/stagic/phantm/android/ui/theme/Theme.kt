package com.stagic.phantm.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PhantmColorScheme = darkColorScheme(
    primary = PhantmAccent,
    onPrimary = PhantmText,
    primaryContainer = PhantmBgSent,
    onPrimaryContainer = PhantmText,
    secondary = PhantmAccent2,
    onSecondary = PhantmText,
    background = PhantmBgBase,
    onBackground = PhantmText,
    surface = PhantmBgSurface,
    onSurface = PhantmText,
    surfaceVariant = PhantmBgElevated,
    onSurfaceVariant = PhantmText2,
    outline = PhantmDivider,
    error = PhantmDanger,
    onError = PhantmText,
)

@Composable
fun PhantmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PhantmColorScheme,
        typography = PhantmTypography,
        content = content,
    )
}
