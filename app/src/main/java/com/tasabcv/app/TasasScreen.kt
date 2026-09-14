package com.tasabcv.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/** Máximo de caracteres del número formateado (sanity check ≤ 1.000.000 → "1.000.000,00"). */
private const val MAX_RATE_CHARS = 12

@Composable
fun TasasScreen(viewModel: DolarViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val context = LocalContext.current
    val colors = appColors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.backgroundTop, colors.backgroundBottom)
                )
            )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TasasTopBar(
                fecha = updatedDate(uiState),
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() }
            )

            Spacer(Modifier.height(24.dp))

            CurrencyCard(
                title = stringResource(R.string.currency_usd_title),
                state = uiState.usdState,
                onCopy = { copyRate(context, it, "USD") },
                onShare = { shareRate(context, it, "USD") },
                onRetry = { viewModel.refresh() },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            CurrencyCard(
                title = stringResource(R.string.currency_eur_title),
                state = uiState.eurState,
                onCopy = { copyRate(context, it, "EUR") },
                onShare = { shareRate(context, it, "EUR") },
                onRetry = { viewModel.refresh() },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}

/** Fecha de la barra superior: la del primer estado Loaded (USD, luego EUR). */
private fun updatedDate(uiState: DolarUiState): String {
    val raw = listOf(uiState.usdState, uiState.eurState)
        .filterIsInstance<SingleRateState.Loaded>()
        .firstOrNull()?.rate?.fechaActualizacion.orEmpty()
    return if (raw.isBlank()) "—" else Format.fecha(raw)
}

/**
 * Barra superior del apartado Tasas: superficie elevada (degradado + bisel +
 * sombra) con esquinas 32dp. Fecha a la izquierda, botón de refrescar a la
 * derecha: círculo elevado que se HUNDE al presionar ([RefreshPressSpring]).
 */
@Composable
private fun TasasTopBar(fecha: String, isRefreshing: Boolean, onRefresh: () -> Unit) {
    val colors = appColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neuRaised(colors, RoundedCornerShape(32.dp), elevation = 10.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.screen_tasas_updated, fecha),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.muted,
            modifier = Modifier.weight(1f)
        )

        val refreshInteractionSource = remember { MutableInteractionSource() }
        val refreshPressed by refreshInteractionSource.collectIsPressedAsState()
        Box(
            modifier = Modifier
                .size(44.dp)
                .neuSurface(colors, CircleShape, refreshPressed, elevation = 6.dp)
                .bounceClick(
                    interactionSource = refreshInteractionSource,
                    pressedScale = 0.85f,
                    springSpec = RefreshPressSpring
                ) {
                    if (!isRefreshing) onRefresh()
                },
            contentAlignment = Alignment.Center
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = colors.accent
                )
            } else {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Actualizar",
                    tint = colors.accent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun CurrencyCard(
    title: String,
    state: SingleRateState,
    onCopy: (DolarRate) -> Unit,
    onShare: (DolarRate) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = appColors
    // Solo hay pie de "Sin conexión" cuando el fetch falló y se muestra caché;
    // sin aviso ese pie se colapsa por completo (sin espacio en blanco) y la
    // zona de tasa crece: el valor y el delta ganan tamaño y bajan.
    val hasWarning = (state as? SingleRateState.Loaded)?.let { it.isFromCache && it.warning != null } ?: false

    // Esqueleto unificado para TODOS los estados: título arriba, zona de
    // tasa expandida (weight 1f), divisor fijo y acciones abajo. El divisor
    // y las acciones jamás se mueven entre Loading/Empty/Loaded ni entre
    // USD y EUR (distribución consistente garantizada).
    Column(
        modifier = modifier
            .neuRaised(colors, RoundedCornerShape(32.dp), elevation = 12.dp)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.muted
        )
        Spacer(Modifier.height(12.dp))

        // Zona de tasa: la MISMA zona visual en los 3 estados (sin saltos al
        // terminar de cargar). El divisor y las acciones quedan fijos abajo.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is SingleRateState.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp,
                            color = colors.accent
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Cargando...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.muted
                        )
                    }
                }
                is SingleRateState.Empty -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = colors.muted
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = state.errorMessage,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is SingleRateState.Loaded -> {
                    val rate = state.rate
                    val formatted = Format.number(rate.promedio)

                    // Sin pie de aviso la zona de tasa crece: el valor sube
                    // un peldaño de tamaño (transición animada, sin saltos).
                    val valueFontSize by animateFloatAsState(
                        targetValue = when {
                            formatted.length <= 8 -> if (hasWarning) 48f else 52f
                            formatted.length <= 10 -> if (hasWarning) 42f else 46f
                            else -> if (hasWarning) 36f else 40f
                        },
                        animationSpec = tween(300),
                        label = "ValueFontSize"
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Odómetro por dígitos: cada carácter rueda de forma
                        // independiente (hacia arriba si sube, hacia abajo si
                        // baja) con desvanecimiento, sin cortes ni superposiciones.
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RollingNumber(
                                value = rate.promedio,
                                fontSize = valueFontSize,
                                color = colors.onSurface
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Bs",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface,
                                modifier = Modifier.padding(bottom = 7.dp)
                            )
                        }

                        // Slot del delta SIEMPRE reservado: el valor queda a la
                        // misma altura en USD y EUR aunque solo uno tenga delta;
                        // el chip entra con fade dentro del slot sin mover nada.
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier.height(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val previous = rate.previousPromedio
                            if (previous != null) {
                                DeltaChip(
                                    current = rate.promedio,
                                    previous = previous,
                                    large = !hasWarning
                                )
                            }
                        }
                    }
                }
            }
        }

        // Divisor fijo: separa la zona de tasa de las acciones; sin aviso
        // queda directamente sobre los botones (sin hueco muerto).
        HorizontalDivider(color = colors.divider)

        // Pie de aviso COLAPSABLE: solo existe cuando hay aviso de "Sin
        // conexión" (fetch falló y se muestra caché). Sin aviso colapsa a
        // cero y el espacio se redistribuye hacia la zona de tasa.
        AnimatedVisibility(
            visible = hasWarning,
            enter = fadeIn(tween(200)) + expandVertically(tween(250)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(250))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.height(22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CardStatusRow(
                        warning = (state as? SingleRateState.Loaded)?.warning ?: "Sin conexión"
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        Spacer(Modifier.height(12.dp))

        // Slot de acciones reservado (48dp): copiar/compartir en Loaded,
        // "Reintentar" en Empty; vacío en Loading (misma posición siempre).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is SingleRateState.Loaded -> {
                    Row(horizontalArrangement = Arrangement.Center) {
                        CardActionButton(
                            icon = Icons.Filled.ContentCopy,
                            contentDescription = "Copiar",
                            onClick = { onCopy(state.rate) }
                        )
                        Spacer(Modifier.width(16.dp))
                        CardActionButton(
                            icon = Icons.Filled.Share,
                            contentDescription = "Compartir",
                            onClick = { onShare(state.rate) }
                        )
                    }
                }
                is SingleRateState.Empty -> {
                    Button(onClick = onRetry) { Text("Reintentar") }
                }
                is SingleRateState.Loading -> Unit
            }
        }
    }
}

