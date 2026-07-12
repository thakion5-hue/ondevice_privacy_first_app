package com.genspark.privacyfirstai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Mint,
    secondary = Violet,
    tertiary = Rose,
    background = Ink,
    surface = Slate,
    onPrimary = Ink,
    onBackground = Cloud,
    onSurface = Cloud
)

private val LightColors = lightColorScheme(
    primary = Slate,
    secondary = Violet,
    tertiary = Rose,
    background = Cloud,
    surface = androidx.compose.ui.graphics.Color.White,
    onPrimary = Cloud,
    onBackground = Ink,
    onSurface = Ink
)

@Composable
fun PrivacyFirstTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
