package com.tasabcv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    private val viewModel: DolarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge: el contenido se extiende bajo las barras del sistema
        // (estilo moderno) y el Scaffold aplica los insets. A prueba de
        // Android 15, que fuerza este modo.
        enableEdgeToEdge()
        viewModel.loadOnFirstOpen()

        setContent {
            DolarBCVTheme {
                MainScreen(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }
}

@Composable
fun MainScreen(viewModel: DolarViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "tasas"
    val tabTasas = stringResource(R.string.nav_tab_tasas)
    val tabCalculadora = stringResource(R.string.nav_tab_calculadora)
    val tabLabels = listOf(tabTasas, tabCalculadora)

    Scaffold(
        bottomBar = {
            Surface(
                color = appColors.backgroundBottom,
                modifier = Modifier
                    .fillMaxWidth()
                    // Bajo edge-to-edge la barra se extiende hasta el borde;
                    // este padding deja el contenido por encima de la barra de
                    // gestos/navegación del sistema.
                    .navigationBarsPadding()
            ) {
                val selectedIndex = if (currentRoute == "calculadora") 1 else 0
                SlidingSegmentedControl(
                    options = tabLabels,
                    selectedIndex = selectedIndex,
                    onOptionSelected = { index ->
                        val route = if (index == 0) "tasas" else "calculadora"
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    height = 64.dp,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(32.dp))
                ) { label, _, contentColor ->
                    NavItemContent(
                        label = label,
                        icon = if (label == tabTasas) Icons.Filled.AttachMoney else Icons.Filled.Calculate,
                        contentColor = contentColor
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "tasas",
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            enterTransition = {
                // Aparición con "peso": crece un poco de más y se asienta (overshoot),
                // mismo lenguaje de movimiento que el indicador deslizante (§13).
                fadeIn(animationSpec = tween(220)) +
                    scaleIn(
                        initialScale = 0.92f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = 380f
                        )
                    )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(180)) +
                    scaleOut(targetScale = 1.02f, animationSpec = tween(180))
            }
        ) {
            composable("tasas") { TasasScreen(viewModel) }
            composable("calculadora") { CalculadoraScreen(viewModel) }
        }
    }
}

@Composable
private fun NavItemContent(label: String, icon: ImageVector, contentColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
