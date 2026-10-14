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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Medidas del tab Ajustes según la altura de pantalla: tipografía, paddings y
 * alturas escalan JUNTOS para que las cuatro fichas quepan SIEMPRE, proporciona-
 * les y sin scroll. Un preset por franja de altura (calculado 1 vez, costo 0).
 */
private data class AjustesSizing(
    val titleFontSize: Int,
    val bodyFontSize: Int,
    val bodyLineHeight: Int,
    val smallFontSize: Int,
    val smallLineHeight: Int,
    val labelFontSize: Int,
    val bannerTitleSize: Int,
    val bannerHeight: Dp,
    val gridCellHeight: Dp,
    val cardPaddingV: Dp,
    val sectionGap: Dp,
    val infoRowV: Dp,
    val infoPaddingV: Dp,
    val fichaGap: Dp
)

private fun ajustesSizing(screenHeightDp: Int): AjustesSizing = when {
    screenHeightDp >= 800 -> AjustesSizing(
        17, 12, 16, 11, 15, 12, 14, 42.dp, 96.dp, 14.dp, 8.dp, 4.dp, 8.dp, 16.dp
    )
    screenHeightDp >= 700 -> AjustesSizing(
        16, 11, 15, 10, 14, 11, 13, 38.dp, 76.dp, 12.dp, 6.dp, 4.dp, 6.dp, 12.dp
    )
    else -> AjustesSizing(
        14, 10, 13, 9, 12, 9, 12, 34.dp, 64.dp, 9.dp, 4.dp, 3.dp, 5.dp, 8.dp
    )
}

/**
 * Tab Ajustes (a la izquierda de Tasas), SIN título: el contenido arranca
 * directo. Cuatro fichas del mismo lenguaje visual (glassRaised 32dp):
 *   1. "Añadir widget": acción principal, arriba; ancla el widget (API 26+)
 *      o muestra los pasos manuales en launchers sin soporte.
 *   2. "Personalización": banner informativo del modo + selección de tema
 *      (Sistema/Azul/Verde/Rosado) con doble tono claro/oscuro por opción.
 *   3. "Buscar actualización": ficha-botón compacta (solo texto centrado con
 *      la misma transición del pie de Tasas) que busca, descarga e instala.
 *   4. Información fija al pie: versión, fuente, creador y lema.
 * Los 3 huecos entre las 4 fichas usan exactamente la misma pareja
 * (Spacer weight(1f) + Spacer fichaGap): márgenes idénticos y cero scroll.
 */
