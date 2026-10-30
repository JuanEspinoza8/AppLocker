package com.jair.applocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    NavHost(navController = navController, startDestination = "home") {

                        composable("home") {
                            HomeScreen(
                                uiState = uiState,
                                onSaveRules = viewModel::saveRules,
                                onActivateHoliday = viewModel::attemptActivateHoliday,
                                onNavigateToAppList = { navController.navigate("appList") }
                            )
                        }

                        composable("appList") {
                            AppSelectionScreen(uiState = uiState, onToggleApp = viewModel::toggleAppLock)
                        }
                    }
                }
            }
        }
    }
}
