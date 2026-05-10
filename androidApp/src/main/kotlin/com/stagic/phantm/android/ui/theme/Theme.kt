package com.stagic.phantm.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PhantmGreen,
    onPrimary = PhantmOnBackground,
    background = PhantmBackground,
    onBackground = PhantmOnBackground,
    surface = PhantmSurface,
    onSurface = PhantmOnSurface,
    error = PhantmError,
    outline = PhantmOutline,
)

private val LightColorScheme = lightColorScheme(
    primary = PhantmGreenLight,
    onPrimary = PhantmBackgroundLight,
    background = PhantmBackgroundLight,
    onBackground = PhantmOnBackgroundLight,
    surface = PhantmSurfaceLight,
    onSurface = PhantmOnSurfaceLight,
    error = PhantmErrorLight,
    outline = PhantmOutlineLight,
)

/** Phantm app theme — automatically switches between dark/light based on system setting (AC-M13-8). */
@Composable
fun PhantmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = PhantmTypography,
        content = content,
    )
}
