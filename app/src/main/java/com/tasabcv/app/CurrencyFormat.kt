package com.tasabcv.app

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.util.Locale

/** Máximo de dígitos enteros aceptados en la calculadora (evita desbordes). */
private const val MAX_INT_DIGITS = 12

/**
 * Formato visual venezolano: puntos de miles, coma decimal (máx. 2 dígitos).
 */
object CurrencyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val parts = original.split(",")
        val intPart = parts[0]
        val decPart = if (parts.size > 1) parts[1] else null

        val formattedInt = if (intPart.isNotEmpty() && intPart != "-") {
            val isNegative = intPart.startsWith("-")
            val absInt = if (isNegative) intPart.substring(1) else intPart
            val chunked = absInt.reversed().chunked(3).joinToString(".").reversed()
            if (isNegative) "-$chunked" else chunked
        } else {
            intPart
        }

        val formattedText = when {
            decPart != null -> "$formattedInt,$decPart"
            original.endsWith(",") -> "$formattedInt,"
            else -> formattedInt
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val intPartLength = intPart.length
                if (offset <= intPartLength) {
                    val isNegative = intPart.startsWith("-")
                    val absIntLength = if (isNegative) intPartLength - 1 else intPartLength
                    val absOffset = if (isNegative) offset - 1 else offset
                    if (absOffset <= 0) return offset

                    val digitsToRight = absIntLength - absOffset
                    val totalDots = if (absIntLength > 0) (absIntLength - 1) / 3 else 0
                    val dotsToRight = digitsToRight / 3
                    val dotsToLeft = totalDots - dotsToRight
                    return offset + dotsToLeft
                } else {
                    val isNegative = intPart.startsWith("-")
                    val absIntLength = if (isNegative) intPartLength - 1 else intPartLength
                    val totalDots = if (absIntLength > 0) (absIntLength - 1) / 3 else 0
                    return offset + totalDots
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                var originalOffset = 0
                var currentTransformedOffset = 0
                for (char in formattedText) {
                    if (currentTransformedOffset == offset) break
                    if (char != '.') originalOffset++
                    currentTransformedOffset++
                }
                return originalOffset
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}

fun formatCurrencyDisplay(raw: String): String =
    CurrencyVisualTransformation.filter(AnnotatedString(raw)).text.text

fun sanitizeInput(newValue: String): String {
    val filtered = newValue.filter { it.isDigit() || it == ',' || it == '.' }
    if (filtered.isEmpty()) return ""

    // Si contiene coma decimal (p. ej. al pegar "1.000,00" o editar un monto ya
    // formateado), los puntos anteriores son separadores de miles y se descartan;
    // cualquier separador extra después de la primera coma se ignora.
    val standardized = when {
        filtered.contains(',') -> {
            val commaIndex = filtered.indexOf(',')
            val intDigits = filtered.substring(0, commaIndex)
                .filter { it.isDigit() }
                .take(MAX_INT_DIGITS)
            val decRaw = filtered.substring(commaIndex + 1)
                .takeWhile { it.isDigit() }
                .take(2)
            "$intDigits,$decRaw"
        }
        filtered.count { it == '.' } > 1 -> {
            // Varios puntos sin coma (p. ej. "1.000.000"): son puntos de miles.
            filtered.filter { it.isDigit() }.take(MAX_INT_DIGITS)
        }
        filtered.contains('.') -> {
            // Un punto con exactamente 3 dígitos detrás y una parte entera de
            // 1-3 dígitos (distinta de cero) es separador de miles: "1.000" -> 1000.
            // El resto de puntos sueltos son decimales: "36.50" -> 36,50.
            // Al teclear, el punto se convierte en coma en el primer carácter.
            val dotIndex = filtered.indexOf('.')
            val intDigits = filtered.substring(0, dotIndex)
                .filter { it.isDigit() }
                .take(MAX_INT_DIGITS)
            val decRaw = filtered.substring(dotIndex + 1)
                .takeWhile { it.isDigit() }
            val normalizedInt = intDigits.trimStart('0').ifEmpty { "0" }
            val isThousands = normalizedInt.length in 1..3 &&
                normalizedInt != "0" &&
                decRaw.length == 3
            if (isThousands) {
                (normalizedInt + decRaw).take(MAX_INT_DIGITS)
            } else {
                "$normalizedInt,${decRaw.take(2)}"
            }
        }
        else -> filtered.take(MAX_INT_DIGITS)
    }
    return standardized
}

fun formatDoubleToRaw(value: Double): String {
    val str = String.format(Locale.US, "%.2f", value)
    return str.replace('.', ',')
}
