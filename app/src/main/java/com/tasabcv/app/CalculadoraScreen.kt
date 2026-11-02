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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val FieldShape = RoundedCornerShape(16.dp)
private val QuickAmountPresets = listOf(5, 10, 20, 50)

@Composable
fun CalculadoraScreen(viewModel: DolarViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var isUsdBase by rememberSaveable { mutableStateOf(true) }

    var baseText by rememberSaveable { mutableStateOf("") }
    var bsText by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val colors = appColors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.backgroundTop, colors.backgroundBottom)
                )
            )
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        // SIN título y SIN scroll: el contenido arranca directo con el mismo
        // marco exterior y separación entre bloques que Tasas y Ajustes, y se
        // compacta solo si el teclado reduce el alto disponible.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = MaxContentWidth)
        ) {
            val availH = maxHeight
            val gap = UniversalGap

            val selectorHeight = when {
                availH < 360.dp -> 40.dp
                availH < 460.dp -> 48.dp
                else -> 56.dp
            }
            val cardPadding = if (availH < 380.dp) 10.dp else if (availH < 520.dp) 12.dp else 16.dp
            val swapBoxHeight = when {
                availH < 380.dp -> 24.dp
                availH < 500.dp -> 34.dp
                else -> 44.dp
            }
            val swapCircleSize = if (availH < 380.dp) 22.dp else 30.dp
            val quickBtnHeight = when {
                availH < 380.dp -> 32.dp
                availH < 500.dp -> 38.dp
                else -> 42.dp
            }
            val fieldFontSize = when {
                availH < 380.dp -> 18f
                availH < 500.dp -> 21f
                else -> 24f
            }
            val copyBtnSize = when {
                availH < 380.dp -> 32.dp
                availH < 500.dp -> 36.dp
                else -> 40.dp
            }
            val maxTextScale = if (availH >= 620.dp) 1.15f else 1f

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = UniversalGap, end = UniversalGap, top = UniversalGap),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FloatingSegmentedButton(
                    options = listOf(
                        stringResource(R.string.currency_usd_title),
                        stringResource(R.string.currency_eur_title)
                    ),
                    selectedIndex = if (isUsdBase) 0 else 1,
                    height = selectorHeight,
                    onOptionSelected = { idx ->
                        val newIsUsd = idx == 0
                        if (isUsdBase != newIsUsd) {
                            isUsdBase = newIsUsd
                            baseText = ""
                            bsText = ""
                        }
                    }
                )

                Spacer(Modifier.height(gap))

                // Transición direccional tipo carrusel al alternar USD↔EUR:
                // USD→EUR desliza a la izquierda; EUR→USD a la derecha.
                // El selector de arriba NO se mueve (solo su indicador interior).
                // Ocupa TODO el alto restante y centra la ficha: sin huecos
                // muertos abajo en teléfonos altos.
                AnimatedContent(
                    targetState = isUsdBase,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
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
                                fontSize = 14f.boundedSp(maxTextScale),
                                color = colors.muted,
                                textAlign = TextAlign.Center
                            )
                        }
                        is SingleRateState.Empty -> {
                            Text(
                                text = "No hay tasa disponible para $activeSymbol. Verifique su conexión en la pestaña Tasas.",
                                fontSize = 14f.boundedSp(maxTextScale),
                                color = colors.error,
                                textAlign = TextAlign.Center
                            )
                        }
                        is SingleRateState.Loaded -> {
                            val activeTasa = activeState.rate.promedio
                            if (activeTasa <= 0.0) {
                                Text(
                                    text = "No hay tasa disponible para $activeSymbol. Verifique su conexión en la pestaña Tasas.",
                                    fontSize = 14f.boundedSp(maxTextScale),
                                    color = colors.error,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glassRaised(
                                            colors,
                                            RoundedCornerShape(CardCornerRadius),
                                            elevation = 12.dp
                                        )
                                        .padding(cardPadding)
                                ) {
                                    val fieldSymbol = activeSymbol
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
                                            placeholder = fieldSymbol,
                                            fontSize = fieldFontSize,
                                            maxFontScale = maxTextScale,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(UniversalGap))
                                        CopyIconButton(buttonSize = copyBtnSize) {
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
                                            .height(swapBoxHeight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(swapCircleSize)
                                                .glassRaised(colors, CircleShape, elevation = 4.dp)
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.SwapVert,
                                                contentDescription = null,
                                                tint = colors.accent,
                                                modifier = Modifier.size(if (availH < 340.dp) 15.dp else 18.dp)
                                            )
                                        }
                                    }

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
                                            placeholder = "Bs",
                                            fontSize = fieldFontSize,
                                            maxFontScale = maxTextScale,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(UniversalGap))
                                        CopyIconButton(buttonSize = copyBtnSize) {
                                            copyToClipboard(
                                                context,
                                                formatCurrencyDisplay(bsText),
                                                "Bs"
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(gap))
                                    val currencySign = if (usdBase) "$" else "€"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(UniversalGap),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        QuickAmountPresets.forEach { amount ->
                                            QuickAmountButton(
                                                label = "$amount $currencySign",
                                                buttonHeight = quickBtnHeight,
                                                maxFontScale = maxTextScale,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    val raw = amount.toString()
                                                    baseText = raw
                                                    bsText = formatDoubleToRaw(amount * activeTasa)
                                                }
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
    }
}

