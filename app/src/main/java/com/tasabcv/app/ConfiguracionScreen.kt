package com.tasabcv.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Medidas del tab Ajustes calculadas de forma CONTINUA según el alto libre
 * real de la pestaña y si se muestran o no los pasos manuales del widget:
 * tipografía, rellenos e interlineados escalan suavemente para que las 4
 * fichas llenen la pantalla manteniendo SIEMPRE el mismo margen pegadito
 * universal entre ellas y con la barra inferior, sin desplazamiento.
 */
private data class AjustesSizing(
    val titleFontSize: Float,
    val bodyFontSize: Float,
    val bodyLineHeight: Float,
    val smallFontSize: Float,
    val smallLineHeight: Float,
    val labelFontSize: Float,
    val bannerTitleSize: Float,
    val bannerHeight: Dp,
    val bannerPaddingV: Dp,
    val bannerIconBox: Dp,
    val cardPaddingV: Dp,
    val sectionGap: Dp,
    val infoRowV: Dp,
    val infoPaddingV: Dp,
    val maxFontScale: Float
)

private fun ajustesSizing(availableHeightDp: Int): AjustesSizing {
    val t = ((availableHeightDp - 470f) / 310f).coerceIn(0f, 1.15f)
    return AjustesSizing(
        titleFontSize = (13.5f + 3.5f * t).coerceAtMost(17.5f),
        bodyFontSize = (9.5f + 2.5f * t).coerceAtMost(12.5f),
        bodyLineHeight = (12.5f + 3.8f * t).coerceAtMost(16.8f),
        smallFontSize = (8.5f + 2.5f * t).coerceAtMost(11.2f),
        smallLineHeight = (11.0f + 4.0f * t).coerceAtMost(15.5f),
        labelFontSize = (9.5f + 2.5f * t).coerceAtMost(12.2f),
        bannerTitleSize = (11.5f + 2.5f * t).coerceAtMost(14.2f),
        bannerHeight = (28f + 16f * t).dp,
        bannerPaddingV = (5f + 9f * t).dp,
        bannerIconBox = (26f + 9f * t).dp,
        cardPaddingV = (6f + 12f * t).dp,
        sectionGap = (3f + 7.5f * t).dp,
        infoRowV = (2f + 4.5f * t).dp,
        infoPaddingV = (4f + 9f * t).dp,
        maxFontScale = (1f + 0.14f * t).coerceAtMost(1.15f)
    )
}

/**
 * Tab Ajustes (a la izquierda de Tasas), SIN título: el contenido arranca
 * directo. Cuatro fichas del mismo lenguaje visual (glassRaised 32dp):
 *   1. "Añadir widget": acción principal, arriba; ancla el widget (API 26+)
 *      con texto explicativo en alineación normal.
 *   2. "Personalización": banner informativo del modo + selección de tema
 *      (Sistema/Azul/Verde/Rosado) con doble tono claro/oscuro por opción.
 *   3. "Buscar actualización": ficha-botón compacta (solo texto centrado con
 *      la misma transición del pie de Tasas) que busca, descarga e instala.
 *   4. Información fija al pie: versión, fuente, creador y lema.
 * Las 3 separaciones entre fichas y el marco exterior usan el mismo margen
 * universal de toda la app, y cero scroll.
 */
