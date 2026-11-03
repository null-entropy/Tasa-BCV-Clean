package com.tasabcv.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Pulso de presión ONE-SHOT: al tocar, el elemento hace una pequeña
 * animación ACOTADA — encoge hasta [pressedScale] y regresa SIEMPRE a 1f exacto
 * (dos animateTo con el muelle compartido). NO existe estado "presionado"
 * persistente que apagar: la familia de bugs b/414319919 y b/186223077 de
 * foundation 1.6.8 (toques rápidos, presionar+arrastrar) ya no tiene superficie
 * sobre la que colgarse — el peor caso posible es que el pulso se reinicie,
 * nunca que quede congelado. Si el nodo se elimina, un nuevo nodo arranca en
 * 1f exacto (auto-reparación).
 *
 * Detección del tap con [Modifier.clickable] estándar (cancelación por scroll
 * y arrastre correcta por diseño, semántica de accesibilidad incluida). Su
 * fuente de interacciones NO se observa (indication = null): aunque sus
 * emisiones internas fallaran, el visual NO depende de ellas.
 *
 * Cero trabajo por frame: [graphicsLayer] con lambda lee el valor solo al
 * dibujar (sin recomposiciones durante el pulso); costo nulo en reposo.
 *
 * @param pressedScale escala mínima del pulso (0.94f por defecto).
 * @param springSpec muelle compartido; siempre [MotionSpring].
 */
fun Modifier.pulseClick(
    pressedScale: Float = 0.94f,
    springSpec: SpringSpec<Float> = MotionSpring,
    onClick: () -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val currentOnClick by rememberUpdatedState(onClick)
    val pulse = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    this
        .graphicsLayer {
            scaleX = pulse.value
            scaleY = pulse.value
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            // Animatable serializa las animaciones: un nuevo pulso cancela el
            // anterior y arranca desde el valor actual; toda secuencia termina
            // en exactamente 1f.
            scope.launch {
                pulse.animateTo(pressedScale, springSpec)
                pulse.animateTo(1f, springSpec)
            }
            currentOnClick()
        }
}

// ---------------------------------------------------------------------------
// Lenguaje de movimiento ÚNICO: toda la app comparte estos muelles.
//   MotionSpring  -> TODO movimiento (indicador deslizante, carrusel USD↔EUR,
//                    transición de tabs, odómetro, tamaño del valor, presiones):
//                    fluido, llega rápido y asienta sin sobresalto (damping
//                    0.9 ≈ sin rebote visible; nunca sale de su burbuja).
//   PopSpring     -> entradas "pop" (delta 0.55→1, mensajes de pill 0.85→1):
//                    rebote mínimo y contenido.
//   Los fades acompañan con tweens cortos (140–200 ms). Prohibido añadir
//   springs ad-hoc fuera de este par (mandato de optimización §1).
// ---------------------------------------------------------------------------
val MotionSpring: SpringSpec<Float> = spring(dampingRatio = 0.9f, stiffness = 350f)
val MotionSpringIntOffset: SpringSpec<IntOffset> = spring(dampingRatio = 0.9f, stiffness = 350f)
val PopSpring: SpringSpec<Float> = spring(dampingRatio = 0.75f, stiffness = 420f)

/**
 * Control segmentado neumorfista: el contenedor es un HUECO hundido
 * (bisel invertido, sin sombra) y el indicador es una PÍLDORA elevada que se
 * desliza con spring (degradado + bisel + sombra proyectada). Reutilizado por
 * la barra inferior y el selector USD/EUR: un solo lenguaje de forma/movimiento.
 */
