package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1F5C4),
    onPrimaryContainer = Color(0xFF172B00),
    secondary = SecondaryOrangeDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC3),
    onSecondaryContainer = Color(0xFF603100),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1C),
    surfaceVariant = Color(0xFFF3F3F4),
    onSurfaceVariant = Color(0xFF4B5563),
    background = Color(0xFFF9FAFB),
    onBackground = Color(0xFF1A1C1C),
    outline = Color(0xFF737967),
    outlineVariant = Color(0xFFE5E7EB),
    error = ErrorRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryFixedDim,
    onPrimary = OnPrimaryFixedVariant,
    primaryContainer = Color(0xFF2E4C0E),
    onPrimaryContainer = Color(0xFFD5EDB0),
    secondary = SecondaryOrangeBright,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF5A2E00),
    onSecondaryContainer = Color(0xFFFFDCC3),
    surface = Color(0xFF1E2124),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF25292D),
    onSurfaceVariant = Color(0xFF9CA3AF),
    background = Color(0xFF121416),
    onBackground = Color(0xFFF3F4F6),
    outline = Color(0xFF6B7280),
    outlineVariant = Color(0xFF374151),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

