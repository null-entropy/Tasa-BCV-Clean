package com.tasabcv.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// Motor de relieve "Grafito": neumorfismo pronunciado SIN blur.
// Todo el 3D se resuelve con degradados diagonales + biseles (borde con
// degradado luz->sombra) + sombras de elevación estándar: costo GPU mínimo.
//
//   neuRaised  -> elemento elevado: luz arriba-izquierda, sombra abajo-derecha.
//                 Superficies SIEMPRE estáticas en su forma: la respuesta al
//                 toque la pone pulseClick (pulso one-shot de escala), no el
//                 relieve (sin estados que puedan quedar colgados).
//   neuInset   -> elemento hundido: bisel invertido (pozo), sin sombra.
// ---------------------------------------------------------------------------

/** Degradado diagonal de las superficies elevadas (luz -> sombra). */
fun raisedBrush(colors: AppColors): Brush =
    Brush.linearGradient(listOf(colors.surfaceTop, colors.surfaceBottom))

/** Bisel de elemento elevado: arista de luz arriba-izq, sombra abajo-der. */
fun raisedBorder(colors: AppColors): BorderStroke =
    BorderStroke(1.dp, Brush.linearGradient(listOf(colors.bevelLight, colors.bevelDark)))

/** Degradado diagonal de los huecos hundidos. */
fun insetBrush(colors: AppColors): Brush =
    Brush.linearGradient(listOf(colors.insetTop, colors.insetBottom))

/** Bisel de hueco hundido: sombra arriba-izq, luz abajo-der (invertido). */
fun insetBorder(colors: AppColors): BorderStroke =
    BorderStroke(1.dp, Brush.linearGradient(listOf(colors.insetBevelDark, colors.insetBevelLight)))

/**
 * Superficie elevada con sombra proyectada. Orden de modifiers: sombra detrás,
 * luego el degradado y el bisel encima (el borde pinta sobre el borde del fill).
 */
fun Modifier.neuRaised(colors: AppColors, shape: Shape, elevation: Dp = 10.dp): Modifier =
    this
        .shadow(elevation, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
        .background(raisedBrush(colors), shape)
        .border(raisedBorder(colors), shape)

/** Hueco hundido (sin sombra proyectada: el bisel invertido hace el trabajo). */
fun Modifier.neuInset(colors: AppColors, shape: Shape): Modifier =
    this
        .background(insetBrush(colors), shape)
        .border(insetBorder(colors), shape)