@Composable
fun ConfiguracionScreen() {
    val colors = appColors
    val context = LocalContext.current

    // ¿El launcher permite anclar widgets? (API 26+). Se consulta UNA vez:
    // cero trabajo repetido por recomposición.
    val pinSupported = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                context.getSystemService(Context.APPWIDGET_SERVICE) as AppWidgetManager
            manager.isRequestPinAppWidgetSupported
        } else {
            false
        }
    }

    val versionName = remember {
        try {
            context.packageManager
                .getPackageInfo(context.packageName, 0).versionName ?: "—"
        } catch (_: Exception) {
            "—"
        }
    }

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
            val availH = maxHeight.value.toInt()
            val isNarrow = maxWidth < 360.dp
            val sizing = remember(availH) {
                ajustesSizing(availH)
            }
            val outerH = screenOuterPaddingH(maxWidth)
            val outerV = screenOuterPaddingV(maxHeight)
            val gap = screenSectionGap(maxHeight)
            val cardPaddingH = if (isNarrow) 16.dp else 20.dp
            val infoInnerWidthDp = (maxWidth - outerH * 2 - 32.dp).value.coerceAtLeast(200f)
            val lemaFontSize = minOf(sizing.smallFontSize, (infoInnerWidthDp / 21.5f).coerceAtLeast(8.5f))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = outerH, vertical = outerV)
            ) {
                // FICHA 1 — Añadir widget: superficie SIEMPRE elevada; el toque
                // hace el pulso one-shot compartido. Nunca cambia de forma.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassRaised(colors, RoundedCornerShape(32.dp), elevation = 12.dp)
                        .pulseClick(pressedScale = 0.97f) {
                            if (pinSupported) tryPinWidget(context)
                        }
                        .padding(horizontal = cardPaddingH, vertical = sizing.cardPaddingV)
                ) {
                    Column {
                        // Ícono a la IZQUIERDA (no centrado); título "Añadir Widget"
                        // centrado con margen lateral para nunca pisar el ícono.
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                Icons.Filled.Widgets,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier
                                    .size(22.dp)
                                    .align(Alignment.CenterStart)
                            )
                            Text(
                                text = stringResource(R.string.ajustes_widget_title),
                                fontSize = sizing.titleFontSize.boundedSp(sizing.maxFontScale),
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 28.dp)
                            )
                        }
                        Spacer(Modifier.height(sizing.sectionGap))
                        // Justificado: más palabras por línea para que el texto
                        // quede parejo de borde a borde dentro de la ficha.
                        Text(
                            text = stringResource(R.string.ajustes_widget_description),
                            fontSize = sizing.bodyFontSize.boundedSp(sizing.maxFontScale),
                            lineHeight = sizing.bodyLineHeight.boundedSp(sizing.maxFontScale),
                            color = colors.muted,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Los 3 huecos entre las 4 fichas usan el MISMO margen
                // universal pegadito que separa las fichas de Tasas, Calculadora
                // y la barra inferior de pestañas.
                Spacer(Modifier.height(gap))

                PersonalizacionCard(
                    sizing = sizing,
                    cardPaddingH = cardPaddingH,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                Spacer(Modifier.height(gap))

                UpdateCheckCard(sizing, cardPaddingH, isNarrow)

                Spacer(Modifier.height(gap))

                // FICHA 4 — Información fija: no es un botón (sin pulseClick).
                // Filas etiqueta↔valor + lema centrado en 1 sola línea como cierre.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassRaised(colors, RoundedCornerShape(32.dp), elevation = 10.dp)
                        .padding(horizontal = 16.dp, vertical = sizing.infoPaddingV)
                ) {
                    Column {
                        InfoRow(
                            label = stringResource(R.string.ajustes_info_version),
                            value = versionName,
                            fontSize = sizing.bodyFontSize,
                            maxFontScale = sizing.maxFontScale,
                            rowV = sizing.infoRowV
                        )
                        HorizontalDivider(color = colors.divider)
                        InfoRow(
                            label = stringResource(R.string.ajustes_info_fuente),
                            value = DolarRepository.FUENTE,
                            fontSize = sizing.bodyFontSize,
                            maxFontScale = sizing.maxFontScale,
                            rowV = sizing.infoRowV,
                            // Fuente clicable: abre el sitio del proveedor en el
                            // navegador (sin flechita: es el único presionable y
                            // el pulso one-shot ya lo delata).
                            onClick = { openFuente(context) }
                        )
                        HorizontalDivider(color = colors.divider)
                        InfoRow(
                            label = stringResource(R.string.ajustes_info_creador),
                            value = stringResource(R.string.ajustes_info_creador_nombre),
                            fontSize = sizing.bodyFontSize,
                            maxFontScale = sizing.maxFontScale,
                            rowV = sizing.infoRowV
                        )
                        HorizontalDivider(color = colors.divider)
                        // El lema cierra la ficha siempre en 1 sola línea centrada.
                        Text(
                            text = stringResource(R.string.ajustes_info_lema),
                            fontSize = lemaFontSize.boundedSp(1f),
                            color = colors.muted,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = sizing.sectionGap)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ficha 3 — "Buscar actualización": ficha compacta que actúa únicamente como
 * botón, sin ícono y solo con el texto centrado coherente con las demás fichas.
 * La transición entre textos ("Buscar actualización" -> "Buscando…" ->
 * "Ya tienes la última versión" / "Descargando…" / "Sin conexión") usa
 * estrictamente la MISMA animación que el texto del pie de Tasas (StatusPill).
 */
@Composable
private fun UpdateCheckCard(sizing: AjustesSizing, cardPaddingH: Dp, isNarrow: Boolean) {
    val colors = appColors
    val context = LocalContext.current
    val state = AppUpdater.manualState

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassRaised(colors, RoundedCornerShape(32.dp), elevation = 12.dp)
            .pulseClick(pressedScale = 0.97f) {
                AppUpdater.onManualCardClick(context)
            }
            .padding(horizontal = cardPaddingH, vertical = sizing.cardPaddingV),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn(tween(180)) +
                    slideInVertically(MotionSpringIntOffset) { it / 3 } +
                    scaleIn(PopSpring, initialScale = 0.85f)) togetherWith
                    (fadeOut(tween(140)) + scaleOut(targetScale = 0.92f, animationSpec = tween(140)))
            },
            contentKey = { it.animationKey },
            label = "UpdateCheckCardText"
        ) { current ->
            val (text, textColor) = when (current) {
                ManualUpdateState.Idle ->
                    stringResource(R.string.update_card_idle) to colors.onSurface
                ManualUpdateState.Checking ->
                    stringResource(R.string.update_card_checking) to colors.muted
                ManualUpdateState.UpToDate ->
                    stringResource(R.string.update_card_up_to_date) to colors.deltaDown
                ManualUpdateState.Offline ->
                    stringResource(R.string.update_card_offline) to colors.error
                is ManualUpdateState.Downloading ->
                    stringResource(R.string.update_card_downloading, current.progress) to colors.accent
            }
            val effectiveFontSize = if (current is ManualUpdateState.Downloading && isNarrow) {
                (sizing.titleFontSize - 2f).coerceAtLeast(12f)
            } else {
                sizing.titleFontSize
            }
            Text(
                text = text,
                fontSize = effectiveFontSize.boundedSp(sizing.maxFontScale),
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Ficha "Personalización": encabezado con pincel, banner informativo del modo
 * (estático, sin flecha: solo comunica) y cuadrícula de 4 temas. La selección
 * persiste y repinta el widget al momento.
 */
@Composable
private fun PersonalizacionCard(
    sizing: AjustesSizing,
    cardPaddingH: Dp,
    modifier: Modifier = Modifier
) {
    val colors = appColors
    val context = LocalContext.current
    val variant = ThemeStore.variant
    val isDark = colors.isDark

    Box(
        modifier = modifier
            .glassRaised(colors, RoundedCornerShape(32.dp), elevation = 12.dp)
            .padding(horizontal = cardPaddingH, vertical = sizing.cardPaddingV)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Encabezado: pincel a la izquierda + título centrado con margen.
            Box(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Filled.Brush,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.CenterStart)
                )
                Text(
                    text = stringResource(R.string.ajustes_personalizacion_title),
                    fontSize = sizing.titleFontSize.boundedSp(sizing.maxFontScale),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 28.dp)
                )
            }

            Spacer(Modifier.height(sizing.sectionGap))

            // Banner informativo (NO tocable, sin chevron): ícono + explicación
            // con alineación natural al inicio para nunca dejar huecos grandes
            // entre palabras.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = sizing.bannerHeight)
                    .glassInset(colors, RoundedCornerShape(18.dp))
                    .padding(horizontal = 12.dp, vertical = sizing.bannerPaddingV),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(sizing.bannerIconBox)
                        .glassInset(colors, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Brightness6,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.ajustes_modo_subtitulo),
                    fontSize = sizing.bodyFontSize.boundedSp(sizing.maxFontScale),
                    lineHeight = sizing.bodyLineHeight.boundedSp(sizing.maxFontScale),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.muted,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(sizing.sectionGap))
            HorizontalDivider(color = colors.divider)
            Spacer(Modifier.height(sizing.sectionGap))

            // Fila "Temas" + indicador del modo del sistema (informativo):
            // el modo activo se resalta; el otro queda apagado.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ajustes_temas_titulo),
                    fontSize = sizing.bannerTitleSize.boundedSp(sizing.maxFontScale),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ModeIndicator(
                        icon = Icons.Filled.LightMode,
                        label = stringResource(R.string.modo_claro),
                        active = !isDark,
                        fontSize = sizing.bodyFontSize,
                        maxFontScale = sizing.maxFontScale
                    )
                    Text(
                        text = "/",
                        fontSize = sizing.bodyFontSize.boundedSp(sizing.maxFontScale),
                        color = colors.muted,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    ModeIndicator(
                        icon = Icons.Filled.DarkMode,
                        label = stringResource(R.string.modo_oscuro),
                        active = isDark,
                        fontSize = sizing.bodyFontSize,
                        maxFontScale = sizing.maxFontScale
                    )
                }
            }

            Spacer(Modifier.height(sizing.sectionGap))

            // Cuadrícula: 4 celdas con weight(1f) que toman el alto libre de la
            // ficha para que las 4 fichas de Ajustes llenen la pantalla sin
            // estirar los huecos entre fichas.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeVariant.values().forEach { candidate ->
                    ThemePreviewCell(
                        variant = candidate,
                        selected = candidate == variant,
                        sizing = sizing,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onSelect = {
                            if (ThemeStore.variant != candidate) {
                                ThemeStore.select(candidate)
                                DolarWidgetProvider.refreshAll(context)
                            }
                        }
                    )
                }
            }
        }
    }
}

