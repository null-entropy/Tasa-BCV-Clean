package com.tasabcv.app

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class DolarRate(
    val promedio: Double,
    val fechaActualizacion: String,
    val fuente: String,
    val previousPromedio: Double? = null
)

sealed class DolarResult {
    data class Success(val rate: DolarRate) : DolarResult()
    data class Error(val errorMessage: String, val cachedRate: DolarRate?) : DolarResult()
}

/**
 * Repositorio: proveedor único dolarapi.com + caché por moneda.
 * Sin cadena de fallback: una sola fuente, respuesta directa y rápida.
 */
class DolarRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun fetchRate(currency: String): DolarResult {
        val endpoint = if (currency == "EUR") ENDPOINT_DOLARAPI_EUR else ENDPOINT_DOLARAPI_USD
        val rate = fetchDolarApi(endpoint)
        if (rate != null) {
            // saveCache devuelve la tasa RESUELTA (con el valor previo calculado
            // en memoria): cero relecturas de caché tras un fetch exitoso.
            return DolarResult.Success(saveCache(currency, rate))
        }
        return DolarResult.Error(ERROR_MESSAGE, getCachedRate(currency))
    }

    fun getCachedRate(currency: String): DolarRate? {
        val promedio = readDouble(keyPromedio(currency)) ?: return null
        if (!isSane(promedio)) return null
        val fecha = prefs.getString(keyFecha(currency), "") ?: ""
        val fuente = prefs.getString(keyFuente(currency), FUENTE) ?: FUENTE
        val previousPromedio = readDouble(keyPrevious(currency))
        return DolarRate(promedio, fecha, fuente, previousPromedio)
    }

    /** Marca temporal (ms) del último fetch exitoso de una moneda; 0 si nunca. */
    fun getCacheTimeMillis(currency: String): Long =
        prefs.getLong(keyCacheTime(currency), 0L)

    /** El mayor `cache_time` entre USD y EUR; 0 si no hay caché. */
    fun getLastSuccessfulFetchMillis(): Long =
        maxOf(getCacheTimeMillis("USD"), getCacheTimeMillis("EUR"))

    /**
     * Descarga y parsea la respuesta de dolarapi.com: body mínimo (~200 bytes)
     * leído directamente y parseado con [JSONObject]. Un solo GET por moneda;
     * sanity check incluido.
     */
    private fun fetchDolarApi(endpoint: String): DolarRate? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                instanceFollowRedirects = true
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            // Body mínimo (~200 bytes); lectura directa y parseo simple.
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(body)
            if (!json.has("promedio")) return null
            val value = json.getDouble("promedio")
            if (!isSane(value)) return null
            DolarRate(
                promedio = value,
                fechaActualizacion = json.optString("fechaActualizacion"),
                fuente = FUENTE
            )
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Persiste la tasa y devuelve la versión RESUELTA (con [DolarRate.previousPromedio]
     * calculado en memoria): el previo solo se relee cuando NO hubo cambio.
     * `readDouble` ya filtra valores ≤ 0, así que basta con comparar.
     */
    private fun saveCache(currency: String, rate: DolarRate): DolarRate {
        val oldPromedio = readDouble(keyPromedio(currency))
        val changed = oldPromedio != null && !sameRate(oldPromedio, rate.promedio)
        val editor = prefs.edit()
            .putString(keyPromedio(currency), rate.promedio.toString())
            .putString(keyFecha(currency), rate.fechaActualizacion)
            .putString(keyFuente(currency), rate.fuente)
            .putLong(keyCacheTime(currency), System.currentTimeMillis())

        // Solo cuando hay cambio real de tasa (no en cada refresh del mismo día).
        if (oldPromedio != null && changed) {
            editor.putString(keyPrevious(currency), oldPromedio.toString())
        }

        editor.apply()
        val previous = if (changed) oldPromedio else readDouble(keyPrevious(currency))
        return rate.copy(previousPromedio = previous)
    }

    /** Lee Double guardado como String; fallback a Float legacy (evita ClassCastException). */
    private fun readDouble(key: String): Double? {
        if (!prefs.contains(key)) return null
        try {
            prefs.getString(key, null)?.toDoubleOrNull()?.let { value ->
                if (value > 0.0) return value
            }
        } catch (_: ClassCastException) {
            // Valor legacy guardado como Float.
        }
        return try {
            val legacy = prefs.getFloat(key, -1f)
            if (legacy > 0f) legacy.toDouble() else null
        } catch (_: ClassCastException) {
            null
        }
    }

    companion object {
        const val PREFS_NAME = "dolar_bcv_prefs"
        const val KEY_WIDGET_CURRENCY = "widget_currency"

        private const val ENDPOINT_DOLARAPI_USD = "https://ve.dolarapi.com/v1/dolares/oficial"
        private const val ENDPOINT_DOLARAPI_EUR = "https://ve.dolarapi.com/v1/euros/oficial"
        private const val FUENTE = "dolarapi.com"
        private const val TIMEOUT_MS = 6000
        private const val USER_AGENT = "TasaBCVCleam-App/4.6.3 (Android)"
        private const val ERROR_MESSAGE = "No se pudo conectar con la fuente de tasas."

        private const val KEY_PROMEDIO = "promedio"
        private const val KEY_FECHA = "fecha"
        private const val KEY_FUENTE = "fuente"
        private const val KEY_CACHE_TIME = "cache_time"
        private const val KEY_PREVIOUS = "previous"

        fun keyPromedio(currency: String) = "${KEY_PROMEDIO}_$currency"
        fun keyFecha(currency: String) = "${KEY_FECHA}_$currency"
        fun keyFuente(currency: String) = "${KEY_FUENTE}_$currency"
        fun keyCacheTime(currency: String) = "${KEY_CACHE_TIME}_$currency"
        fun keyPrevious(currency: String) = "${KEY_PREVIOUS}_$currency"

        fun isSane(value: Double): Boolean = value > 0.0 && value <= 1_000_000.0

        /** Comparación robusta ante ruido de punto flotante / serialización. */
        fun sameRate(a: Double, b: Double): Boolean = kotlin.math.abs(a - b) < 0.00005
    }
}