package com.jair.applocker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import com.jair.applocker.ui.screens.LockScreenOverlay
import com.jair.applocker.ui.theme.AppLockerTheme

class LockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppLockerTheme {
                // Interceptamos el botón físico de "Atrás" del celular
                // Al dejar esto vacío, el botón Atrás no hace nada. ¡Queda atrapado!
                BackHandler(enabled = true) { }

                LockScreenOverlay(
                    onGoHomeClick = {
                        // Lo mandamos obligatoriamente a la pantalla de inicio del celular
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                        finish() // Cerramos el muro
                    }
                )
            }
        }
    }
}