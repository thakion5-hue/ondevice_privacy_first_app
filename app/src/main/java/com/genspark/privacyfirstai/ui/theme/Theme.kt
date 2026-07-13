package com.genspark.privacyfirstai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = Ink,
    primaryContainer = Mint.copy(alpha = 0.16f),
    onPrimaryContainer = MintSoft,
    secondary = Violet,
    onSecondary = Color.White,
    secondaryContainer = Violet.copy(alpha = 0.18f),
    onSecondaryContainer = VioletSoft,
    tertiary = Rose,
    onTertiary = Color.White,
    tertiaryContainer = Rose.copy(alpha = 0.18f),
    onTertiaryContainer = RoseSoft,
    background = Ink,
    onBackground = Color(0xFFF5F7FC),
    surface = InkSoft,
    onSurface = Color(0xFFF5F7FC),
    surfaceVariant = Slate,
    onSurfaceVariant = Color(0xFFB9C6E3),
    outline = Outline,
    error = Danger,
    errorContainer = Danger.copy(alpha = 0.22f),
    onErrorContainer = Color(0xFFFFE1E4)
)

private val LightColors = lightColorScheme(
    primary = Slate,
    onPrimary = Color.White,
    primaryContainer = MintSoft,
    onPrimaryContainer = Ink,
    secondary = Violet,
    onSecondary = Color.White,
    secondaryContainer = VioletSoft,
    onSecondaryContainer = Ink,
    tertiary = Rose,
    onTertiary = Color.White,
    tertiaryContainer = RoseSoft,
    onTertiaryContainer = Ink,
    background = Cloud,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = CloudSoft,
    onSurfaceVariant = SlateSoft,
    outline = Outline,
    error = Danger,
    errorContainer = Color(0xFFFFE1E6),
    onErrorContainer = Color(0xFF53111B)
)

@Composable
fun PrivacyFirstTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
