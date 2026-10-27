package com.health.knowledge.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.health.knowledge.util.UserPreferencesManager

// Health-themed green palette
val HealthGreen = Color(0xFF2E7D32)
val HealthGreenLight = Color(0xFF60AD5E)
val HealthGreenDark = Color(0xFF005005)
val HealthGreenContainer = Color(0xFFB8F5B0)
val HealthGreenOnContainer = Color(0xFF002204)

val HealthAmber = Color(0xFFFF8F00)
val HealthAmberLight = Color(0xFFFFBF47)
val HealthAmberContainer = Color(0xFFFFECB3)

val SurfaceLight = Color(0xFFF9FBF7)
val SurfaceDark = Color(0xFF121212)

// Soft Sepia reading palette (warm parchment)
val SepiaPrimary = Color(0xFF4A6B3B)
val SepiaPrimaryContainer = Color(0xFFDCE6C8)
val SepiaOnPrimaryContainer = Color(0xFF1E3314)
val SepiaSecondary = Color(0xFF9E5A1A)
val SepiaSecondaryContainer = Color(0xFFF3DEC3)
val SepiaBackground = Color(0xFFF5ECD9)
val SepiaSurface = Color(0xFFFAF3E3)
val SepiaSurfaceVariant = Color(0xFFEAE0C8)
val SepiaOnSurface = Color(0xFF2E241D)
val SepiaOnSurfaceVariant = Color(0xFF5C4B3E)

private val LightColorScheme = lightColorScheme(
    primary = HealthGreen,
    onPrimary = Color.White,
    primaryContainer = HealthGreenContainer,
    onPrimaryContainer = HealthGreenOnContainer,
    secondary = HealthAmber,
    onSecondary = Color.White,
    secondaryContainer = HealthAmberContainer,
    surface = SurfaceLight,
    onSurface = Color(0xFF1A1A1A),
    background = SurfaceLight,
    onBackground = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE8F5E9),
    outline = Color(0xFF81C784)
)

private val SepiaColorScheme = lightColorScheme(
    primary = SepiaPrimary,
    onPrimary = Color.White,
    primaryContainer = SepiaPrimaryContainer,
    onPrimaryContainer = SepiaOnPrimaryContainer,
    secondary = SepiaSecondary,
    onSecondary = Color.White,
    secondaryContainer = SepiaSecondaryContainer,
    surface = SepiaSurface,
    onSurface = SepiaOnSurface,
    onSurfaceVariant = SepiaOnSurfaceVariant,
    background = SepiaBackground,
    onBackground = SepiaOnSurface,
    surfaceVariant = SepiaSurfaceVariant,
    outline = Color(0xFF9E8D76)
)

private val DarkColorScheme = darkColorScheme(
    primary = HealthGreenLight,
    onPrimary = Color(0xFF003909),
    primaryContainer = HealthGreenDark,
    onPrimaryContainer = HealthGreenContainer,
    secondary = HealthAmberLight,
    surface = SurfaceDark,
    onSurface = Color(0xFFE8F5E9),
    background = SurfaceDark,
    surfaceVariant = Color(0xFF1E2D1F)
)

@Composable
fun HealthKnowledgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: String = UserPreferencesManager.THEME_SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        UserPreferencesManager.THEME_DARK -> true
        UserPreferencesManager.THEME_LIGHT, UserPreferencesManager.THEME_SEPIA -> false
        else -> darkTheme
    }

    val colorScheme = when (themeMode) {
        UserPreferencesManager.THEME_SEPIA -> SepiaColorScheme
        UserPreferencesManager.THEME_DARK -> DarkColorScheme
        UserPreferencesManager.THEME_LIGHT -> LightColorScheme
        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
