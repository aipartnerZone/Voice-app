package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MunnaLightColorScheme = lightColorScheme(
    primary = BrandRoyalBlue,
    onPrimary = Color.White,
    primaryContainer = SoftSkyBlueCard,
    onPrimaryContainer = NavyTextPrimary,
    secondary = BrandCyanGlow,
    onSecondary = Color.White,
    secondaryContainer = LighterSkyBlueSurface,
    onSecondaryContainer = NavyTextPrimary,
    tertiary = StatusSuccessGreen,
    background = IceWhiteBackground,
    onBackground = NavyTextPrimary,
    surface = PureWhiteSurface,
    onSurface = NavyTextPrimary,
    surfaceVariant = SoftSkyBlueCard,
    onSurfaceVariant = NavyTextSecondary,
    outline = SoftBlueBorder,
    error = StatusErrorRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MunnaLightColorScheme,
        typography = Typography,
        content = content
    )
}
