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
 * Medidas del tab Ajustes según el alto libre REAL de la pestaña y si se
 * muestran o no los pasos manuales del widget: tipografía, paddings y alturas
 * escalan JUNTOS para que las cuatro fichas quepan SIEMPRE en vertical,
 * proporcionales, con 3 márgenes idénticos y sin desplazamiento.
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
    val gridCellHeight: Dp,
    val cardPaddingV: Dp,
    val sectionGap: Dp,
    val infoRowV: Dp,
    val infoPaddingV: Dp,
    val maxFontScale: Float,
    val weight1: Float,
    val weight2: Float,
    val weight3: Float,
    val weight4: Float
)

private fun ajustesSizing(availableHeightDp: Int, pinSupported: Boolean): AjustesSizing {
    // Pesos de reparto del alto libre entre las 4 fichas: el espacio sobrante
    // se lo reparten las FICHAS (nunca los huecos, que quedan fijos en el
    // margen universal pegadito). Si el launcher no soporta anclar, la Ficha 1
    // crece para alojar los pasos manuales y la Ficha 2 cede ese espacio.
    fun w(base1: Float, base2: Float, base3: Float, base4: Float): FloatArray =
        if (pinSupported) {
            floatArrayOf(base1, base2, base3, base4)
        } else {
            floatArrayOf(base1 * 1.70f, base2 * 0.90f, base3, base4)
        }

    return when {
        availableHeightDp >= 680 -> w(0.86f, 3.05f, 0.30f, 1.00f).let { (a, b, c, d) ->
            AjustesSizing(
                titleFontSize = 17f,
                bodyFontSize = 12f,
                bodyLineHeight = 16f,
                smallFontSize = 11f,
                smallLineHeight = 15f,
                labelFontSize = 12f,
                bannerTitleSize = 14f,
                bannerHeight = 42.dp,
                bannerPaddingV = 12.dp,
                bannerIconBox = 34.dp,
                gridCellHeight = 92.dp,
                cardPaddingV = 12.dp,
                sectionGap = 8.dp,
                infoRowV = 3.5.dp,
                infoPaddingV = 6.dp,
                maxFontScale = ((availableHeightDp - 660f) / 600f + 1f).coerceIn(1f, 1.18f),
                weight1 = a,
                weight2 = b,
                weight3 = c,
                weight4 = d
            )
        }
        availableHeightDp >= 575 -> w(0.95f, 2.55f, 0.33f, 0.98f).let { (a, b, c, d) ->
            AjustesSizing(
                titleFontSize = 16f,
                bodyFontSize = 11f,
                bodyLineHeight = 15f,
                smallFontSize = 10f,
                smallLineHeight = 14f,
                labelFontSize = 11f,
                bannerTitleSize = 13f,
                bannerHeight = 36.dp,
                bannerPaddingV = 8.dp,
                bannerIconBox = 32.dp,
                gridCellHeight = 72.dp,
                cardPaddingV = 10.dp,
                sectionGap = 6.dp,
                infoRowV = 3.dp,
                infoPaddingV = 6.dp,
                maxFontScale = ((availableHeightDp - 560f) / 550f + 1f).coerceIn(1f, 1.12f),
                weight1 = a,
                weight2 = b,
                weight3 = c,
                weight4 = d
            )
        }
        availableHeightDp >= 470 -> w(1.05f, 1.95f, 0.40f, 1.15f).let { (a, b, c, d) ->
            AjustesSizing(
                titleFontSize = 14f,
                bodyFontSize = 10f,
                bodyLineHeight = 13f,
                smallFontSize = 9f,
                smallLineHeight = 12f,
                labelFontSize = 10f,
                bannerTitleSize = 12f,
                bannerHeight = 32.dp,
                bannerPaddingV = 6.dp,
                bannerIconBox = 28.dp,
                gridCellHeight = 56.dp,
                cardPaddingV = 8.dp,
                sectionGap = 4.dp,
                infoRowV = 2.5.dp,
                infoPaddingV = 5.dp,
                maxFontScale = 1f,
                weight1 = a,
                weight2 = b,
                weight3 = c,
                weight4 = d
            )
        }
        else -> w(1.00f, 1.70f, 0.40f, 1.30f).let { (a, b, c, d) ->
            AjustesSizing(
                titleFontSize = 13f,
                bodyFontSize = 9.5f,
                bodyLineHeight = 12f,
                smallFontSize = 8.5f,
                smallLineHeight = 11f,
                labelFontSize = 9f,
                bannerTitleSize = 11f,
                bannerHeight = 28.dp,
                bannerPaddingV = 5.dp,
                bannerIconBox = 26.dp,
                gridCellHeight = 46.dp,
                cardPaddingV = 6.dp,
                sectionGap = 3.dp,
                infoRowV = 2.dp,
                infoPaddingV = 4.dp,
                maxFontScale = 1f,
                weight1 = a,
                weight2 = b,
                weight3 = c,
                weight4 = d
            )
        }
    }
}

