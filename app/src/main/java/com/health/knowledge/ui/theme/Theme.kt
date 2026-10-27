package com.health.knowledge.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat

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
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
