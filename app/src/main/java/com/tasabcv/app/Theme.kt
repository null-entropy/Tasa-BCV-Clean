package com.tasabcv.app

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat

// ---------------------------------------------------------------------------
// Paleta "Grafito" (v4.5.0): identidad monocroma, sin azul.
//   Claro  -> blancos y grises claros; acento casi negro.
//   Oscuro -> negros y grises reales; acento casi blanco.
// El delta (subida/bajada) se adapta por tema: clarito en claro, oscuro en
// oscuro. El relieve lo pone Depth.kt (neumorfismo sin blur).
// ---------------------------------------------------------------------------

data class AppColors(
    // Fondo general (degradado vertical muy sutil)
    val backgroundTop: Color,
    val backgroundBottom: Color,
    // Superficies elevadas (degradado diagonal luz->sombra)
    val surfaceTop: Color,
    val surfaceBottom: Color,
    // Huecos/hundidos (controles segmentados, campos de texto)
    val insetTop: Color,
    val insetBottom: Color,
    // Biseles: arista de luz y de sombra del relieve
    val bevelLight: Color,
    val bevelDark: Color,
    val insetBevelLight: Color,
    val insetBevelDark: Color,
    // Sombra proyectada de los elementos elevados
    val shadow: Color,
    // Contenido
    val accent: Color,
    val onSurface: Color,
    val muted: Color,
    val divider: Color,
    // Delta adaptado por tema (subida = rojo, bajada = verde)
    val deltaUp: Color,
    val deltaDown: Color,
    val isDark: Boolean
)

val LightAppColors = AppColors(
    backgroundTop = Color(0xFFEFF0F3),
    backgroundBottom = Color(0xFFE8E9ED),
    surfaceTop = Color(0xFFFCFDFE),
    surfaceBottom = Color(0xFFF1F2F5),
    insetTop = Color(0xFFE4E5E9),
    insetBottom = Color(0xFFEEEFF2),
    bevelLight = Color(0xE6FFFFFF),
    bevelDark = Color(0x80C7CAD3),
    insetBevelLight = Color(0xCCFFFFFF),
    insetBevelDark = Color(0x99C3C6CF),
    shadow = Color(0x66A9ADB8),
    accent = Color(0xFF1B1C20),
    onSurface = Color(0xFF17181B),
    muted = Color(0xFF6E7278),
    divider = Color(0x1417181B),
    deltaUp = Color(0xFFF87171),
    deltaDown = Color(0xFF34D399),
    isDark = false
)

val DarkAppColors = AppColors(
    backgroundTop = Color(0xFF0A0A0C),
    backgroundBottom = Color(0xFF060607),
    surfaceTop = Color(0xFF1B1B1F),
    surfaceBottom = Color(0xFF121215),
    insetTop = Color(0xFF0A0A0D),
    insetBottom = Color(0xFF101014),
    bevelLight = Color(0x1AFFFFFF),
    bevelDark = Color(0xE6000000),
    insetBevelLight = Color(0x12FFFFFF),
    insetBevelDark = Color(0xE6000000),
    shadow = Color(0xA6000000),
    accent = Color(0xFFE6E7EA),
    onSurface = Color(0xFFF4F4F5),
    muted = Color(0xFF9CA0A8),
    divider = Color(0x14FFFFFF),
    deltaUp = Color(0xFFDC2626),
    deltaDown = Color(0xFF059669),
    isDark = true
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/** Colores de la identidad Grafito según el tema activo. */
val appColors: AppColors
    @Composable get() = LocalAppColors.current

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B1C20),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E8EC),
    onPrimaryContainer = Color(0xFF17181B),
    secondary = Color(0xFF6E7278),
    background = Color(0xFFEFF0F3),
    onBackground = Color(0xFF17181B),
    surface = Color(0xFFF7F8FA),
    onSurface = Color(0xFF17181B),
    surfaceVariant = Color(0xFFE7E8EC),
    onSurfaceVariant = Color(0xFF6E7278),
    outline = Color(0xFFD9DBE1),
    error = Color(0xFFE5484D),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE6E7EA),
    onPrimary = Color(0xFF0A0A0C),
    primaryContainer = Color(0xFF1B1B1F),
    onPrimaryContainer = Color(0xFFF4F4F5),
    secondary = Color(0xFF9CA0A8),
    background = Color(0xFF0A0A0C),
    onBackground = Color(0xFFF4F4F5),
    surface = Color(0xFF161619),
    onSurface = Color(0xFFF4F4F5),
    surfaceVariant = Color(0xFF1E1E22),
    onSurfaceVariant = Color(0xFF9CA0A8),
    outline = Color(0xFF26262B),
    error = Color(0xFFF87171),
    onError = Color(0xFF0A0A0C)
)

val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

private val AppTypography = Typography().run {
    Typography(
        displayLarge = displayLarge.copy(fontFamily = PoppinsFontFamily),
        displayMedium = displayMedium.copy(fontFamily = PoppinsFontFamily),
        displaySmall = displaySmall.copy(fontFamily = PoppinsFontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = PoppinsFontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = PoppinsFontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = PoppinsFontFamily),
        titleLarge = titleLarge.copy(fontFamily = PoppinsFontFamily),
        titleMedium = titleMedium.copy(fontFamily = PoppinsFontFamily),
        titleSmall = titleSmall.copy(fontFamily = PoppinsFontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = PoppinsFontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = PoppinsFontFamily),
        bodySmall = bodySmall.copy(fontFamily = PoppinsFontFamily),
        labelLarge = labelLarge.copy(fontFamily = PoppinsFontFamily),
        labelMedium = labelMedium.copy(fontFamily = PoppinsFontFamily),
        labelSmall = labelSmall.copy(fontFamily = PoppinsFontFamily)
    )
}

@Composable
fun DolarBCVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val identityColors = if (darkTheme) DarkAppColors else LightAppColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // Con edge-to-edge las barras del sistema son transparentes y el
            // fondo lo pinta el contenido; aquí solo se controla la apariencia
            // (claro/oscuro) de los íconos de la barra de estado.
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAppColors provides identityColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
