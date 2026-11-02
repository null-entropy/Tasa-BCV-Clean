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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    val fecha = remember(uiState.usdState, uiState.eurState) { updatedDate(uiState) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.backgroundTop, colors.backgroundBottom)
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = MaxContentWidth)
        ) {
            val outerH = screenOuterPaddingH(maxWidth)
            val outerV = screenOuterPaddingV(maxHeight)
            val gap = screenSectionGap(maxHeight)
            val isCompactScreen = maxHeight < 540.dp
            val topBarH = if (isCompactScreen) 50.dp else 60.dp
            val cardW = (maxWidth - outerH * 2).coerceAtLeast(200.dp)
            val cardH = ((maxHeight - outerV * 2 - topBarH - gap * 2) / 2).coerceAtLeast(140.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = outerH, vertical = outerV),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TasasTopBar(
                    fecha = fecha,
                    isRefreshing = isRefreshing,
                    compact = isCompactScreen,
                    onRefresh = { viewModel.refresh() }
                )

                // Los 2 huecos entre los 3 bloques (barra superior, tarjeta USD
                // y tarjeta EUR) miden exactamente lo mismo.
                Spacer(Modifier.height(gap))

                CurrencyCard(
                    title = stringResource(R.string.currency_usd_title),
                    state = uiState.usdState,
                    isRefreshing = isRefreshing,
                    cardW = cardW,
                    cardH = cardH,
                    onCopy = { copyRate(context, it, "USD") },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                Spacer(Modifier.height(gap))

                CurrencyCard(
                    title = stringResource(R.string.currency_eur_title),
                    state = uiState.eurState,
                    isRefreshing = isRefreshing,
                    cardW = cardW,
                    cardH = cardH,
                    onCopy = { copyRate(context, it, "EUR") },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
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
private fun TasasTopBar(
    fecha: String,
    isRefreshing: Boolean,
    compact: Boolean,
    onRefresh: () -> Unit
) {
    val colors = appColors
    val buttonSize = if (compact) 38.dp else 44.dp
    val padV = if (compact) 6.dp else 8.dp
    val padH = if (compact) 16.dp else 20.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassRaised(colors, RoundedCornerShape(32.dp), elevation = 10.dp)
            .padding(horizontal = padH, vertical = padV),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.screen_tasas_updated, fecha),
            fontSize = 14f.boundedSp(maxScale = if (compact) 1.0f else 1.15f),
            fontWeight = FontWeight.Medium,
            color = colors.muted,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .size(buttonSize)
                .glassRaised(colors, CircleShape, elevation = 6.dp)
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
                    modifier = Modifier.size(if (compact) 20.dp else 22.dp)
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
 * Esqueleto unificado y 100% responsivo para TODOS los estados: título arriba,
 * zona de tasa expandida (weight 1f), divisor fijo y pie de estado abajo.
 * Adapta sus alturas y tipografía según el espacio real de la tarjeta para que
 * nada se corte ni encime en ningún tamaño de teléfono.
 */
@Composable
fun CurrencyCard(
    title: String,
    state: SingleRateState,
    isRefreshing: Boolean,
    cardW: Dp,
    cardH: Dp,
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

    val cardPadV = when {
        cardH >= 250.dp -> 20.dp
        cardH >= 210.dp -> 14.dp
        else -> 10.dp
    }
    val cardPadH = if (cardW < 290.dp) 12.dp else 16.dp
    val titleSize = when {
        cardH >= 250.dp -> 18f
        cardH >= 210.dp -> 16f
        else -> 15f
    }
    val titleGap = when {
        cardH >= 250.dp -> 12.dp
        cardH >= 210.dp -> 8.dp
        else -> 4.dp
    }
    val deltaGap = when {
        cardH >= 250.dp -> 10.dp
        cardH >= 210.dp -> 6.dp
        else -> 4.dp
    }
    val deltaSlotHeight = if (cardH >= 220.dp) 36.dp else 30.dp
    val deltaFontSize = if (cardH >= 220.dp) 17f else 14.5f
    val dividerGap = when {
        cardH >= 250.dp -> 12.dp
        cardH >= 210.dp -> 8.dp
        else -> 6.dp
    }
    val footerHeight = when {
        cardH >= 250.dp -> 48.dp
        cardH >= 210.dp -> 38.dp
        else -> 32.dp
    }
    val maxTextScale = if (cardH >= 260.dp) 1.15f else 1.0f

    Column(
        modifier = modifier
            .glassRaised(colors, RoundedCornerShape(32.dp), elevation = 12.dp)
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
            .padding(horizontal = cardPadH, vertical = cardPadV),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = titleSize.boundedSp(maxTextScale),
            fontWeight = FontWeight.SemiBold,
            color = colors.muted,
            maxLines = 1
        )
        Spacer(Modifier.height(titleGap))

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
                    val spinnerSize = if (cardH < 210.dp) 28.dp else 36.dp
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(spinnerSize),
                            strokeWidth = 3.dp,
                            color = colors.accent
                        )
                        Spacer(Modifier.height(if (cardH < 210.dp) 8.dp else 14.dp))
                        Text(
                            "Cargando...",
                            fontSize = 14f.boundedSp(maxTextScale),
                            fontWeight = FontWeight.Medium,
                            color = colors.muted,
                            maxLines = 1
                        )
                    }
                }
                is SingleRateState.Empty -> {
                    val iconSize = if (cardH < 210.dp) 34.dp else 48.dp
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                            tint = colors.muted
                        )
                        Spacer(Modifier.height(if (cardH < 210.dp) 8.dp else 14.dp))
                        Text(
                            text = state.errorMessage,
                            fontSize = 14f.boundedSp(maxTextScale),
                            color = colors.error,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
                is SingleRateState.Loaded -> {
                    val rate = state.rate
                    val formatted = remember(rate.promedio) { Format.number(rate.promedio) }

                    val baseSize = when {
                        formatted.length <= 6 -> 52f
                        formatted.length <= 8 -> 44f
                        formatted.length <= 9 -> 40f
                        else -> 34f
                    }
                    // Tope por ancho interior de la tarjeta: dígitos (0.60em),
                    // separadores (0.30em) + espacio + "Bs" (~0.72em).
                    val innerWidthDp = (cardW - cardPadH * 2).value
                    val seps = formatted.count { it == '.' || it == ',' }
                    val digits = formatted.length - seps
                    val totalEmUnits = digits * 0.60f + seps * 0.30f + 0.72f
                    val maxByWidth = ((innerWidthDp - 8f) / totalEmUnits).coerceAtLeast(20f)

                    // Tope por alto disponible en la zona central de la tarjeta.
                    val centerHeightDp =
                        (cardH - cardPadV * 2 - 22.dp - titleGap - dividerGap - footerHeight).value
                    val maxByHeight =
                        ((centerHeightDp - deltaSlotHeight.value - deltaGap.value) * 0.72f).coerceAtLeast(22f)

                    val targetFontSize = minOf(baseSize, maxByWidth, maxByHeight)

                    val valueFontSize by animateFloatAsState(
                        targetValue = targetFontSize,
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
                                text = formatted,
                                fontSize = valueFontSize,
                                color = colors.onSurface
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Bs",
                                fontSize = (valueFontSize * 0.5f).boundedSp(1f),
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(bottom = (valueFontSize * 0.135f).dp)
                            )
                        }

                        // Slot del delta SIEMPRE reservado y ocupado: el valor
                        // queda a la misma altura en USD y EUR en cualquier caso.
                        Spacer(Modifier.height(deltaGap))
                        DeltaSlot(
                            current = rate.promedio,
                            previous = rate.previousPromedio,
                            slotHeight = deltaSlotHeight,
                            fontSize = deltaFontSize
                        )
                    }
                }
            }
        }

        // Divisor fijo: separa la zona de tasa del pie de estado.
        HorizontalDivider(color = colors.divider)
        Spacer(Modifier.height(dividerGap))

        // Pie SIEMPRE presente con la pill de estado de conexión
        // PERFECTAMENTE centrada en TODOS los estados.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(footerHeight),
            contentAlignment = Alignment.Center
        ) {
            StatusPill(
                status = connectionStatus(state, isRefreshing),
                flash = showFlash,
                maxFontScale = maxTextScale
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
private fun StatusPill(status: ConnectionStatus, flash: Boolean, maxFontScale: Float) {
    val colors = appColors
    val content = pillContent(status, flash)
    val pillColor = when (content) {
        PillContent.CopyFlash -> colors.accent
        is PillContent.Status -> when (content.status) {
            ConnectionStatus.Online -> colors.deltaDown
            ConnectionStatus.Updating, ConnectionStatus.Connecting -> colors.muted
            ConnectionStatus.OfflineCached, ConnectionStatus.Offline -> colors.error
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
                PillContent.CopyFlash -> CopyFlashRow(maxFontScale)
                is PillContent.Status -> StatusRow(pill.status, maxFontScale)
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
private fun CopyFlashRow(maxFontScale: Float) {
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
            fontSize = 12f.boundedSp(maxFontScale),
            fontWeight = FontWeight.SemiBold,
            color = colors.accent,
            maxLines = 1
        )
    }
}

/** Mensajes de estado de conexión: el color del texto replica el del chip. */
@Composable
private fun StatusRow(status: ConnectionStatus, maxFontScale: Float) {
    val colors = appColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (status) {
            ConnectionStatus.Online -> {
                StatusText("Actualizado", color = colors.deltaDown, maxFontScale = maxFontScale)
            }
            ConnectionStatus.Updating -> {
                StatusText("Actualizando…", color = colors.muted, maxFontScale = maxFontScale)
            }
            ConnectionStatus.Connecting -> {
                StatusText("Conectando…", color = colors.muted, maxFontScale = maxFontScale)
            }
            ConnectionStatus.OfflineCached -> {
                StatusIcon(icon = Icons.Filled.WifiOff, tint = colors.error)
                Spacer(Modifier.width(5.dp))
                StatusText("Sin conexión; mostrando caché", color = colors.error, maxFontScale = maxFontScale)
            }
            ConnectionStatus.Offline -> {
                StatusIcon(icon = Icons.Filled.WifiOff, tint = colors.error)
                Spacer(Modifier.width(5.dp))
                StatusText("Sin conexión; sin caché", color = colors.error, maxFontScale = maxFontScale)
            }
        }
    }
}

@Composable
private fun StatusText(text: String, color: Color, maxFontScale: Float) {
    Text(
        text = text,
        fontSize = 12f.boundedSp(maxFontScale),
        fontWeight = FontWeight.SemiBold,
        color = color,
        maxLines = 1
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
 * Slot del delta con altura adaptada SIEMPRE ocupado: chip de flecha de tendencia
 * (subida/bajada) con la diferencia cuando hay valor previo distinto, o ficha fantasma ("—") neutra con las
 * MISMAS medidas cuando no hay dato con qué comparar. Nunca queda un hueco.
 * La animación solo corre durante la transición: en reposo el chip es
 * estático (cero costo continuo).
 */
@Composable
private fun DeltaSlot(
    current: Double,
    previous: Double?,
    slotHeight: Dp,
    fontSize: Float
) {
    Box(
        modifier = Modifier.height(slotHeight),
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
                DeltaChip(
                    current = current,
                    previous = previous ?: current,
                    fontSize = fontSize
                )
            } else {
                GhostDeltaChip(fontSize = fontSize)
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
private fun DeltaChip(current: Double, previous: Double, fontSize: Float) {
    val colors = appColors
    val delta = current - previous
    val isPositive = delta > 0
    val color = if (isPositive) colors.deltaUp else colors.deltaDown
    val trendIcon = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown
    val sign = if (isPositive) "+" else "-"
    val padV = if (fontSize < 16f) 3.dp else 5.dp
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = padV)
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
                fontSize = fontSize.boundedSp(1f),
                fontWeight = FontWeight.Bold,
                maxLines = 1
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
private fun GhostDeltaChip(fontSize: Float) {
    val colors = appColors
    val padV = if (fontSize < 16f) 3.dp else 5.dp
    Box(
        modifier = Modifier
            .background(colors.muted.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .border(1.dp, colors.muted.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
    ) {
        Text(
            text = "—",
            color = colors.muted,
            fontSize = fontSize.boundedSp(1f),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = padV)
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
private fun RollingNumber(
    value: Double,
    text: String,
    fontSize: Float,
    color: Color
) {
    val previousHolder = remember { doubleArrayOf(value) }
    val previousValue = previousHolder[0]
    val direction = when {
        value > previousValue -> 1f
        value < previousValue -> -1f
        else -> 0f
    }
    SideEffect { previousHolder[0] = value }

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
    val em = fontSize.dp
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
                fontSize = fontSize.boundedSp(1f),
                fontWeight = FontWeight.Bold,
                color = if (ch != null) color else Color.Transparent,
                maxLines = 1
            )
        }
    }
}
