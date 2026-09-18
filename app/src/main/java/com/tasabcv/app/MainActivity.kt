package com.tasabcv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Settings
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

/** Rutas de tabs en orden de la barra inferior: Ajustes · Tasas · Calculadora. */
private val tabRoutes = listOf("ajustes", "tasas", "calculadora")

/** Índice del tab para transiciones direccionales e indicador de la barra. */
private fun routeIndex(route: String?): Int = when (route) {
    "ajustes" -> 0
    "calculadora" -> 2
    else -> 1
}

@Composable
fun MainScreen(viewModel: DolarViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "tasas"
    val tabAjustes = stringResource(R.string.nav_tab_ajustes)
    val tabTasas = stringResource(R.string.nav_tab_tasas)
    val tabCalculadora = stringResource(R.string.nav_tab_calculadora)
    val tabLabels = listOf(tabAjustes, tabTasas, tabCalculadora)

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
                val selectedIndex = routeIndex(currentRoute)
                SlidingSegmentedControl(
                    options = tabLabels,
                    selectedIndex = selectedIndex,
                    onOptionSelected = { index ->
                        val route = tabRoutes[index]
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
                        icon = when (label) {
                            tabAjustes -> Icons.Filled.Settings
                            tabCalculadora -> Icons.Filled.Calculate
                            else -> Icons.Filled.AttachMoney
                        },
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
            // Transición de tabs DIRECCIONAL y coherente con el indicador de
            // la barra (3 tabs: Ajustes 0 · Tasas 1 · Calculadora 2): la
            // pantalla nueva entra deslizando desde el lado del tab elegido
            // (comparación de índices, no de ruta); la anterior se retira al
            // lado opuesto. Muelles compartidos (§10): fluido, sin
            // agrandamientos ni sobresaltos.
            enterTransition = {
                val forward = routeIndex(targetState.destination.route) >
                    routeIndex(initialState.destination.route)
                slideInHorizontally(MotionSpringIntOffset) { full ->
                    if (forward) full / 4 else -full / 4
                } + fadeIn(animationSpec = tween(200))
            },
            exitTransition = {
                val forward = routeIndex(targetState.destination.route) >
                    routeIndex(initialState.destination.route)
                slideOutHorizontally(MotionSpringIntOffset) { full ->
                    if (forward) -full / 6 else full / 6
                } + fadeOut(animationSpec = tween(160))
            }
        ) {
            composable("ajustes") { ConfiguracionScreen() }
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
