package com.tasabcv.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FieldShape = RoundedCornerShape(16.dp)

@Composable
fun CalculadoraScreen(viewModel: DolarViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var isUsdBase by rememberSaveable { mutableStateOf(true) }

    var baseText by rememberSaveable { mutableStateOf("") }
    var bsText by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    val currencyVisual = remember { CurrencyVisualTransformation() }
    val colors = appColors

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
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.screen_calculadora_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingSegmentedButton(
                    options = listOf(
                        stringResource(R.string.currency_usd_title),
                        stringResource(R.string.currency_eur_title)
                    ),
                    selectedIndex = if (isUsdBase) 0 else 1,
                    onOptionSelected = { idx ->
                        val newIsUsd = idx == 0
                        if (isUsdBase != newIsUsd) {
                            isUsdBase = newIsUsd
                            baseText = ""
                            bsText = ""
                        }
                    }
                )

                Spacer(Modifier.height(24.dp))

                // Transición direccional tipo carrusel al alternar USD↔EUR:
                // USD→EUR desliza a la izquierda; EUR→USD a la derecha.
                // El selector de arriba NO se mueve (solo su indicador interior).
                AnimatedContent(
                    targetState = isUsdBase,
                    transitionSpec = {
                        val direction = if (targetState) {
                            // EUR → USD: desplazar a la derecha.
                            AnimatedContentTransitionScope.SlideDirection.Right
                        } else {
                            // USD → EUR: desplazar a la izquierda.
                            AnimatedContentTransitionScope.SlideDirection.Left
                        }
                        (fadeIn(tween(200)) +
                            slideIntoContainer(direction, animationSpec = MotionSpringIntOffset)) togetherWith
                            (fadeOut(tween(160)) +
                                slideOutOfContainer(direction, animationSpec = MotionSpringIntOffset))
                    },
                    label = "CalcCurrencySwitch"
                ) { usdBase ->
                    val activeState = if (usdBase) uiState.usdState else uiState.eurState
                    val activeSymbol = if (usdBase) "USD" else "EUR"

                    when (activeState) {
                        is SingleRateState.Loading -> {
                            Text(
                                text = "Cargando la tasa de $activeSymbol...",
                                color = colors.muted,
                                textAlign = TextAlign.Center
                            )
                        }
                        is SingleRateState.Empty -> {
                            Text(
                                text = "No hay tasa disponible para $activeSymbol. Verifique su conexión en la pestaña Tasas.",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                        is SingleRateState.Loaded -> {
                            val activeTasa = activeState.rate.promedio
                            if (activeTasa <= 0.0) {
                                Text(
                                    text = "No hay tasa disponible para $activeSymbol. Verifique su conexión en la pestaña Tasas.",
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .neuRaised(colors, RoundedCornerShape(32.dp), elevation = 12.dp)
                                        .padding(16.dp)
                                ) {
                                    val fieldSymbol = activeSymbol
                                    Text(
                                        text = "Monto en $fieldSymbol",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.muted,
                                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        InsetCalcField(
                                            value = baseText,
                                            onValueChange = { newValue ->
                                                val raw = sanitizeInput(newValue)
                                                baseText = raw
                                                val parsed = Format.parseUserInput(raw)
                                                bsText = if (parsed != null && activeTasa > 0.0) {
                                                    formatDoubleToRaw(parsed * activeTasa)
                                                } else {
                                                    ""
                                                }
                                            },
                                            currencyVisual = currencyVisual,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        CopyIconButton {
                                            copyToClipboard(
                                                context,
                                                formatCurrencyDisplay(baseText),
                                                fieldSymbol
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .neuRaised(colors, CircleShape, elevation = 4.dp)
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.SwapVert,
                                                contentDescription = null,
                                                tint = colors.accent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Monto en Bolívares",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.muted,
                                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        InsetCalcField(
                                            value = bsText,
                                            onValueChange = { newValue ->
                                                val raw = sanitizeInput(newValue)
                                                bsText = raw
                                                val parsed = Format.parseUserInput(raw)
                                                baseText = if (parsed != null && activeTasa > 0.0) {
                                                    formatDoubleToRaw(parsed / activeTasa)
                                                } else {
                                                    ""
                                                }
                                            },
                                            currencyVisual = currencyVisual,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        CopyIconButton {
                                            copyToClipboard(
                                                context,
                                                formatCurrencyDisplay(bsText),
                                                "Bs"
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(32.dp))
                                    Text(
                                        text = "Referencia: 1 $fieldSymbol = ${Format.number(activeTasa)} Bs",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.muted,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Campo de la calculadora dentro de un hueco hundido (bisel invertido):
 * el TextField pinta transparente y el relieve lo pone el contenedor.
 */
@Composable
private fun InsetCalcField(
    value: String,
    onValueChange: (String) -> Unit,
    currencyVisual: CurrencyVisualTransformation,
    modifier: Modifier = Modifier
) {
    val colors = appColors
    Box(
        modifier = modifier.neuInset(colors, FieldShape)
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            visualTransformation = currencyVisual,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            shape = FieldShape,
            colors = calcFieldColors(),
            textStyle = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start
            )
        )
    }
}

@Composable
private fun calcFieldColors() = TextFieldDefaults.colors(
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedTextColor = appColors.onSurface,
    unfocusedTextColor = appColors.onSurface,
    cursorColor = appColors.accent
)

/** Botón circular de copiar: pulso one-shot al tocar (sin estado persistente). */
@Composable
private fun CopyIconButton(onClick: () -> Unit) {
    val colors = appColors

    Box(
        modifier = Modifier
            .size(40.dp)
            .neuRaised(colors, CircleShape, elevation = 5.dp)
            .pulseClick {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.ContentCopy,
            contentDescription = "Copiar",
            tint = colors.accent,
            modifier = Modifier.size(18.dp)
        )
    }
}
