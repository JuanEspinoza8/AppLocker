package com.jair.applocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jair.applocker.ui.screens.AppSelectionScreen
import com.jair.applocker.ui.screens.HomeScreen
import com.jair.applocker.ui.theme.AppLockerTheme
import com.jair.applocker.ui.viewmodels.AppViewModel

class MainActivity : ComponentActivity() {

    // Instanciamos nuestro gestor de estado (ViewModel)
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppLockerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 1. Creamos el controlador de navegación (El equivalente a BrowserRouter)
                    val navController = rememberNavController()

                    // 2. Definimos el mapa de rutas. Le decimos que arranque en "home"
                    NavHost(navController = navController, startDestination = "home") {

                        // Ruta A: Pantalla principal de configuración
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToAppList = {
                                    // Cuando el botón dispara este evento, navegamos
                                    navController.navigate("appList")
                                }
                            )
                        }

                        // Ruta B: Pantalla de la lista de aplicaciones
                        composable("appList") {
                            AppSelectionScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}