/**
 * Tab Ajustes (a la izquierda de Tasas), SIN título: el contenido arranca
 * directo. Cuatro fichas del mismo lenguaje visual (glassRaised):
 *   1. "Añadir widget": acción principal, arriba; ancla el widget (API 26+)
 *      o muestra los pasos manuales en launchers sin soporte.
 *   2. "Personalización": banner informativo del modo + selección de tema
 *      (Sistema/Azul/Verde/Rosado) con doble tono claro/oscuro por opción.
 *   3. "Buscar actualización": ficha-botón compacta (solo texto centrado con
 *      la misma transición del pie de Tasas) que busca, descarga e instala.
 *   4. Información fija al pie: versión, fuente, creador y lema.
 * Reparto: margen universal pegadito (8dp) en el marco exterior y en los
 * 3 huecos entre fichas; el alto sobrante se reparte ENTRE LAS FICHAS por
 * peso, nunca entre los huecos. Cero scroll.
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
            val sizing = remember(availH, pinSupported) {
                ajustesSizing(availH, pinSupported)
            }
            val cardPaddingH = if (isNarrow) 14.dp else 16.dp

            // Alto exacto que le toca a cada ficha: el sobrante del teléfono se
            // reparte entre las fichas por peso, y los 3 huecos quedan SIEMPRE
            // en el margen universal pegadito (8dp).
            val totalWeight = sizing.weight1 + sizing.weight2 + sizing.weight3 + sizing.weight4
            val freeHeightDp = (maxHeight - UniversalGap * 4).value.coerceAtLeast(0f)
            val cardH2 = (freeHeightDp * (sizing.weight2 / totalWeight)).dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = UniversalGap, end = UniversalGap, top = UniversalGap)
            ) {
                // FICHA 1 — Añadir widget: superficie SIEMPRE elevada; el toque
                // hace el pulso one-shot compartido. Nunca cambia de forma.
                Box(
                    modifier = Modifier
                        .weight(sizing.weight1)
                        .fillMaxWidth()
                        .glassRaised(colors, RoundedCornerShape(CardCornerRadius), elevation = 12.dp)
                        .pulseClick(pressedScale = 0.97f) {
                            if (pinSupported) tryPinWidget(context)
                        }
                        .padding(horizontal = cardPaddingH, vertical = sizing.cardPaddingV)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
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
                        // Justificado: más palabras por línea para que el texto
                        // quede parejo, sin huecos desproporcionados entre palabras.
                        Text(
                            text = stringResource(R.string.ajustes_widget_description),
                            fontSize = sizing.bodyFontSize.boundedSp(sizing.maxFontScale),
                            lineHeight = sizing.bodyLineHeight.boundedSp(sizing.maxFontScale),
                            color = colors.muted,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!pinSupported) {
                            // Fallback sin estado transitorio: si el launcher no
                            // soporta anclar, los pasos manuales viven aquí.
                            Column {
                                HorizontalDivider(color = colors.divider)
                                Spacer(Modifier.height(sizing.sectionGap))
                                Text(
                                    text = stringResource(R.string.ajustes_widget_manual_steps),
                                    fontSize = sizing.smallFontSize.boundedSp(sizing.maxFontScale),
                                    lineHeight = sizing.smallLineHeight.boundedSp(sizing.maxFontScale),
                                    color = colors.muted,
                                    textAlign = TextAlign.Justify,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Los 3 huecos entre las 4 fichas miden EXACTAMENTE el mismo
                // margen universal pegadito de toda la aplicación.
                Spacer(Modifier.height(UniversalGap))

                PersonalizacionCard(
                    sizing = sizing,
                    cardPaddingH = cardPaddingH,
                    cardH = cardH2,
                    modifier = Modifier
                        .weight(sizing.weight2)
                        .fillMaxWidth()
                )

                Spacer(Modifier.height(UniversalGap))

                UpdateCheckCard(
                    sizing = sizing,
                    cardPaddingH = cardPaddingH,
                    isNarrow = isNarrow,
                    modifier = Modifier
                        .weight(sizing.weight3)
                        .fillMaxWidth()
                )

                Spacer(Modifier.height(UniversalGap))

                // FICHA 4 — Información fija: no es un botón (sin pulseClick).
                // Filas etiqueta↔valor + lema centrado como cierre.
                Box(
                    modifier = Modifier
                        .weight(sizing.weight4)
                        .fillMaxWidth()
                        .glassRaised(colors, RoundedCornerShape(CardCornerRadius), elevation = 10.dp)
                        .padding(horizontal = cardPaddingH, vertical = sizing.infoPaddingV)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
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
                        }
                        // El lema cierra la ficha como texto centrado armonioso.
                        Text(
                            text = stringResource(R.string.ajustes_info_lema),
                            fontSize = sizing.smallFontSize.boundedSp(sizing.maxFontScale),
                            lineHeight = sizing.smallLineHeight.boundedSp(sizing.maxFontScale),
                            color = colors.muted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
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
private fun UpdateCheckCard(
    sizing: AjustesSizing,
    cardPaddingH: Dp,
    isNarrow: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = appColors
    val context = LocalContext.current
    val state = AppUpdater.manualState

    Box(
        modifier = modifier
            .glassRaised(colors, RoundedCornerShape(CardCornerRadius), elevation = 12.dp)
            .pulseClick(pressedScale = 0.97f) {
                AppUpdater.onManualCardClick(context)
            }
            .padding(
                horizontal = cardPaddingH,
                // Ficha-botón compacta: menos aire vertical para que el texto
                // quepa completo incluso en el reparto más ajustado.
                vertical = if (sizing.cardPaddingV >= 12.dp) 6.dp else 4.dp
            ),
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
    cardH: Dp,
    modifier: Modifier = Modifier
) {
    val colors = appColors
    val context = LocalContext.current
    val variant = ThemeStore.variant
    val isDark = colors.isDark

    // Alto de los 4 cuadritos de tema: parte del preset y se lleva el sobrante
    // real de la ficha (acotado) para que no queden huecos muertos dentro de
    // la ficha en los teléfonos altos.
    val fixedContent = sizing.cardPaddingV * 2 +
        22.dp +
        sizing.sectionGap * 3 +
        sizing.bannerHeight +
        1.5.dp +
        (sizing.bannerTitleSize * 1.45f).dp
    val availableForGrid = cardH - fixedContent
    val maxGrid = sizing.gridCellHeight * 2.10f
    // Si la ficha es baja, los cuadritos se quedan con lo que quede (nunca
    // empujan el contenido fuera de la ficha); si es alta, crecen con tope.
    val gridH = availableForGrid.coerceAtMost(maxGrid).coerceAtLeast(0.dp)

    Box(
        modifier = modifier
            .glassRaised(colors, RoundedCornerShape(CardCornerRadius), elevation = 12.dp)
            .padding(horizontal = cardPaddingH, vertical = sizing.cardPaddingV)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
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

            // Banner informativo (NO tocable, sin chevron): ícono + UNA línea
            // explicativa justificada. La burbuja usa heightIn(min): si el
            // texto ocupa 2 líneas en pantallas angostas, la burbuja crece y
            // el texto jamás se corta ni sale del bisel. El texto con
            // weight(1f) ocupa solo el ancho restante: nunca se superpone con
            // el ícono.
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
                    // Alineación natural (no justificada): en un ancho corto la
                    // justificación abría huecos enormes entre palabras.
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = colors.divider)

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

            // Cuadrícula: 4 celdas con weight(1f) => tamaño idéntico garantizado.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UniversalGap)
            ) {
                ThemeVariant.values().forEach { candidate ->
                    ThemePreviewCell(
                        variant = candidate,
                        selected = candidate == variant,
                        sizing = sizing,
                        cellHeight = gridH,
                        // weight(1f) desde el RowScope: 4 celdas idénticas.
                        modifier = Modifier.weight(1f),
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
    cellHeight: Dp,
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
                .height(cellHeight)
                .clip(shape)
                .border(
                    if (selected) 1.5.dp else 1.dp,
                    if (selected) colors.accent else colors.muted.copy(alpha = 0.25f),
                    shape
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                val dense = cellHeight < 60.dp
                PreviewBand(palette = light, dense = dense, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.muted.copy(alpha = 0.32f))
                )
                PreviewBand(palette = dark, dense = dense, modifier = Modifier.weight(1f))
            }
            if (selected) {
                // Insignia de selección sobre la COSTURA central de la celda:
                // queda en el hueco entre las dos bandas, así nunca tapa las
                // barritas del mini esqueleto en ningún tamaño de pantalla.
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .size(15.dp)
                        .background(colors.accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = colors.backgroundTop,
                        modifier = Modifier.size(10.dp)
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
    dense: Boolean,
    modifier: Modifier = Modifier
) {
    val padH = if (dense) 6.dp else 8.dp
    val padV = if (dense) 2.dp else 4.dp
    val barH = if (dense) 2.5.dp else 3.5.dp
    val barGap = if (dense) 1.5.dp else 2.5.dp
    val dotSize = if (dense) 6.dp else 8.dp

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
            .padding(horizontal = padH, vertical = padV),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .background(palette.accent, CircleShape)
        )
        Spacer(Modifier.width(5.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(barH)
                    .background(palette.muted.copy(alpha = 0.55f), RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.height(barGap))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(barH)
                    .background(palette.muted.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
            )
        }
        Spacer(Modifier.width(5.dp))
        Box(
            modifier = Modifier
                .size(
                    width = if (dense) 10.dp else 12.dp,
                    height = if (dense) 5.5.dp else 7.dp
                )
                .background(palette.accent, RoundedCornerShape(4.dp))
        )
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
