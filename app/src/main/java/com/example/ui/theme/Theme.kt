package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = MidnightBg,
    primaryContainer = MidnightSurfaceVariant,
    onPrimaryContainer = NeonCyanLight,
    secondary = ShieldEmerald,
    onSecondary = MidnightBg,
    secondaryContainer = Color(0xFF00381D),
    onSecondaryContainer = ShieldEmerald,
    tertiary = WarningAmber,
    onTertiary = MidnightBg,
    error = DangerRed,
    onError = Color.White,
    background = MidnightBg,
    onBackground = TextPrimary,
    surface = MidnightSurface,
    onSurface = TextPrimary,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = MidnightCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // Keep consistent cyber dark theme for VPN security aesthetic
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