/** Indicador ☀/☾ del modo del sistema: activo resaltado, inactivo apagado. */
@Composable
private fun ModeIndicator(
    icon: ImageVector,
    label: String,
    active: Boolean,
    fontSize: Float,
    maxFontScale: Float
) {
    val colors = appColors
    val color = if (active) colors.onSurface else colors.muted
    Icon(
        icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(13.dp)
    )
    Spacer(Modifier.width(4.dp))
    Text(
        text = label,
        fontSize = fontSize.boundedSp(maxFontScale),
        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
        color = color,
        maxLines = 1
    )
}

/**
 * Celda de tema: previsualización de DOBLE tono — banda clara arriba, banda
 * oscura abajo, hairline divisor — con la paleta REAL de esa variante; bajo
 * las bandas, la etiqueta. Seleccionada = borde de acento + insignia de check.
 * Todo el dibujo es estático (una sola pasada, cero animación continua).
 */
@Composable
private fun ThemePreviewCell(
    variant: ThemeVariant,
    selected: Boolean,
    sizing: AjustesSizing,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    val colors = appColors
    val light = ThemeStore.paletteFor(variant, isDark = false)
    val dark = ThemeStore.paletteFor(variant, isDark = true)
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .pulseClick(pressedScale = 0.97f) { onSelect() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(shape)
                .border(
                    if (selected) 1.5.dp else 1.dp,
                    if (selected) colors.accent else colors.muted.copy(alpha = 0.25f),
                    shape
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                PreviewBand(
                    palette = light,
                    showEndPill = false,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.muted.copy(alpha = 0.32f))
                )
                PreviewBand(
                    palette = dark,
                    showEndPill = true,
                    modifier = Modifier.weight(1f)
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(16.dp)
                        .background(colors.accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = colors.backgroundTop,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(themeLabelRes(variant)),
            fontSize = sizing.labelFontSize.boundedSp(sizing.maxFontScale),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) colors.onSurface else colors.muted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Banda de la previsualización: cristal de esa paleta + mini esqueleto. */
@Composable
private fun PreviewBand(
    palette: AppColors,
    showEndPill: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.surfaceTop.copy(alpha = 1f),
                        palette.surfaceBottom.copy(alpha = 1f)
                    )
                )
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(palette.accent, CircleShape)
        )
        Spacer(Modifier.width(5.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (showEndPill) 0.8f else 0.62f)
                    .height(3.5.dp)
                    .background(palette.muted.copy(alpha = 0.55f), RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.height(2.5.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (showEndPill) 0.55f else 0.42f)
                    .height(3.5.dp)
                    .background(palette.muted.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
            )
        }
        if (showEndPill) {
            Spacer(Modifier.width(5.dp))
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 7.dp)
                    .background(palette.accent, RoundedCornerShape(4.dp))
            )
        }
    }
}

