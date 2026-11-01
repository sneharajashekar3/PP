package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val PagePilotDarkColorScheme = darkColorScheme(
    primary = PilotIndigoDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF282A4A),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = PilotTealDark,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF133635),
    onSecondaryContainer = Color(0xFF99F6E4),
    tertiary = StatusSky,
    background = ObsidianBackground,
    onBackground = AlabasterText,
    surface = GraphiteSurface,
    onSurface = AlabasterText,
    surfaceVariant = GraphiteElevated,
    onSurfaceVariant = MutedSlateText,
    outline = GraphiteHighlight,
    outlineVariant = Color(0xFF2D3340),
    error = StatusRose,
    onError = Color.White
)

private val PagePilotLightColorScheme = lightColorScheme(
    primary = PilotIndigoLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = PilotTealLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF134E4A),
    tertiary = Color(0xFF0284C7),
    background = PaperBackground,
    onBackground = DeepInkText,
    surface = PureWhiteSurface,
    onSurface = DeepInkText,
    surfaceVariant = SoftStoneContainer,
    onSurfaceVariant = CoolSlateText,
    outline = SoftStoneBorder,
    outlineVariant = Color(0xFFD1D5DB),
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun PagePilotTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val colorScheme = if (useDarkTheme) {
        PagePilotDarkColorScheme
    } else {
        PagePilotLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
