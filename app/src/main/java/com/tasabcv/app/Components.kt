package com.tasabcv.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Efecto de presión 3D ligero (escala con spring) + feedback háptico sutil al
 * presionar (tick tipo TextHandleMove: discreto, premium, sin vibración brusca).
 *
 * @param pressedScale escala al presionar (0.94f = hundimiento sutil por defecto).
 * @param springSpec curva del movimiento; siempre un muelle compartido (v4.6.2).
 */
fun Modifier.bounceClick(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.94f,
    springSpec: SpringSpec<Float> = MotionSpring,
    onClick: () -> Unit
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = springSpec,
        label = "BounceScale"
    )

    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

// ---------------------------------------------------------------------------
// Lenguaje de movimiento ÚNICO (v4.6.2): toda la app comparte estos muelles.
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
 * Control segmentado neumorfista (v4.5.0): el contenedor es un HUECO hundido
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
            fontWeight = FontWeight.SemiBold,
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
            .neuInset(colors, containerShape)
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
                    .neuRaised(colors, indicatorShape, elevation = 6.dp)
            )

            // Etiquetas por encima del indicador.
            Row(modifier = Modifier.fillMaxSize()) {
                options.forEachIndexed { index, text ->
                    val isSelected = index == selectedIndex
                    val interactionSource = remember { MutableInteractionSource() }

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) colors.accent else colors.muted,
                        animationSpec = tween(200),
                        label = "SegmentText"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .bounceClick(interactionSource) { onOptionSelected(index) },
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
    cornerRadius: Dp = 32.dp
) {
    SlidingSegmentedControl(
        options = options,
        selectedIndex = selectedIndex,
        onOptionSelected = onOptionSelected,
        modifier = modifier,
        height = 56.dp,
        cornerRadius = cornerRadius
    )
}
