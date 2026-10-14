package com.tasabcv.app

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat

// ---------------------------------------------------------------------------
// Identidad "Cristal": superficies translúcidas con borde luminoso (Depth.kt).
//   4 variantes de tema x 2 modos del sistema = 8 paletas.
//   SISTEMA = Grafito monocromo (tema original de la app).
//   La transición entre temas interpola TODOS los colores con una sola
//   fracción animada (MotionSpring): el color fluye, nunca aparece de golpe.
//   Costo: recompone solo la pantalla visible durante ~500 ms; en reposo, cero.
// ---------------------------------------------------------------------------

/** Variantes de tema seleccionables en Ajustes. */
enum class ThemeVariant { SISTEMA, AZUL, VERDE, ROSADO }

data class AppColors(
    // Fondo general (degradado vertical muy sutil; opaco: es el telón)
    val backgroundTop: Color,
    val backgroundBottom: Color,
    // Cristal elevado: relleno TRANSLÚCIDO (la alfa ES el efecto vidrio)
    val surfaceTop: Color,
    val surfaceBottom: Color,
    // Huecos/hundidos (controles segmentados, campos de texto, banners)
    val insetTop: Color,
    val insetBottom: Color,
    // Biseles: arista de luz (arriba-izq) y de sombra (abajo-der) del cristal
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
    // Color de error del sistema (ex colorScheme.error; anima con la paleta)
    val error: Color,
    // Delta adaptado por tema (subida = rojo, bajada = verde)
    val deltaUp: Color,
    val deltaDown: Color,
    val isDark: Boolean
)

// ---------------------------------------------------------------------------
// Paletas — Grafito (Sistema)
// ---------------------------------------------------------------------------

val LightAppColors = AppColors(
    backgroundTop = Color(0xFFEFF0F3),
    backgroundBottom = Color(0xFFE8E9ED),
    surfaceTop = Color(0xC8FCFDFE),
    surfaceBottom = Color(0xB8F1F2F5),
    insetTop = Color(0xD9E4E5E9),
    insetBottom = Color(0xC4EEEFF2),
    bevelLight = Color(0xE6FFFFFF),
    bevelDark = Color(0x80C7CAD3),
    insetBevelLight = Color(0xCCFFFFFF),
    insetBevelDark = Color(0x99C3C6CF),
    shadow = Color(0x66A9ADB8),
    accent = Color(0xFF1B1C20),
    onSurface = Color(0xFF17181B),
    muted = Color(0xFF6E7278),
    divider = Color(0x1417181B),
    error = Color(0xFFE5484D),
    deltaUp = Color(0xFFF87171),
    deltaDown = Color(0xFF34D399),
    isDark = false
)

val DarkAppColors = AppColors(
    backgroundTop = Color(0xFF0A0A0C),
    backgroundBottom = Color(0xFF060607),
    surfaceTop = Color(0xCC1B1B1F),
    surfaceBottom = Color(0xB8121215),
    insetTop = Color(0xD90A0A0D),
    insetBottom = Color(0xB3101014),
    bevelLight = Color(0x26FFFFFF),
    bevelDark = Color(0xE6000000),
    insetBevelLight = Color(0x12FFFFFF),
    insetBevelDark = Color(0xE6000000),
    shadow = Color(0xA6000000),
    accent = Color(0xFFE6E7EA),
    onSurface = Color(0xFFF4F4F5),
    muted = Color(0xFF9CA0A8),
    divider = Color(0x14FFFFFF),
    error = Color(0xFFF87171),
    deltaUp = Color(0xFFDC2626),
    deltaDown = Color(0xFF059669),
    isDark = true
)

// ---------------------------------------------------------------------------
// Paletas — Azul
// ---------------------------------------------------------------------------

val AzulLightColors = AppColors(
    backgroundTop = Color(0xFFC8D7F5),
    backgroundBottom = Color(0xFFB4C7F0),
    surfaceTop = Color(0xC8E3ECFF),
    surfaceBottom = Color(0xB8D3E0F6),
    insetTop = Color(0xD9CBD9F2),
    insetBottom = Color(0xC4D0DEF5),
    bevelLight = Color(0xE6FFFFFF),
    bevelDark = Color(0x8099B4DC),
    insetBevelLight = Color(0xCCFFFFFF),
    insetBevelDark = Color(0x998FAFD8),
    shadow = Color(0x66709BCC),
    accent = Color(0xFF1D4ED8),
    onSurface = Color(0xFF0A1226),
    muted = Color(0xFF3E4F72),
    divider = Color(0x140A1226),
    error = Color(0xFFE5484D),
    deltaUp = Color(0xFFF87171),
    deltaDown = Color(0xFF34D399),
    isDark = false
)

