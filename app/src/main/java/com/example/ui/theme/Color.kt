package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Primary avocado greens
val PrimaryGreen = Color(0xFF568203)
val PrimaryGreenDark = Color(0xFF436700)
val PrimaryFixedDim = Color(0xFFA4D659)
val OnPrimaryFixedVariant = Color(0xFF324F00)

val PrimaryGreenContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF2E4C0E) else Color(0xFFE1F5C4)

val OnPrimaryGreenContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFFD5EDB0) else Color(0xFF172B00)

// Secondary citrus oranges
val SecondaryOrange = Color(0xFFFD8B00)
val SecondaryOrangeBright = Color(0xFFFFA500)
val SecondaryOrangeDark = Color(0xFFFF8C00)

val SecondaryContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF5A2E00) else Color(0xFFFFDCC3)

val OnSecondaryContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFFFFDCC3) else Color(0xFF603100)

// Neutral surfaces and borders (Adaptive to Dark and Light theme)
val SurfaceWhite: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF1E2124) else Color(0xFFFFFFFF)

val SurfaceBright: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF121416) else Color(0xFFF9FAFB)

val SurfaceContainerLow: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF25292D) else Color(0xFFF3F3F4)

val SurfaceContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF2C3136) else Color(0xFFEEEEEE)

val SurfaceContainerHigh: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF33383E) else Color(0xFFE8E8E8)

val SurfaceContainerLowest: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF181A1D) else Color(0xFFFFFFFF)

val OnSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFFF3F4F6) else Color(0xFF1A1C1C)

val OnSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF4B5563)

val OutlineVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF374151) else Color(0xFFE5E7EB)

val OutlineVariantGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF385324) else Color(0xFFC3C9B4)

val Outline: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF6B7280) else Color(0xFF737967)

val ErrorRed = Color(0xFFBA1A1A)
val ErrorContainer = Color(0xFFFFDAD6)
val ErrorLight = Color(0xFFEF4444)

val BlueWeather = Color(0xFF2563EB)

val BlueWeatherBg: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF1E293B) else Color(0xFFEFF6FF)

val WhatsAppGreen = Color(0xFF25D366)
val WarningOrange = Color(0xFFF59E0B)