/** Botón circular de acción de la ficha: elevado en reposo, hundido al presionar. */
@Composable
private fun CardActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val colors = appColors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .size(48.dp)
            .neuSurface(colors, CircleShape, pressed, elevation = 6.dp)
            .bounceClick(interactionSource, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = colors.accent,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Odómetro: número formateado con slots fijos anclados a la derecha (12, máximo
 * posible por sanity check). Cada carácter se desliza de forma independiente
 * (arriba si el valor sube, abajo si baja) con desvanecimiento dominante y
 * recorte al slot ([Modifier.clipToBounds]): nunca pinta sobre vecinos, el
 * "Bs" ni el delta. Los slots vacíos no ocupan ancho: el número queda centrado
 * aunque cambie la cantidad de dígitos.
 */
@Composable
private fun RollingNumber(value: Double, fontSize: Float, color: Color) {
    val text = Format.number(value)

    var previousValue by remember { mutableStateOf(value) }
    val direction = when {
        value > previousValue -> 1f
        value < previousValue -> -1f
        else -> 0f
    }
    SideEffect { previousValue = value }

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(MAX_RATE_CHARS) { indexFromLeft ->
            val indexFromRight = MAX_RATE_CHARS - 1 - indexFromLeft
            val char = text.getOrNull(text.length - 1 - indexFromRight)
            RollingCharSlot(
                char = char,
                direction = direction,
                fontSize = fontSize,
                color = color
            )
        }
    }
}

