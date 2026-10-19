package com.tasabcv.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// Motor de relieve 3D "Cristal": volumen vertical estático, cero trabajo por
// frame. La luz cae de arriba hacia abajo:
//   • Superficie con degradado vertical (más iluminada arriba, con cuerpo abajo).
//   • Contorno de 1.5dp en 3 paradas verticales (arista superior iluminada ->
//     costados en el tono medio del propio tema -> base inferior más profunda
//     que da grosor 3D), teñido siempre con la paleta activa (sin bordes
//     blancos y negros ajenos al tema).
//   • Sombra proyectada debajo en los elementos elevados.
//
//   glassRaised -> elemento elevado 3D (tarjetas, barras, botones).
//   glassInset  -> hueco hundido 3D (selector segmentado, campos, banner).
// ---------------------------------------------------------------------------

/** Degradado vertical de superficie elevada (luz arriba -> cuerpo abajo). */
fun raisedBrush(colors: AppColors): Brush =
    Brush.verticalGradient(listOf(colors.surfaceTop, colors.surfaceBottom))

/**
 * Contorno 3D de elemento elevado: brillo en la arista superior, transición del
 * propio tono en los costados y canto inferior más profundo que da relieve.
 */
fun raisedBorder(colors: AppColors): BorderStroke {
    val mid = lerp(colors.bevelLight, colors.bevelDark, 0.5f)
    return BorderStroke(
        1.5.dp,
        Brush.verticalGradient(listOf(colors.bevelLight, mid, colors.bevelDark))
    )
}

/** Degradado vertical de los huecos hundidos (sombra arriba -> base abajo). */
fun insetBrush(colors: AppColors): Brush =
    Brush.verticalGradient(listOf(colors.insetTop, colors.insetBottom))

/**
 * Contorno 3D de hueco hundido: sombra interna arriba, transición en los
 * costados y reflejo suave abajo (relieve invertido).
 */
fun insetBorder(colors: AppColors): BorderStroke {
    val mid = lerp(colors.insetBevelDark, colors.insetBevelLight, 0.5f)
    return BorderStroke(
        1.5.dp,
        Brush.verticalGradient(listOf(colors.insetBevelDark, mid, colors.insetBevelLight))
    )
}

/**
 * Elemento elevado 3D con sombra proyectada. Orden de modifiers: sombra detrás,
 * luego el relleno con volumen vertical y el contorno 3D encima.
 */
fun Modifier.glassRaised(colors: AppColors, shape: Shape, elevation: Dp = 10.dp): Modifier =
    this
        .shadow(elevation, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
        .background(raisedBrush(colors), shape)
        .border(raisedBorder(colors), shape)

/** Hueco hundido 3D (el contorno invertido y el degradado interior dan la profundidad). */
fun Modifier.glassInset(colors: AppColors, shape: Shape): Modifier =
    this
        .background(insetBrush(colors), shape)
        .border(insetBorder(colors), shape)