@Composable
fun ConfiguracionScreen() {
    val colors = appColors
    val context = LocalContext.current
    // La configuración se lee FUERA de remember (lectura componible); el
    // preset de medidas se calcula una vez por altura de pantalla.
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val sizing = remember(screenHeightDp) { ajustesSizing(screenHeightDp) }

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
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
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
                    .padding(horizontal = 20.dp, vertical = sizing.cardPaddingV)
            ) {
                Column {
                    // Ícono a la IZQUIERDA (no centrado); título "Añadir Widget"
                    // centrado como encabezado de la ficha.
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
                            fontSize = sizing.titleFontSize.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Spacer(Modifier.height(sizing.sectionGap))
                    // Justificado: más palabras por línea para que el texto
                    // quede parejo, sin huecos desproporcionados entre palabras.
                    Text(
                        text = stringResource(R.string.ajustes_widget_description),
                        fontSize = sizing.bodyFontSize.sp,
                        lineHeight = sizing.bodyLineHeight.sp,
                        color = colors.muted,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!pinSupported) {
                        // Fallback sin estado transitorio: si el launcher no
                        // soporta anclar, los pasos manuales viven aquí.
                        Spacer(Modifier.height(sizing.sectionGap))
                        HorizontalDivider(color = colors.divider)
                        Spacer(Modifier.height(sizing.sectionGap))
                        Text(
                            text = stringResource(R.string.ajustes_widget_manual_steps),
                            fontSize = sizing.smallFontSize.sp,
                            lineHeight = sizing.smallLineHeight.sp,
                            color = colors.muted,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Los 3 huecos entre las 4 fichas llevan EXACTAMENTE la misma
            // pareja (Spacer weight(1f) + Spacer fichaGap): el margen entre
            // todas las fichas es estrictamente idéntico en cualquier pantalla.
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(sizing.fichaGap))

            PersonalizacionCard(sizing)

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(sizing.fichaGap))

            UpdateCheckCard(sizing)

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(sizing.fichaGap))

            // FICHA 4 — Información fija: no es un botón (sin pulseClick).
            // Filas etiqueta↔valor + lema centrado como cierre.
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
                        rowV = sizing.infoRowV
                    )
                    HorizontalDivider(color = colors.divider)
                    InfoRow(
                        label = stringResource(R.string.ajustes_info_fuente),
                        value = DolarRepository.FUENTE,
                        fontSize = sizing.bodyFontSize,
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
                        rowV = sizing.infoRowV
                    )
                    HorizontalDivider(color = colors.divider)
                    // El lema cierra la ficha como texto centrado armonioso.
                    Text(
                        text = stringResource(R.string.ajustes_info_lema),
                        fontSize = sizing.smallFontSize.sp,
                        lineHeight = sizing.smallLineHeight.sp,
                        color = colors.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = sizing.sectionGap)
                    )
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
private fun UpdateCheckCard(sizing: AjustesSizing) {
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
            .padding(horizontal = 20.dp, vertical = sizing.cardPaddingV),
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
            Text(
                text = text,
                fontSize = sizing.titleFontSize.sp,
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
private fun PersonalizacionCard(sizing: AjustesSizing) {
    val colors = appColors
    val context = LocalContext.current
    val variant = ThemeStore.variant
    val isDark = colors.isDark

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassRaised(colors, RoundedCornerShape(32.dp), elevation = 12.dp)
            .padding(horizontal = 20.dp, vertical = sizing.cardPaddingV)
    ) {
        Column {
            // Encabezado: pincel a la izquierda + título centrado.
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
                    fontSize = sizing.titleFontSize.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(Modifier.height(sizing.sectionGap))

            // Banner informativo (NO tocable, sin chevron): ícono + UNA línea
            // explicativa justificada. La burbuja usa heightIn(min): si el
            // texto ocupa 2 líneas en pantallas angostas, la burbuja crece y
            // el texto jamás se corta ni sale del bisel. El texto con
            // weight(1f) ocupa solo el ancho restante: nunca se superpone con
            // el ícono (34dp reservados + separador).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = sizing.bannerHeight)
                    .glassInset(colors, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(34.dp).glassInset(colors, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Brightness6,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.ajustes_modo_subtitulo),
                    // Mismo tamaño del texto explicativo de la ficha widget:
                    // discreto, gris apagado y justificado.
                    fontSize = sizing.bodyFontSize.sp,
                    lineHeight = sizing.bodyLineHeight.sp,
                    fontWeight = FontWeight.SemiBold,
                    // Mismo gris apagado que el texto explicativo de la ficha widget.
                    color = colors.muted,
                    textAlign = TextAlign.Justify,
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
                    fontSize = sizing.bannerTitleSize.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ModeIndicator(
                        icon = Icons.Filled.LightMode,
                        label = stringResource(R.string.modo_claro),
                        active = !isDark,
                        fontSize = sizing.bodyFontSize
                    )
                    Text(
                        text = "/",
                        fontSize = sizing.bodyFontSize.sp,
                        color = colors.muted,
                        modifier = Modifier.padding(horizontal = 7.dp)
                    )
                    ModeIndicator(
                        icon = Icons.Filled.DarkMode,
                        label = stringResource(R.string.modo_oscuro),
                        active = isDark,
                        fontSize = sizing.bodyFontSize
                    )
                }
            }

            Spacer(Modifier.height(sizing.sectionGap))

            // Cuadrícula: 4 celdas con weight(1f) => tamaño idéntico garantizado.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeVariant.values().forEach { candidate ->
                    ThemePreviewCell(
                        variant = candidate,
                        selected = candidate == variant,
                        sizing = sizing,
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
private fun ModeIndicator(icon: ImageVector, label: String, active: Boolean, fontSize: Int) {
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
        fontSize = fontSize.sp,
        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
        color = color
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
                .height(sizing.gridCellHeight)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            light.surfaceTop.copy(alpha = 1f),
                            light.surfaceBottom.copy(alpha = 1f)
                        )
                    )
                )
                .border(
                    if (selected) 1.5.dp else 1.dp,
                    if (selected) colors.accent else colors.muted.copy(alpha = 0.25f),
                    shape
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                PreviewBand(palette = light, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.muted.copy(alpha = 0.32f))
                )
                PreviewBand(palette = dark, modifier = Modifier.weight(1f))
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                        .background(colors.accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = colors.backgroundTop,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = stringResource(themeLabelRes(variant)),
            fontSize = sizing.labelFontSize.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) colors.onSurface else colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Banda de la previsualización: cristal de esa paleta + mini esqueleto. */
@Composable
private fun PreviewBand(palette: AppColors, modifier: Modifier = Modifier) {
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
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(palette.accent, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(4.dp)
                    .background(palette.muted.copy(alpha = 0.55f), RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(4.dp)
                    .background(palette.muted.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
            )
        }
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 8.dp)
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
    fontSize: Int,
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
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Medium,
            color = colors.muted
        )
        Text(
            text = value,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
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
