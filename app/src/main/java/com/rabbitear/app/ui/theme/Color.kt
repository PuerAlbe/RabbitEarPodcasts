package com.rabbitear.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val GruvboxOrange = Color(0xFFFE8019)
val GruvboxCream = Color(0xFFEBDBB2)
val GruvboxGreen = Color(0xFF98971A)
val GruvboxRed = Color(0xFFCC241D)
val GruvboxDarkHard = Color(0xFF14100C)
val GruvboxDarkSoft = Color(0xFF262019)
val GruvboxDarkMedium = Color(0xFF322A20)
val GruvboxMuted = Color(0xFFBDAE93)
val GruvboxBorder = Color(0xFF4A3F31)
val GruvboxBorderSubtle = Color(0xFF8A7D66)

val GruvboxColorScheme = darkColorScheme(
    primary = GruvboxOrange,
    onPrimary = GruvboxDarkHard,
    primaryContainer = GruvboxDarkMedium,
    onPrimaryContainer = GruvboxOrange,
    secondary = GruvboxGreen,
    onSecondary = GruvboxDarkHard,
    tertiary = GruvboxRed,
    onTertiary = GruvboxCream,
    background = GruvboxDarkHard,
    onBackground = GruvboxCream,
    surface = GruvboxDarkHard,
    onSurface = GruvboxCream,
    surfaceVariant = GruvboxDarkSoft,
    onSurfaceVariant = GruvboxMuted,
    outline = GruvboxBorder,
    outlineVariant = GruvboxBorderSubtle
)

val FallbackDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB59D),
    onPrimary = Color(0xFF5F1600),
    primaryContainer = Color(0xFF802900),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    background = Color(0xFF1A110E),
    onBackground = Color(0xFFF1DFDA),
    surface = Color(0xFF1A110E),
    onSurface = Color(0xFFF1DFDA),
    surfaceVariant = Color(0xFF53433F),
    onSurfaceVariant = Color(0xFFD8C2BC),
    outline = Color(0xFFA08C87)
)

val FallbackLightColorScheme = lightColorScheme(
    primary = Color(0xFFA63C00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF380700),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF231917),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF231917),
    surfaceVariant = Color(0xFFF5DED8),
    onSurfaceVariant = Color(0xFF53433F),
    outline = Color(0xFF85736E)
)
