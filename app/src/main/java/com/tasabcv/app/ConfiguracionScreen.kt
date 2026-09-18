package com.tasabcv.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tab Ajustes (a la izquierda de Tasas), SIN título: el contenido arranca
 * directo para aprovechar el espacio. Dos fichas del mismo lenguaje visual
 * (neuRaised 32dp + pulseClick):
 *   1. "Añadir widget": acción principal, arriba; ancla el widget al
 *      escritorio (API 26+). En Android 7 o launchers sin soporte, la misma
 *      ficha muestra los pasos manuales — sin ventanas emergentes.
 *   2. Información fija al pie: versión de la app, fuente de datos y lema.
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
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // FICHA 1 — Añadir widget: superficie SIEMPRE elevada; el toque
            // hace el pulso one-shot compartido (idéntico a las fichas de
            // tasas). Nunca cambia de forma.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neuRaised(colors, RoundedCornerShape(32.dp), elevation = 12.dp)
                    .pulseClick(pressedScale = 0.97f) {
                        if (pinSupported) tryPinWidget(context)
                    }
                    .padding(horizontal = 20.dp, vertical = 20.dp)
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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    // 12sp: con más palabras por línea el justificado queda
                    // parejo, sin huecos desproporcionados entre palabras.
                    Text(
                        text = stringResource(R.string.ajustes_widget_description),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = colors.muted,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!pinSupported) {
                        // Fallback sin estado transitorio: si el launcher no
                        // soporta anclar (Android 7 o launchers limitados),
                        // los pasos manuales viven aquí desde el arranque.
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = colors.divider)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.ajustes_widget_manual_steps),
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = colors.muted,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // El pie informativo queda SIEMPRE abajo, en pantallas de
            // cualquier tamaño: el hueco intermedio absorbe el espacio.
            Spacer(Modifier.weight(1f))

            // FICHA 2 — Información fija: no es un botón (sin pulseClick).
            // Filas de lista: etiqueta a la izquierda, valor a la derecha,
            // separadores sutiles. El lema cierra como línea completa.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neuRaised(colors, RoundedCornerShape(32.dp), elevation = 10.dp)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Column {
                    InfoRow(
                        label = stringResource(R.string.ajustes_info_version),
                        value = versionName
                    )
                    HorizontalDivider(color = colors.divider)
                    InfoRow(
                        label = stringResource(R.string.ajustes_info_fuente),
                        value = DolarRepository.FUENTE
                    )
                    HorizontalDivider(color = colors.divider)
                    Text(
                        text = stringResource(R.string.ajustes_info_lema),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = colors.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }
            }
        }
    }
}

/** Fila de la ficha de información: etiqueta a la izquierda, valor a la derecha. */
@Composable
private fun InfoRow(label: String, value: String) {
    val colors = appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.muted
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
        )
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
