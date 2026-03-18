package com.jair.applocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jair.applocker.ui.screens.AppSelectionScreen
import com.jair.applocker.ui.screens.HomeScreen
import com.jair.applocker.ui.theme.AppLockerTheme
import com.jair.applocker.ui.viewmodels.AppViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppLockerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "home") {

                        composable("home") {
                            // Acá escuchamos si la pantalla debe estar bloqueada o no
                            val isEditable by viewModel.isEditable.collectAsState()

                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToAppList = {
                                    navController.navigate("appList")
                                },
                                isEditable = isEditable
                            )
                        }

                        composable("appList") {
                            AppSelectionScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}