/**
 * Botón de atajo rápido (5, 10, 20, 50 $/€): coloca la cantidad en el campo
 * de divisas y calcula al instante su equivalente en bolívares.
 */
@Composable
private fun QuickAmountButton(
    label: String,
    buttonHeight: Dp,
    maxFontScale: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = appColors
    Box(
        modifier = modifier
            .height(buttonHeight)
            .glassRaised(colors, FieldShape, elevation = 5.dp)
            .pulseClick(pressedScale = 0.94f) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 15f.boundedSp(maxFontScale),
            fontWeight = FontWeight.SemiBold,
            color = colors.accent,
            maxLines = 1
        )
    }
}

/**
 * Campo de la calculadora dentro de un hueco hundido (bisel invertido):
 * el TextField pinta transparente y el relieve lo pone el contenedor.
 * Las siglas de la moneda ([placeholder]) viven DENTRO de la píldora al lado
 * izquierdo, como pista DISCRETA (16sp): aparecen SOLO con el campo vacío
 * (primitivo nativo de Material3) y desaparecen en cuanto el usuario escribe.
 * La cápsula mide exactamente lo mismo que antes: el placeholder nunca la
 * agranda (su altura es menor a la del número que se escribe).
 */
@Composable
private fun InsetCalcField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    fontSize: Float,
    maxFontScale: Float,
    modifier: Modifier = Modifier
) {
    val colors = appColors
    Box(
        modifier = modifier.glassInset(colors, FieldShape)
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            visualTransformation = CurrencyVisualTransformation,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            shape = FieldShape,
            colors = calcFieldColors(),
            textStyle = TextStyle(
                fontSize = fontSize.boundedSp(maxFontScale),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start
            ),
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = 16f.boundedSp(maxFontScale),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.muted,
                    textAlign = TextAlign.Start,
                    maxLines = 1
                )
            }
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
    cursorColor = appColors.accent,
    focusedPlaceholderColor = appColors.muted,
    unfocusedPlaceholderColor = appColors.muted
)

/** Botón circular de copiar: pulso one-shot al tocar (sin estado persistente). */
@Composable
private fun CopyIconButton(buttonSize: Dp, onClick: () -> Unit) {
    val colors = appColors

    Box(
        modifier = Modifier
            .size(buttonSize)
            .glassRaised(colors, CircleShape, elevation = 5.dp)
            .pulseClick {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.ContentCopy,
            contentDescription = "Copiar",
            tint = colors.accent,
            modifier = Modifier.size(if (buttonSize < 38.dp) 16.dp else 18.dp)
        )
    }
}