@Composable
fun SlidingSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    cornerRadius: Dp = height / 2,
    content: @Composable (option: String, isSelected: Boolean, contentColor: Color) -> Unit = { text, _, color ->
        Text(
            text = text,
            color = color,
            fontSize = 14f.boundedSp(maxScale = 1.12f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            style = MaterialTheme.typography.labelLarge
        )
    }
) {
    val colors = appColors
    val containerShape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .glassInset(colors, containerShape)
            // Recorta el indicador a la silueta redondeada (el spring puede
            // empujarlo hacia las esquinas en el overshoot).
            .clip(containerShape)
            .padding(4.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val count = options.size.coerceAtLeast(1)
            val segmentWidth = maxWidth / count
            val indicatorShape = RoundedCornerShape((cornerRadius - 4.dp).coerceAtLeast(0.dp))

            // Fracción 0..(count-1) animada; el indicador desliza con el
            // muelle compartido: fluido y SIEMPRE dentro de la burbuja.
            val fraction by animateFloatAsState(
                targetValue = selectedIndex.toFloat(),
                animationSpec = MotionSpring,
                label = "SegmentIndicator"
            )

            // Píldora elevada que se desliza: degradado + bisel + sombra.
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(segmentWidth)
                    .offset {
                        IntOffset(
                            x = (segmentWidth.toPx() * fraction).roundToInt(),
                            y = 0
                        )
                    }
                    .glassRaised(colors, indicatorShape, elevation = 6.dp)
            )

            // Etiquetas por encima del indicador.
            Row(modifier = Modifier.fillMaxSize()) {
                options.forEachIndexed { index, text ->
                    val isSelected = index == selectedIndex

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) colors.accent else colors.muted,
                        animationSpec = tween(200),
                        label = "SegmentText"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pulseClick { onOptionSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        content(text, isSelected, contentColor)
                    }
                }
            }
        }
    }
}

/**
 * Selector USD/EUR de la calculadora: usa el indicador deslizante compartido.
 * Esquinas redondeadas como las fichas de tasas (32dp, lenguaje visual unificado).
 */
@Composable
fun FloatingSegmentedButton(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    cornerRadius: Dp = 32.dp
) {
    SlidingSegmentedControl(
        options = options,
        selectedIndex = selectedIndex,
        onOptionSelected = onOptionSelected,
        modifier = modifier,
        height = height,
        cornerRadius = cornerRadius
    )
}

// ---------------------------------------------------------------------------
// Marco y escala responsiva compartidos por las 3 pestañas (Ajustes, Tasas y
// Calculadora): cero desplazamiento, márgenes exteriores idénticos y textos
// acotados al alto real disponible (nunca se desbordan con letra grande).
// ---------------------------------------------------------------------------

/** Ancho máximo del contenido en pantallas anchas para evitar estiramiento. */
val MaxContentWidth: Dp = 560.dp

/** Margen horizontal de las fichas en las 3 pantallas (24.dp). */
val ScreenHorizontalMargin: Dp = 24.dp

/** Margen vertical entre fichas en las 3 pantallas (20.dp). */
val ScreenVerticalGap: Dp = 20.dp

/** Margen horizontal exterior idéntico en las 3 pantallas según el ancho real. */
fun screenOuterPaddingH(maxWidth: Dp): Dp = when {
    maxWidth < 340.dp -> 16.dp
    maxWidth < 370.dp -> 20.dp
    else -> ScreenHorizontalMargin
}

/**
 * Margen vertical exterior idéntico en las 3 pantallas (12.dp en pantallas
 * normales: sumado a los 8.dp superiores de la barra inferior forma
 * exactamente los 20.dp de ScreenVerticalGap entre la última ficha y la barra).
 */
fun screenOuterPaddingV(maxHeight: Dp): Dp = when {
    maxHeight < 480.dp -> 8.dp
    maxHeight < 580.dp -> 10.dp
    else -> 12.dp
}

/** Separación vertical entre fichas idéntica en las 3 pantallas (20.dp). */
fun screenSectionGap(maxHeight: Dp): Dp = when {
    maxHeight < 400.dp -> 8.dp
    maxHeight < 500.dp -> 12.dp
    maxHeight < 580.dp -> 16.dp
    else -> ScreenVerticalGap
}

/**
 * Convierte un tamaño base a [TextUnit] permitiendo que crezca con la letra
 * de Accesibilidad del sistema solo hasta [maxScale] (calculado según el alto
 * libre de la pantalla): evita que las pantallas fijas sin scroll se corten.
 */
@Composable
fun Float.boundedSp(maxScale: Float = 1.15f): TextUnit {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(0.85f)
    val effectiveScale = fontScale.coerceAtMost(maxScale.coerceAtLeast(1f))
    return ((this * effectiveScale) / fontScale).sp
}

@Composable
fun Int.boundedSp(maxScale: Float = 1.15f): TextUnit = toFloat().boundedSp(maxScale)