val AzulDarkColors = AppColors(
    backgroundTop = Color(0xFF141C2E),
    backgroundBottom = Color(0xFF101728),
    surfaceTop = Color(0xCC1B2A47),
    surfaceBottom = Color(0xB816233C),
    insetTop = Color(0xD916223A),
    insetBottom = Color(0xB8111A30),
    bevelLight = Color(0x26FFFFFF),
    bevelDark = Color(0xE6000000),
    insetBevelLight = Color(0x12FFFFFF),
    insetBevelDark = Color(0xE6000000),
    shadow = Color(0xA60C1730),
    accent = Color(0xFF7CB0FF),
    onSurface = Color(0xFFF2F6FF),
    muted = Color(0xFF9AB0D6),
    divider = Color(0x14FFFFFF),
    error = Color(0xFFF87171),
    deltaUp = Color(0xFFDC2626),
    deltaDown = Color(0xFF059669),
    isDark = true
)

// ---------------------------------------------------------------------------
// Paletas — Verde
// ---------------------------------------------------------------------------

val VerdeLightColors = AppColors(
    backgroundTop = Color(0xFFC3E2D4),
    backgroundBottom = Color(0xFFAED8C6),
    surfaceTop = Color(0xC8DFF2E8),
    surfaceBottom = Color(0xB8CFE8DA),
    insetTop = Color(0xD9BEDCCB),
    insetBottom = Color(0xC4C7E5D4),
    bevelLight = Color(0xE6FFFFFF),
    bevelDark = Color(0x809FC2AE),
    insetBevelLight = Color(0xCCFFFFFF),
    insetBevelDark = Color(0x9995BFA8),
    shadow = Color(0x6668A88C),
    accent = Color(0xFF0B7A4B),
    onSurface = Color(0xFF071A0F),
    muted = Color(0xFF3A5B4E),
    divider = Color(0x14071A0F),
    error = Color(0xFFE5484D),
    deltaUp = Color(0xFFF87171),
    deltaDown = Color(0xFF34D399),
    isDark = false
)

val VerdeDarkColors = AppColors(
    backgroundTop = Color(0xFF11221A),
    backgroundBottom = Color(0xFF0D1A13),
    surfaceTop = Color(0xCC1A3225),
    surfaceBottom = Color(0xB8142A1F),
    insetTop = Color(0xD9162A20),
    insetBottom = Color(0xB8102218),
    bevelLight = Color(0x26FFFFFF),
    bevelDark = Color(0xE6000000),
    insetBevelLight = Color(0x12FFFFFF),
    insetBevelDark = Color(0xE6000000),
    shadow = Color(0xA60D1A12),
    accent = Color(0xFF45E4AE),
    onSurface = Color(0xFFF0FAF4),
    muted = Color(0xFF97BDA9),
    divider = Color(0x14FFFFFF),
    error = Color(0xFFF87171),
    deltaUp = Color(0xFFDC2626),
    deltaDown = Color(0xFF059669),
    isDark = true
)

// ---------------------------------------------------------------------------
// Paletas — Rosado
// ---------------------------------------------------------------------------

val RosadoLightColors = AppColors(
    backgroundTop = Color(0xFFF3D3E3),
    backgroundBottom = Color(0xFFEBC4D9),
    surfaceTop = Color(0xC8F8E5EF),
    surfaceBottom = Color(0xB8F1D9E6),
    insetTop = Color(0xD9EBD8E5),
    insetBottom = Color(0xC4EFDCE7),
    bevelLight = Color(0xE6FFFFFF),
    bevelDark = Color(0x80C2A2B4),
    insetBevelLight = Color(0xCCFFFFFF),
    insetBevelDark = Color(0x99BEA0B2),
    shadow = Color(0x66B2849E),
    accent = Color(0xFFBE185D),
    onSurface = Color(0xFF220B14),
    muted = Color(0xFF6C4C5F),
    divider = Color(0x14220B14),
    error = Color(0xFFE5484D),
    deltaUp = Color(0xFFF87171),
    deltaDown = Color(0xFF34D399),
    isDark = false
)

