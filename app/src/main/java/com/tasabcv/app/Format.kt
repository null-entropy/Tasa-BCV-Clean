package com.tasabcv.app

import java.text.NumberFormat
import java.util.Locale

/**
 * Utilidades de formato compartidas entre la UI y el widget.
 * Sin java.time: compatible con minSdk 24 sin desugaring.
 */
object Format {

    private val veLocale = Locale("es", "VE")

    private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(veLocale).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }

    /** Solo el número, agrupado es-VE, 2 decimales (sin prefijo). */
    @Synchronized
    fun number(value: Double): String = numberFormat.format(value)

    /**
     * Entrada del usuario a Double: en Venezuela los puntos son miles y las comas decimales.
     */
    fun parseUserInput(raw: String): Double? {
        if (raw.isBlank()) return null

        val trimmed = raw.trim()
        val sanitized = if (trimmed.endsWith(',') || trimmed.endsWith('.')) {
            trimmed.dropLast(1)
        } else {
            trimmed
        }

        if (sanitized.isEmpty()) return null

        val normalized = sanitized.replace(".", "").replace(",", ".")
        return normalized.toDoubleOrNull()
    }

    /**
     * Formatea una fecha ISO (con o sin hora/offset) a "dd/MM/yyyy".
     * Extrae yyyy-MM-dd del prefijo sin usar java.time (API 24+ seguro).
     */
    fun fecha(iso: String): String {
        if (iso.isBlank()) return "Fecha no disponible"
        val datePart = iso.take(10)
        if (datePart.length == 10 &&
            datePart[4] == '-' &&
            datePart[7] == '-' &&
            datePart.substring(0, 4).all { it.isDigit() } &&
            datePart.substring(5, 7).all { it.isDigit() } &&
            datePart.substring(8, 10).all { it.isDigit() }
        ) {
            val y = datePart.substring(0, 4)
            val m = datePart.substring(5, 7)
            val d = datePart.substring(8, 10)
            return "$d/$m/$y"
        }
        return datePart.ifBlank { "Fecha no disponible" }
    }
}
