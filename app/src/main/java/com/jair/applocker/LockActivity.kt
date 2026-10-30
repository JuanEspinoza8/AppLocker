package com.jair.applocker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import com.jair.applocker.ui.screens.LockScreenOverlay
import com.jair.applocker.ui.theme.AppLockerTheme

/** Pantalla que tapa una app bloqueada. La lanza el servicio de accesibilidad. */
class LockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppLockerTheme {
                // "Atrás" no hace nada: la única salida es ir a la pantalla de inicio
                BackHandler(enabled = true) { }

                LockScreenOverlay(
                    onGoHomeClick = {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                        finish()
                    }
                )
            }
        }
    }
}