val RosadoDarkColors = AppColors(
    backgroundTop = Color(0xFF241019),
    backgroundBottom = Color(0xFF1B0D13),
    surfaceTop = Color(0xCC331B26),
    surfaceBottom = Color(0xB8291520),
    insetTop = Color(0xD92B1722),
    insetBottom = Color(0xB820111A),
    bevelLight = Color(0x26FFFFFF),
    bevelDark = Color(0xE6000000),
    insetBevelLight = Color(0x12FFFFFF),
    insetBevelDark = Color(0xE6000000),
    shadow = Color(0xA6150A10),
    accent = Color(0xFFFA90C8),
    onSurface = Color(0xFFFDF4F9),
    muted = Color(0xFFBC9CAE),
    divider = Color(0x14FFFFFF),
    error = Color(0xFFF87171),
    deltaUp = Color(0xFFDC2626),
    deltaDown = Color(0xFF059669),
    isDark = true
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/** Colores de la identidad Cristal según el tema activo. */
val appColors: AppColors
    @Composable get() = LocalAppColors.current

// ---------------------------------------------------------------------------
// ThemeStore: variante seleccionada en Ajustes, persistida en SharedPreferences.
// El modo (claro/oscuro) SIEMPRE lo decide el sistema. Única fuente de verdad
// para la app y el widget.
// ---------------------------------------------------------------------------

object ThemeStore {
    private const val KEY_VARIANT = "app_theme_variant"
    private var prefs: SharedPreferences? = null

    var variant: ThemeVariant by mutableStateOf(ThemeVariant.SISTEMA)
        private set

    /** Se llama UNA vez en MainActivity.onCreate: carga la variante persistida. */
    fun load(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(DolarRepository.PREFS_NAME, Context.MODE_PRIVATE)
        }
        val name = prefs?.getString(KEY_VARIANT, ThemeVariant.SISTEMA.name)
        variant = ThemeVariant.values().firstOrNull { it.name == name } ?: ThemeVariant.SISTEMA
    }

    /** Cambia y persiste la variante (el widget se repinta desde la pantalla).
     *  Nombre `select` para no chocar con el setter JVM de `variant`. */
    fun select(value: ThemeVariant) {
        if (value == variant) return
        variant = value
        prefs?.edit()?.putString(KEY_VARIANT, value.name)?.apply()
    }

    /** Paleta de una variante en un modo: la usan la app y el widget. */
    fun paletteFor(variant: ThemeVariant, isDark: Boolean): AppColors = when (variant) {
        ThemeVariant.SISTEMA -> if (isDark) DarkAppColors else LightAppColors
        ThemeVariant.AZUL -> if (isDark) AzulDarkColors else AzulLightColors
        ThemeVariant.VERDE -> if (isDark) VerdeDarkColors else VerdeLightColors
        ThemeVariant.ROSADO -> if (isDark) RosadoDarkColors else RosadoLightColors
    }

    private fun aliasClassName(variant: ThemeVariant): String = when (variant) {
        ThemeVariant.SISTEMA -> "com.tasabcv.app.LauncherSistema"
        ThemeVariant.AZUL -> "com.tasabcv.app.LauncherAzul"
        ThemeVariant.VERDE -> "com.tasabcv.app.LauncherVerde"
        ThemeVariant.ROSADO -> "com.tasabcv.app.LauncherRosado"
    }

    /**
     * Sincroniza el ícono del escritorio con la variante activa AL SALIR de la
     * app (MainActivity.onStop): dentro de Ajustes la transición de color nunca
     * se interrumpe y, si el ícono ya coincide, no hace nada (costo cero).
     */
    fun applyIconIfNeeded(context: Context) {
        val pm = context.packageManager
        val pkg = context.packageName
        val target = variant
        val targetComponent = ComponentName(pkg, aliasClassName(target))
        val targetState = pm.getComponentEnabledSetting(targetComponent)
        val isTargetEnabled = targetState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
            (target == ThemeVariant.SISTEMA && targetState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)

        val anyOtherEnabled = ThemeVariant.values().any { candidate ->
            if (candidate == target) return@any false
            val state = pm.getComponentEnabledSetting(ComponentName(pkg, aliasClassName(candidate)))
            state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                (candidate == ThemeVariant.SISTEMA && state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
        }

        if (isTargetEnabled && !anyOtherEnabled) return

        // Primero habilitamos el nuevo alias y luego deshabilitamos los demás:
        // así el launcher siempre ve al menos un ícono válido durante el relevo.
        pm.setComponentEnabledSetting(
            targetComponent,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        ThemeVariant.values().forEach { candidate ->
            if (candidate != target) {
                pm.setComponentEnabledSetting(
                    ComponentName(pkg, aliasClassName(candidate)),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}

/** Muelle de color compartido: mismas físicas que MotionSpring (§10). */
private val MotionSpringColor: SpringSpec<Color> = spring(dampingRatio = 0.9f, stiffness = 350f)

/**
 * Paleta animada: CADA color hace la transición hacia el destino con el muelle
 * compartido (animateColorAsState es el primitivo probado de Compose: arranca
 * SIEMPRE desde el valor visible actual y recomposta por frame). Solo anima
 * durante el cambio de tema; en reposo el valor estático cuesta cero. El
 * arranque de la app pinta directo el destino (sin animación inicial).
 */
@Composable
private fun animatedAppColors(target: AppColors): AppColors {
    val backgroundTop by animateColorAsState(target.backgroundTop, MotionSpringColor, label = "bgTop")
    val backgroundBottom by animateColorAsState(target.backgroundBottom, MotionSpringColor, label = "bgBottom")
    val surfaceTop by animateColorAsState(target.surfaceTop, MotionSpringColor, label = "surfTop")
    val surfaceBottom by animateColorAsState(target.surfaceBottom, MotionSpringColor, label = "surfBottom")
    val insetTop by animateColorAsState(target.insetTop, MotionSpringColor, label = "insetTop")
    val insetBottom by animateColorAsState(target.insetBottom, MotionSpringColor, label = "insetBottom")
    val bevelLight by animateColorAsState(target.bevelLight, MotionSpringColor, label = "bevelLight")
    val bevelDark by animateColorAsState(target.bevelDark, MotionSpringColor, label = "bevelDark")
    val insetBevelLight by animateColorAsState(target.insetBevelLight, MotionSpringColor, label = "insBevL")
    val insetBevelDark by animateColorAsState(target.insetBevelDark, MotionSpringColor, label = "insBevD")
    val shadow by animateColorAsState(target.shadow, MotionSpringColor, label = "shadow")
    val accent by animateColorAsState(target.accent, MotionSpringColor, label = "accent")
    val onSurface by animateColorAsState(target.onSurface, MotionSpringColor, label = "onSurface")
    val muted by animateColorAsState(target.muted, MotionSpringColor, label = "muted")
    val divider by animateColorAsState(target.divider, MotionSpringColor, label = "divider")
    val error by animateColorAsState(target.error, MotionSpringColor, label = "error")
    val deltaUp by animateColorAsState(target.deltaUp, MotionSpringColor, label = "deltaUp")
    val deltaDown by animateColorAsState(target.deltaDown, MotionSpringColor, label = "deltaDown")
    return AppColors(
        backgroundTop = backgroundTop,
        backgroundBottom = backgroundBottom,
        surfaceTop = surfaceTop,
        surfaceBottom = surfaceBottom,
        insetTop = insetTop,
        insetBottom = insetBottom,
        bevelLight = bevelLight,
        bevelDark = bevelDark,
        insetBevelLight = insetBevelLight,
        insetBevelDark = insetBevelDark,
        shadow = shadow,
        accent = accent,
        onSurface = onSurface,
        muted = muted,
        divider = divider,
        error = error,
        deltaUp = deltaUp,
        deltaDown = deltaDown,
        isDark = target.isDark
    )
}

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
    val target = ThemeStore.paletteFor(ThemeStore.variant, darkTheme)
    // Transición de color real: cada color fluye desde donde está hacia el
    // nuevo tema (animateColorAsState, primitivo probado de Compose). En
    // reposo: cero trabajo; el arranque pinta directo sin animación.
    val identityColors = animatedAppColors(target)

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
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content
        )
    }
}
