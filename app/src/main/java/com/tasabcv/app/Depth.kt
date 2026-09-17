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
// Motor de relieve "Grafito" (v4.5.0): neumorfismo pronunciado SIN blur.
// Todo el 3D se resuelve con degradados diagonales + biseles (borde con
// degradado luz->sombra) + sombras de elevación estándar: costo GPU mínimo.
//
//   neuRaised  -> elemento elevado: luz arriba-izquierda, sombra abajo-derecha.
//   neuInset   -> elemento hundido: bisel invertido (pozo), sin sombra.
//   neuSurface -> alterna elevado/hundido según `pressed` (botones que se
//                 hunden de verdad al presionar).
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

/**
 * Superficie interactiva: elevada en reposo, hundida al presionar. El swap de
 * bisel + el scale de [bounceClick] dan la sensación física de "pulsar".
 * Uso también en superficies grandes (fichas de tasas): costo por presión =
 * un solo intercambio de modificadores (sin trabajo de dibujo por frame).
 */
fun Modifier.neuSurface(colors: AppColors, shape: Shape, pressed: Boolean, elevation: Dp = 8.dp): Modifier =
    if (pressed) neuInset(colors, shape) else neuRaised(colors, shape, elevation)
