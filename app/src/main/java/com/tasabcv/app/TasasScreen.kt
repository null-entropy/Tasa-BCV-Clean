package com.tasabcv.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
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
                isRefreshing = isRefreshing,
                onCopy = { copyRate(context, it, "USD") },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            CurrencyCard(
                title = stringResource(R.string.currency_eur_title),
                state = uiState.eurState,
                isRefreshing = isRefreshing,
                onCopy = { copyRate(context, it, "EUR") },
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
 * derecha: círculo elevado que se HUNDE al presionar ([MotionSpring]).
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

        Box(
            modifier = Modifier
                .size(44.dp)
                .neuRaised(colors, CircleShape, elevation = 6.dp)
                .pulseClick(pressedScale = 0.85f) {
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

/**
 * Ficha de divisa INTERACTIVA: la ficha entera actúa como botón que copia la
 * tasa. Al tocar hace un pulso one-shot ([pulseClick]: sin estado presionado
 * persistente) y al soltar se copia el monto (Toast + destello en la pill del
 * pie). Solo es presionable con tasa cargada (Loaded); en Loading/Empty queda
 * elevada estática.
 *
 * Esqueleto unificado para TODOS los estados: título arriba, zona de tasa
 * expandida (weight 1f), divisor fijo y pie de estado abajo. El divisor y el
 * pie jamás se mueven entre Loading/Empty/Loaded (distribución consistente).
 */
@Composable
fun CurrencyCard(
    title: String,
    state: SingleRateState,
    isRefreshing: Boolean,
    onCopy: (DolarRate) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = appColors

    // Destello "Tasa copiada" en la pill del pie: dura ~1.2 s y regresa sola
    // al estado de conexión correspondiente.
    var flashKey by remember { mutableStateOf(0) }
    var showFlash by remember { mutableStateOf(false) }
    LaunchedEffect(flashKey) {
        if (flashKey > 0) {
            showFlash = true
            delay(1_200)
            showFlash = false
        }
    }

    Column(
        modifier = modifier
            .neuRaised(colors, RoundedCornerShape(32.dp), elevation = 12.dp)
            .then(
                if (state is SingleRateState.Loaded) {
                    // Pulso one-shot al tocar: sin estado presionado
                    // persistente, nada que pueda quedar colgado.
                    Modifier.pulseClick(pressedScale = 0.97f) {
                        onCopy(state.rate)
                        flashKey++
                    }
                } else {
                    Modifier
                }
            )
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
        // terminar de cargar). El divisor y el pie quedan fijos abajo.
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

                    val valueFontSize by animateFloatAsState(
                        targetValue = when {
                            formatted.length <= 8 -> 52f
                            formatted.length <= 10 -> 46f
                            else -> 40f
                        },
                        animationSpec = MotionSpring,
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

                        // Slot del delta SIEMPRE reservado y ocupado: el valor
                        // queda a la misma altura en USD y EUR en cualquier caso.
                        Spacer(Modifier.height(10.dp))
                        DeltaSlot(current = rate.promedio, previous = rate.previousPromedio)
                    }
                }
            }
        }

        // Divisor fijo: separa la zona de tasa del pie de estado.
        HorizontalDivider(color = colors.divider)
        Spacer(Modifier.height(12.dp))

        // Pie SIEMPRE presente con la pill de estado de conexión
        // PERFECTAMENTE centrada en TODOS los estados. El reintento vive en el
        // círculo de refrescar de la barra superior (siempre visible) y en
        // onAppResumed al volver a la app.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            StatusPill(
                status = connectionStatus(state, isRefreshing),
                flash = showFlash
            )
        }
    }
}

/** Estados de conexión con el proveedor que pinta la pill del pie. */
private enum class ConnectionStatus {
    /** Dato fresco descargado del proveedor (o caché reciente y sana): todo OK. */
    Online,

    /** Caché visible mientras la descarga en curso aún no termina. */
    Updating,

    /** Sin caché que mostrar; la primera descarga está en curso. */
    Connecting,

    /** Fetch falló y se muestra el último dato guardado. */
    OfflineCached,

    /** Sin conexión y sin caché que mostrar. */
    Offline
}

/** Estado de conexión con el proveedor, derivado del estado de la moneda. */
private fun connectionStatus(state: SingleRateState, isRefreshing: Boolean): ConnectionStatus =
    when (state) {
        is SingleRateState.Loading -> ConnectionStatus.Connecting
        is SingleRateState.Empty -> ConnectionStatus.Offline
        is SingleRateState.Loaded -> when {
            state.isFromCache && state.warning != null -> ConnectionStatus.OfflineCached
            state.isFromCache && isRefreshing -> ConnectionStatus.Updating
            else -> ConnectionStatus.Online
        }
    }

/**
 * Pill del pie (donde vivían los botones copiar/compartir): HERMANA VISUAL del
 * chip del delta — fondo translúcido 10% + borde 28% + esquinas 16dp — con el
 * color según estado (verde actualizado, rojo sin conexión, neutro conectando,
 * acento al destellar "Tasa copiada"). El color ES el estado: sin punto
 * separado. Al copiar destella y regresa sola al estado correspondiente.
 *
 * UN solo AnimatedContent maneja todos los cambios (estado ↔ estado y estado ↔
 * destello): cuando el mensaje no cambia no anima nada (targetState igual);
 * las transiciones usan los muelles compartidos, sin capas ni loops.
 */
