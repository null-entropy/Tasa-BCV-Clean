package com.tasabcv.app

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
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
 * Repositorio: cadena de 3 proveedores con fallback y caché por moneda.
 */
class DolarRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun fetchRate(currency: String): DolarResult {
        for (provider in PROVIDERS) {
            try {
                val rate = provider.fetch(currency) ?: continue
                if (isSane(rate.promedio)) {
                    saveCache(currency, rate)
                    val fullRate = getCachedRate(currency) ?: rate
                    return DolarResult.Success(fullRate)
                }
            } catch (_: Exception) {
                // Proveedor caído/tarde: siguiente de forma transparente.
            }
        }
        val cached = getCachedRate(currency)
        return DolarResult.Error("No se pudo conectar con ninguna fuente de tasa.", cached)
    }

    fun getCachedRate(currency: String): DolarRate? {
        val promedio = readDouble(keyPromedio(currency)) ?: return null
        if (!isSane(promedio)) return null
        val fecha = prefs.getString(keyFecha(currency), "") ?: ""
        val fuente = prefs.getString(keyFuente(currency), "caché") ?: "caché"
        val previousPromedio = readDouble(keyPrevious(currency))
        return DolarRate(promedio, fecha, fuente, previousPromedio)
    }

    /** Marca temporal (ms) del último fetch exitoso de una moneda; 0 si nunca. */
    fun getCacheTimeMillis(currency: String): Long =
        prefs.getLong(keyCacheTime(currency), 0L)

    /** El mayor `cache_time` entre USD y EUR; 0 si no hay caché. */
    fun getLastSuccessfulFetchMillis(): Long =
        maxOf(getCacheTimeMillis("USD"), getCacheTimeMillis("EUR"))

    private interface Provider {
        val name: String
        fun fetch(currency: String): DolarRate?
    }

    private val PROVIDERS: List<Provider> = listOf(
        object : Provider {
            override val name = "dolarapi.com"
            override fun fetch(currency: String): DolarRate? {
                val endpoint = if (currency == "EUR") ENDPOINT_DOLARAPI_EUR else ENDPOINT_DOLARAPI_USD
                val json = httpGetJson(endpoint) ?: return null
                if (!json.has("promedio")) return null
                val value = json.getDouble("promedio")
                if (!isSane(value)) return null
                return DolarRate(
                    promedio = value,
                    fechaActualizacion = json.optString("fechaActualizacion"),
                    fuente = name
                )
            }
        },
        object : Provider {
            override val name = "pydolarve.dev"
            override fun fetch(currency: String): DolarRate? {
                val currUrl = if (currency == "EUR") "euro" else "dollar"
                val json = httpGetJson("https://pydolarve.dev/api/v1/$currUrl/bcv") ?: return null
                if (!json.has("price")) return null
                val price = json.getDouble("price")
                if (!isSane(price)) return null
                val fecha = json.optString("last_update", "")
                return DolarRate(price, fecha, name)
            }
        },
        object : Provider {
            override val name = "bcv.org.ve"
            override fun fetch(currency: String): DolarRate? {
                // Un solo fetch del HTML por ciclo de refresh (memo compartido
                // entre USD y EUR): el scraping corre en paralelo y no debe
                // descargar ~200KB dos veces.
                val html = getBcvHtml() ?: return null
                val currRegex = if (currency == "EUR") "(?:euro|EUR)" else "(?:d[oó]lar|USD)"

                val regexes = listOf(
                    Regex("""(?i)$currRegex[\s\S]{0,400}?(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{2}))"""),
                    Regex("""(?i)id="[a-zA-Z]*$currRegex[a-zA-Z]*"[\s\S]{0,300}?(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{2}))"""),
                    Regex("""(?i)(?:<strong|class="[^"]*(?:price|rate|amount|valor|tasa|cambio)[^"]*")[\s\S]{0,300}?(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{2}))""")
                )
                for (regex in regexes) {
                    val match = regex.find(html) ?: continue
                    val raw = match.groupValues[1]
                    val normalized = normalizeNumber(raw) ?: continue
                    if (isSane(normalized)) {
                        return DolarRate(normalized, "", name)
                    }
                }
                return null
            }
        }
    )

    private fun httpGetJson(url: String): JSONObject? {
        val body = httpGetText(url) ?: return null
        return JSONObject(body)
    }

    /** HTML de la página del BCV memoizado (una descarga por ciclo de refresh). */
    private var bcvHtml: String? = null
    private var bcvHtmlFetchedAt: Long = 0L

    @Synchronized
    private fun getBcvHtml(): String? {
        val now = System.currentTimeMillis()
        if (bcvHtml == null || now - bcvHtmlFetchedAt > BCV_HTML_MEMO_TTL_MS) {
            bcvHtml = httpGetText(ENDPOINT_BCV)
            bcvHtmlFetchedAt = now
        }
        return bcvHtml
    }

    private fun httpGetText(url: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json, text/html, */*")
                setRequestProperty("User-Agent", USER_AGENT)
                instanceFollowRedirects = true
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun normalizeNumber(raw: String): Double? {
        return try {
            val cleaned = when {
                raw.contains('.') && raw.contains(',') -> raw.replace(".", "").replace(",", ".")
                raw.contains(',') -> raw.replace(",", ".")
                else -> raw
            }
            cleaned.toDouble()
        } catch (_: Exception) {
            null
        }
    }

    private fun saveCache(currency: String, rate: DolarRate) {
        val oldPromedio = readDouble(keyPromedio(currency))
        val editor = prefs.edit()
            .putString(keyPromedio(currency), rate.promedio.toString())
            .putString(keyFecha(currency), rate.fechaActualizacion)
            .putString(keyFuente(currency), rate.fuente)
            .putLong(keyCacheTime(currency), System.currentTimeMillis())

        // Solo cuando hay cambio real de tasa (no en cada refresh del mismo día).
        if (oldPromedio != null && oldPromedio > 0.0 && !sameRate(oldPromedio, rate.promedio)) {
            editor.putString(keyPrevious(currency), oldPromedio.toString())
        }

        editor.apply()
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
        private const val ENDPOINT_BCV = "https://www.bcv.org.ve/"
        private const val TIMEOUT_MS = 8000
        private const val USER_AGENT = "TasaBCVCleam-App/4.5.0 (Android)"

        /** TTL del memo del HTML del BCV: cubre un ciclo de refresh completo (USD+EUR). */
        private const val BCV_HTML_MEMO_TTL_MS = 10_000L

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
