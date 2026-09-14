package com.tasabcv.app

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.util.Locale

/**
 * Formato visual venezolano: puntos de miles, coma decimal (máx. 2 dígitos).
 */
class CurrencyVisualTransformation : VisualTransformation {
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

private val currencyVisual = CurrencyVisualTransformation()

fun formatCurrencyDisplay(raw: String): String =
    currencyVisual.filter(AnnotatedString(raw)).text.text

fun sanitizeInput(newValue: String): String {
    val filtered = newValue.filter { it.isDigit() || it == ',' || it == '.' }
    val standardized = filtered.replace('.', ',')
    val safeRaw = if (standardized.count { it == ',' } > 1) {
        standardized.substring(0, standardized.lastIndexOf(','))
    } else {
        standardized
    }
    return if (safeRaw.contains(",")) {
        val parts = safeRaw.split(",")
        if (parts[1].length > 2) parts[0] + "," + parts[1].take(2) else safeRaw
    } else {
        safeRaw
    }
}

fun formatDoubleToRaw(value: Double): String {
    val str = String.format(Locale.US, "%.2f", value)
    return str.replace('.', ',')
}
