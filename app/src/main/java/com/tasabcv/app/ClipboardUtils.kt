package com.tasabcv.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

fun copyToClipboard(context: Context, text: String, label: String) {
    if (text.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Copiado: $text $label", Toast.LENGTH_SHORT).show()
}

fun copyRate(context: Context, rate: DolarRate, currency: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Tasa $currency", Format.number(rate.promedio)))
    Toast.makeText(context, "Tasa $currency copiada", Toast.LENGTH_SHORT).show()
}
