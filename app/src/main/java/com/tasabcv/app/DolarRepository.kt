package com.tasabcv.app

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale

/** Resultado mínimo de un GET: el JSON del cuerpo y el código de respuesta (0 = error de conexión). */
private class RespuestaHttp(val json: JSONObject?, val codigo: Int)

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
 * Cada actualización pide la tasa VIGENTE y la ANTERIOR (historial diario del
 * proveedor): el delta funciona desde la instalación nueva, sin depender de
 * tener caché del día anterior. Sin cadena de proveedores: una sola fuente.
 */
class DolarRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * "Saludo" de la app ante el proveedor. El número de versión se toma SIEMPRE
     * del instalable (PackageManager): una sola fuente de verdad, imposible que
     * se desfase de `versionName` al publicar una versión nueva.
     */
    private val userAgent: String = buildUserAgent(context)

    fun fetchRate(currency: String): DolarResult {
        val endpoint = if (currency == "EUR") ENDPOINT_DOLARAPI_EUR else ENDPOINT_DOLARAPI_USD
        val rate = fetchDolarApi(endpoint)
        if (rate != null) {
            // Tasa anterior del PROVEEDOR (día publicado inmediatamente antes).
            // Si la fuente no la aporta, vale la última comparación guardada:
            // el delta nunca depende de la caché del día anterior.
            val previous = fetchPrevious(currency, rate.fechaActualizacion)
                ?: readDouble(keyPrevious(currency))
            // saveCache devuelve la tasa RESUELTA: cero relecturas de caché tras
            // un fetch exitoso.
            return DolarResult.Success(saveCache(currency, rate.copy(previousPromedio = previous)))
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
     * Tasa vigente: body mínimo (~200 bytes) leído directo y parseado con
     * [JSONObject], con sanity check incluido.
     */
    private fun fetchDolarApi(endpoint: String): DolarRate? {
        val json = descargarJson(endpoint, TIMEOUT_MS).json ?: return null
        return try {
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
        }
    }

    /**
     * Tasa del día publicado inmediatamente ANTES del vigente. El BCV no
     * publica fines de semana ni feriados (esos días la fuente responde 404):
     * se retrocede día a día, saltando sábados y domingos, hasta encontrar el
     * valor. Máximo [MAX_PREV_LOOKBACK] días HÁBILES consultados y espera corta
     * ([PREV_TIMEOUT_MS]) por intento. Cualquier problema distinto de "ese día
     * no hay publicación" corta el retroceso al instante: el llamador usa la
     * última comparación guardada. Respuestas diminutas (~100 bytes).
     */
    private fun fetchPrevious(currency: String, fechaActualizacion: String): Double? {
        val (anio, mes, dia) = parseFechaDia(fechaActualizacion) ?: return null
        val calendar = Calendar.getInstance().apply {
            clear()
            set(anio, mes - 1, dia)
        }
        val base = if (currency == "EUR") ENDPOINT_HISTORICO_EUR else ENDPOINT_HISTORICO_USD
        var consultas = 0
        while (consultas < MAX_PREV_LOOKBACK) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SATURDAY, Calendar.SUNDAY -> continue // el BCV no publica
            }
            consultas++
            val fecha = String.format(
                Locale.US, "%04d/%02d/%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            val respuesta = descargarJson("$base/$fecha", PREV_TIMEOUT_MS)
            when {
                // Día sin publicación (fin de semana largo / feriado): seguir retrocediendo.
                respuesta.codigo == HttpURLConnection.HTTP_NOT_FOUND -> continue
                // Problema real de la fuente: parar ya y usar lo guardado.
                respuesta.codigo != HttpURLConnection.HTTP_OK -> return null
                else -> {
                    val value = try {
                        respuesta.json?.let { if (it.has("promedio")) it.getDouble("promedio") else null }
                    } catch (_: Exception) {
                        null
                    }
                    return if (value != null && isSane(value)) value else null
                }
            }
        }
        return null
    }

    /**
     * "AAAA-MM-DD" (prefijo ISO) a sus tres partes numéricas; null si no es una
     * fecha válida. Sin `java.time` (minSdk 24 sin desugaring).
     */
    private fun parseFechaDia(iso: String): Triple<Int, Int, Int>? {
        val fecha = iso.take(10)
        if (fecha.length != 10 || fecha[4] != '-' || fecha[7] != '-') return null
        val anio = fecha.substring(0, 4).toIntOrNull() ?: return null
        val mes = fecha.substring(5, 7).toIntOrNull() ?: return null
        val dia = fecha.substring(8, 10).toIntOrNull() ?: return null
        if (anio < 2000 || mes !in 1..12 || dia !in 1..31) return null
        return Triple(anio, mes, dia)
    }

    /** GET simple a la fuente: body leído directo y parseado con [JSONObject]. */
    private fun descargarJson(url: String, timeoutMs: Int): RespuestaHttp {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", userAgent)
                instanceFollowRedirects = true
            }
            val codigo = connection.responseCode
            if (codigo != HttpURLConnection.HTTP_OK) return RespuestaHttp(null, codigo)
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            RespuestaHttp(JSONObject(body), codigo)
        } catch (_: Exception) {
            RespuestaHttp(null, 0)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Persiste la tasa vigente y la comparación del delta (valor anterior) para
     * el uso SIN internet, y devuelve la misma tasa resuelta: cero relecturas
     * de caché tras un fetch exitoso. El anterior se guarda SIEMPRE que exista
     * (traído del proveedor o heredado como respaldo): así el indicador de
     * variación sigue funcionando aunque la app se use sin conexión.
     */
    private fun saveCache(currency: String, rate: DolarRate): DolarRate {
        val editor = prefs.edit()
            .putString(keyPromedio(currency), rate.promedio.toString())
            .putString(keyFecha(currency), rate.fechaActualizacion)
            .putString(keyFuente(currency), rate.fuente)
            .putLong(keyCacheTime(currency), System.currentTimeMillis())
        rate.previousPromedio?.let { editor.putString(keyPrevious(currency), it.toString()) }
        editor.apply()
        return rate
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

        // Historial diario del proveedor (la fecha se pide con BARRAS: AAAA/MM/DD;
        // el cuerpo de la respuesta la trae con guiones: AAAA-MM-DD).
        private const val ENDPOINT_HISTORICO_USD = "https://ve.dolarapi.com/v1/historicos/dolares/oficial"
        private const val ENDPOINT_HISTORICO_EUR = "https://ve.dolarapi.com/v1/historicos/euros/oficial"

        /** Fuente de datos visible en la ficha de información de Ajustes. */
        const val FUENTE = "dolarapi.com"
        private const val TIMEOUT_MS = 6000

        /** Espera corta por consulta diaria del historial: nunca congelar la app. */
        private const val PREV_TIMEOUT_MS = 3000

        /** Máximo de días HÁBILES consultados al buscar la tasa anterior. */
        private const val MAX_PREV_LOOKBACK = 5

        private const val ERROR_MESSAGE = "No se pudo conectar con la fuente de tasas."

        /** Marca de identificación de la app ante el proveedor (con versión real). */
        private fun buildUserAgent(context: Context): String {
            val version = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (_: Exception) {
                null
            }
            return "TasaBCVCleam-App/${version ?: "desconocida"} (Android)"
        }

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