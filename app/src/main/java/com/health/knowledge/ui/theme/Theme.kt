package com.health.knowledge.ui.theme

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
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

val SurfaceLight = Color(0xFFFFFFFF)
val BackgroundLight = Color(0xFFF6F9F5)
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
    background = BackgroundLight,
    onBackground = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFEBF3EC),
    onSurfaceVariant = Color(0xFF425244),
    outline = Color(0xFFB4C6B6),
    outlineVariant = Color(0xFFD8E4D9)
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
    outline = Color(0xFFB8A890),
    outlineVariant = Color(0xFFD8CBB5)
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
    surfaceVariant = Color(0xFF1E2D1F),
    outline = Color(0xFF3E5240),
    outlineVariant = Color(0xFF2B3B2D)
)

/**
 * Returns an exact 1px Dp value for crisp, lightweight 1px borders across all screen densities.
 */
@Composable
fun onePxDp(): Dp = with(LocalDensity.current) { 1f.toDp() }

/**
 * Standard 1px subtle border for cards and surfaces.
 */
@Composable
fun onePxBorder(color: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)): BorderStroke {
    return BorderStroke(width = onePxDp(), color = color)
}

/**
 * Blends a translucent color onto the current theme surface so that Card containerColors
 * remain 100% opaque (eliminating hardware shadow inner-frame artifacts on Android).
 */
@Composable
fun Color.solidOverSurface(): Color = this.compositeOver(MaterialTheme.colorScheme.surface)

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