@Composable
private fun RollingCharSlot(char: Char?, direction: Float, fontSize: Float, color: Color) {
    val density = LocalDensity.current
    val em = with(density) { fontSize.sp.toDp() }
    // Ancho fijo por slot (dígitos ~0.60em, separadores ~0.30em, vacío 0dp):
    // los dígitos no se desplazan horizontalmente al cambiar y el número queda
    // centrado aunque cambie la cantidad de caracteres.
    val slotWidth = when (char) {
        null -> 0.dp
        '.', ',' -> em * 0.30f
        else -> em * 0.60f
    }

    Box(
        modifier = Modifier
            .width(slotWidth)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = char,
            transitionSpec = {
                val upward = direction >= 0f
                val enterOffset: (Int) -> Int = { height -> if (upward) height else -height }
                val exitOffset: (Int) -> Int = { height -> if (upward) -height else height }
                (slideInVertically(
                    animationSpec = tween(400, easing = FastOutSlowInEasing),
                    initialOffsetY = enterOffset
                ) + fadeIn(tween(250))) togetherWith
                    (slideOutVertically(
                        animationSpec = tween(400, easing = FastOutSlowInEasing),
                        targetOffsetY = exitOffset
                    ) + fadeOut(tween(250)))
            },
            contentKey = { it },
            label = "RollingChar"
        ) { ch ->
            // El espacio transparente mantiene la altura de línea idéntica en
            // slots vacíos: sin cortes verticales ni saltos de layout.
            Text(
                text = (ch ?: ' ').toString(),
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                color = if (ch != null) color else Color.Transparent,
                maxLines = 1
            )
        }
    }
}

/** Chip de variación ▲/▼ con fade. Vive en un slot de altura fija en la tarjeta,
 *  así el valor no cambia de posición cuando aparece o desaparece. Colores de
 *  delta adaptados por tema (claritos en claro, oscuros en oscuro). */
@Composable
private fun DeltaChip(current: Double, previous: Double, large: Boolean) {
    val showDelta = !DolarRepository.sameRate(previous, current)
    AnimatedVisibility(
        visible = showDelta,
        enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.9f, animationSpec = tween(250)),
        exit = fadeOut(tween(150))
    ) {
        val colors = appColors
        val delta = current - previous
        val isPositive = delta > 0
        val color = if (isPositive) colors.deltaUp else colors.deltaDown
        val icon = if (isPositive) "▲" else "▼"
        val sign = if (isPositive) "+" else "-"
        Box(
            modifier = Modifier
                .background(color.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
        ) {
            Text(
                text = "$icon $sign${Format.number(abs(delta))} Bs",
                color = color,
                fontSize = if (large) 17.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = if (large) 5.dp else 6.dp)
            )
        }
    }
}

/** Fila del pie de la tarjeta: solo el aviso de "Sin conexión" (la fecha vive
 *  en la barra superior). Con fade, sin desplazar ningún elemento. */
@Composable
private fun CardStatusRow(warning: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.WifiOff,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = warning,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.error
        )
    }
}