@Composable
private fun StatusPill(status: ConnectionStatus, flash: Boolean) {
    val colors = appColors
    val content = pillContent(status, flash)
    val pillColor = when (content) {
        PillContent.CopyFlash -> colors.accent
        is PillContent.Status -> when (content.status) {
            ConnectionStatus.Online -> colors.deltaDown
            ConnectionStatus.Updating, ConnectionStatus.Connecting -> colors.muted
            ConnectionStatus.OfflineCached, ConnectionStatus.Offline -> MaterialTheme.colorScheme.error
        }
    }
    Box(
        modifier = Modifier
            .background(pillColor.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .border(1.dp, pillColor.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = content,
            transitionSpec = {
                (fadeIn(tween(180)) +
                    slideInVertically(MotionSpringIntOffset) { it / 3 } +
                    scaleIn(PopSpring, initialScale = 0.85f)) togetherWith
                    (fadeOut(tween(140)) + scaleOut(targetScale = 0.92f, animationSpec = tween(140)))
            },
            label = "StatusPill"
        ) { pill ->
            when (pill) {
                PillContent.CopyFlash -> CopyFlashRow()
                is PillContent.Status -> StatusRow(pill.status)
            }
        }
    }
}

/** Contenido lógico de la pill: destello de copia o estado de conexión. */
private sealed interface PillContent {
    data object CopyFlash : PillContent
    data class Status(val status: ConnectionStatus) : PillContent
}

private fun pillContent(status: ConnectionStatus, flash: Boolean): PillContent =
    if (flash) PillContent.CopyFlash else PillContent.Status(status)

/** Mensaje del destello: "Tasa copiada" con check, en color de acento. */
@Composable
private fun CopyFlashRow() {
    val colors = appColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = colors.accent
        )
        Spacer(Modifier.width(5.dp))
        Text(
            "Tasa copiada",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.accent
        )
    }
}

/** Mensajes de estado de conexión: el color del texto replica el del chip. */
@Composable
private fun StatusRow(status: ConnectionStatus) {
    val colors = appColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (status) {
            ConnectionStatus.Online -> {
                StatusText("Actualizado", color = colors.deltaDown)
            }
            ConnectionStatus.Updating -> {
                StatusText("Actualizando…", color = colors.muted)
            }
            ConnectionStatus.Connecting -> {
                StatusText("Conectando…", color = colors.muted)
            }
            ConnectionStatus.OfflineCached -> {
                StatusIcon(icon = Icons.Filled.WifiOff, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(5.dp))
                StatusText("Sin conexión; mostrando caché", color = MaterialTheme.colorScheme.error)
            }
            ConnectionStatus.Offline -> {
                StatusIcon(icon = Icons.Filled.WifiOff, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(5.dp))
                StatusText("Sin conexión; sin caché", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun StatusText(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}

/** Ícono de estado pequeño (WifiOff): siempre rojo de error. */
@Composable
private fun StatusIcon(icon: ImageVector, tint: Color) {
    Icon(
        icon,
        contentDescription = null,
        modifier = Modifier.size(14.dp),
        tint = tint
    )
}

/**
 * Slot del delta con altura fija SIEMPRE ocupado: chip de flecha de tendencia
 * (subida/bajada) con la diferencia cuando hay valor previo distinto, o ficha fantasma ("—") neutra con las
 * MISMAS medidas cuando no hay dato con qué comparar. Nunca queda un hueco.
 * La animación solo corre durante la transición: en reposo el chip es
 * estático (cero costo continuo).
 */
@Composable
private fun DeltaSlot(current: Double, previous: Double?) {
    Box(
        modifier = Modifier.height(36.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = previous != null && !DolarRepository.sameRate(previous, current),
            transitionSpec = {
                if (targetState) {
                    // Neutro → valor: POP de entrada (0.55 → 1 con rebote
                    // contenido de PopSpring), el mismo peso del resto de la app.
                    (fadeIn(tween(180)) + scaleIn(
                        initialScale = 0.55f,
                        animationSpec = PopSpring
                    )) togetherWith
                        (fadeOut(tween(150)) + scaleOut(targetScale = 0.9f, animationSpec = tween(150)))
                } else {
                    // Valor → neutro: retirada suave con la misma lengua.
                    (fadeIn(tween(200)) + scaleIn(initialScale = 0.9f, animationSpec = MotionSpring)) togetherWith
                        (fadeOut(tween(150)) + scaleOut(targetScale = 0.9f, animationSpec = tween(150)))
                }
            },
            label = "DeltaSlot"
        ) { hasDelta ->
            if (hasDelta) {
                DeltaChip(current = current, previous = previous ?: current)
            } else {
                GhostDeltaChip()
            }
        }
    }
}

/**
 * Chip real de variación: flecha de tendencia (gráfica) + diferencia.
 * Rojo para subida, verde para bajada, colores adaptados por tema
 * (claritos en claro, oscuros en oscuro).
 */
@Composable
private fun DeltaChip(current: Double, previous: Double) {
    val colors = appColors
    val delta = current - previous
    val isPositive = delta > 0
    val color = if (isPositive) colors.deltaUp else colors.deltaDown
    val trendIcon = if (isPositive) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown
    val sign = if (isPositive) "+" else "-"
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Icon(
                trendIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "$sign${Format.number(abs(delta))} Bs",
                color = color,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Ficha fantasma: idéntica al chip real en forma y tamaño (padding, esquinas
 * y peso de texto) pero neutra y SIN datos — indica "sin valor previo con
 * qué comparar" ocupando exactamente su lugar.
 */
@Composable
private fun GhostDeltaChip() {
    val colors = appColors
    Box(
        modifier = Modifier
            .background(colors.muted.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .border(1.dp, colors.muted.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
    ) {
        Text(
            text = "—",
            color = colors.muted,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
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
                    animationSpec = MotionSpringIntOffset,
                    initialOffsetY = enterOffset
                ) + fadeIn(tween(200))) togetherWith
                    (slideOutVertically(
                        animationSpec = MotionSpringIntOffset,
                        targetOffsetY = exitOffset
                    ) + fadeOut(tween(200)))
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
