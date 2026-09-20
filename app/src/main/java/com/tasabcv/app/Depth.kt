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
// Motor de relieve "Cristal": VIDRIO estático, cero trabajo por frame.
// El efecto se logra con: relleno translúcido (la alfa de las paletas es el
// vidrio), degradado diagonal sutil y bisel luminoso de 1px (arista de luz
// arriba-izq, sombra abajo-der). Sin desenfoque (RenderEffect), que sería
// costoso y solo API 31+: visualmente equivalente sobre fondos planos.
//
//   glassRaised -> elemento elevado: cristal + arista luminosa + sombra suave.
//                 Superficies SIEMPRE estáticas en su forma: la respuesta al
//                 toque la pone pulseClick (pulso one-shot de escala).
//   glassInset  -> elemento hundido: bisel invertido (pozo), sin sombra.
// ---------------------------------------------------------------------------

/** Degradado diagonal del cristal elevado (luz -> sombra). */
fun raisedBrush(colors: AppColors): Brush =
    Brush.linearGradient(listOf(colors.surfaceTop, colors.surfaceBottom))

/** Bisel de cristal elevado: arista de luz arriba-izq, sombra abajo-der. */
fun raisedBorder(colors: AppColors): BorderStroke =
    BorderStroke(1.dp, Brush.linearGradient(listOf(colors.bevelLight, colors.bevelDark)))

/** Degradado diagonal de los huecos hundidos. */
fun insetBrush(colors: AppColors): Brush =
    Brush.linearGradient(listOf(colors.insetTop, colors.insetBottom))

/** Bisel de hueco hundido: sombra arriba-izq, luz abajo-der (invertido). */
fun insetBorder(colors: AppColors): BorderStroke =
    BorderStroke(1.dp, Brush.linearGradient(listOf(colors.insetBevelDark, colors.insetBevelLight)))

/**
 * Cristal elevado con sombra proyectada. Orden de modifiers: sombra detrás,
 * luego el relleno translúcido y el bisel encima (el borde pinta sobre el
 * borde del fill).
 */
fun Modifier.glassRaised(colors: AppColors, shape: Shape, elevation: Dp = 10.dp): Modifier =
    this
        .shadow(elevation, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
        .background(raisedBrush(colors), shape)
        .border(raisedBorder(colors), shape)

/** Hueco hundido (sin sombra proyectada: el bisel invertido hace el trabajo). */
fun Modifier.glassInset(colors: AppColors, shape: Shape): Modifier =
    this
        .background(insetBrush(colors), shape)
        .border(insetBorder(colors), shape)
