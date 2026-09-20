package com.tasabcv.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb

/**
 * Widget de pantalla de inicio: lee caché (offline), toggle USD/EUR, abre la app.
 * No hace red: un valor oficial por día hábil; la app principal refresca el caché.
 * Se tiñe con la variante elegida en Ajustes (ThemeStore) según el modo del
 * sistema: cristal translúcido sobre el fondo de pantalla.
 */
class DolarWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_CURRENCY) {
            val prefs = context.getSharedPreferences(DolarRepository.PREFS_NAME, Context.MODE_PRIVATE)
            val current = prefs.getString(DolarRepository.KEY_WIDGET_CURRENCY, "USD") ?: "USD"
            val newCurr = if (current == "USD") "EUR" else "USD"
            prefs.edit().putString(DolarRepository.KEY_WIDGET_CURRENCY, newCurr).apply()
            refreshAll(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    private fun buildViews(context: Context): RemoteViews {
        val repo = DolarRepository(context)
        val prefs = context.getSharedPreferences(DolarRepository.PREFS_NAME, Context.MODE_PRIVATE)
        val currency = prefs.getString(DolarRepository.KEY_WIDGET_CURRENCY, "USD") ?: "USD"
        val rate = repo.getCachedRate(currency)

        // Tema activo: variante persistida en Ajustes + modo del sistema.
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val palette = ThemeStore.paletteFor(ThemeStore.variant, isDark)

        val views = RemoteViews(context.packageName, R.layout.widget_dolar)
        // Cristal del widget: fondo translúcido según tema + chip del modo.
        views.setInt(
            R.id.widget_root,
            "setBackgroundResource",
            widgetBackgroundRes(ThemeStore.variant, isDark)
        )
        views.setInt(
            R.id.widget_currency_chip,
            "setBackgroundResource",
            if (isDark) R.drawable.widget_currency_chip else R.drawable.widget_chip_light
        )
        // Textos: color de la paleta activa (acento / superficie / apagado).
        views.setTextColor(R.id.widget_title, palette.accent.toArgb())
        views.setTextColor(R.id.widget_value, palette.onSurface.toArgb())
        views.setTextColor(R.id.widget_date, palette.muted.toArgb())

        if (rate != null) {
            val formatted = Format.number(rate.promedio)
            // "Bs" más pequeño en el MISMO texto (RelativeSizeSpan es parcelable,
            // compatible con RemoteViews): el grupo dígitos+Bs queda centrado
            // como un solo bloque, al estilo de la app.
            val text = SpannableString("$formatted Bs")
            // El espacio entre dígitos y "Bs" queda DENTRO del span: la
            // separación se reduce a la mitad del tamaño y el "Bs" queda
            // visualmente pegado a los dígitos.
            text.setSpan(
                RelativeSizeSpan(0.5f),
                formatted.length,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            views.setTextViewText(R.id.widget_value, text)
            views.setContentDescription(R.id.widget_value, "$formatted Bs")
        } else {
            views.setTextViewText(R.id.widget_value, "—")
            views.setContentDescription(R.id.widget_value, context.getString(R.string.widget_no_data))
        }
        views.setTextViewText(
            R.id.widget_date,
            if (rate != null) "Actualizado: ${Format.fecha(rate.fechaActualizacion)}"
            else context.getString(R.string.widget_no_data)
        )
        views.setTextViewText(R.id.widget_title, currency)
        views.setContentDescription(
            R.id.widget_currency_chip,
            context.getString(R.string.widget_toggle_description)
        )
        views.setContentDescription(
            R.id.widget_title,
            context.getString(R.string.widget_toggle_description)
        )
        views.setContentDescription(R.id.widget_root, context.getString(R.string.app_name))

        val toggleIntent = Intent(context, DolarWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE_CURRENCY
        }
        val togglePi = PendingIntent.getBroadcast(
            context,
            1,
            toggleIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_currency_chip, togglePi)
        views.setOnClickPendingIntent(R.id.widget_title, togglePi)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pi = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_root, pi)
        return views
    }

    companion object {
        const val ACTION_TOGGLE_CURRENCY = "com.tasabcv.app.TOGGLE_CURRENCY"

        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, DolarWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                val intent = Intent(context, DolarWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}

/** Fondo de cristal del widget según variante de tema y modo del sistema. */
private fun widgetBackgroundRes(variant: ThemeVariant, isDark: Boolean): Int = when (variant) {
    ThemeVariant.SISTEMA ->
        if (isDark) R.drawable.widget_background else R.drawable.widget_bg_grafito_light
    ThemeVariant.AZUL ->
        if (isDark) R.drawable.widget_bg_azul_dark else R.drawable.widget_bg_azul_light
    ThemeVariant.VERDE ->
        if (isDark) R.drawable.widget_bg_verde_dark else R.drawable.widget_bg_verde_light
    ThemeVariant.ROSADO ->
        if (isDark) R.drawable.widget_bg_rosado_dark else R.drawable.widget_bg_rosado_light
}
