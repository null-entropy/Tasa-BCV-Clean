package com.tasabcv.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.widget.RemoteViews

/**
 * Widget de pantalla de inicio: lee caché (offline), toggle USD/EUR, abre la app.
 * No hace red: un valor oficial por día hábil; la app principal refresca el caché.
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

        val views = RemoteViews(context.packageName, R.layout.widget_dolar)
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