private fun themeLabelRes(variant: ThemeVariant): Int = when (variant) {
    ThemeVariant.SISTEMA -> R.string.tema_sistema
    ThemeVariant.AZUL -> R.string.tema_azul
    ThemeVariant.VERDE -> R.string.tema_verde
    ThemeVariant.ROSADO -> R.string.tema_rosado
}

/** Fila de la ficha de información: etiqueta a la izquierda, valor a la derecha.
 *  Con [onClick] la fila es presionable (pulso one-shot) y actúa como enlace. */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    fontSize: Float,
    maxFontScale: Float,
    rowV: Dp,
    onClick: (() -> Unit)? = null
) {
    val colors = appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.pulseClick(pressedScale = 0.97f) { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(vertical = rowV),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = fontSize.boundedSp(maxFontScale),
            fontWeight = FontWeight.Medium,
            color = colors.muted,
            maxLines = 1
        )
        Text(
            text = value,
            fontSize = fontSize.boundedSp(maxFontScale),
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface,
            maxLines = 1
        )
    }
}

/** Abre el sitio de la fuente de datos en el navegador (silencioso si no hay). */
private fun openFuente(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://dolarapi.com")))
    } catch (_: Exception) {
    }
}

/**
 * Ancla el widget al escritorio (sistema de Android 8+ muestra el diálogo de
 * confirmación). Devuelve false si el launcher no soporta el anclado: la
 * ficha ya muestra en ese caso los pasos manuales, así que no hay más UI.
 */
private fun tryPinWidget(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    return try {
        val manager = context.getSystemService(Context.APPWIDGET_SERVICE) as AppWidgetManager
        if (!manager.isRequestPinAppWidgetSupported) return false
        val provider = ComponentName(context, DolarWidgetProvider::class.java)
        manager.requestPinAppWidget(provider, null, null)
    } catch (_: Exception) {
        false
    }
}
