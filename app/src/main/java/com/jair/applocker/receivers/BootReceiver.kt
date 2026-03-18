package com.jair.applocker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class BootReceiver : BroadcastReceiver() {

    // Esta función se ejecuta sola apenas el S23 Ultra termina de encenderse
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {

            // El Servicio de Accesibilidad se levanta solo gracias a Android,
            // pero le clava este cartelito en la pantalla apenas prende el celular
            // para causarle daño psicológico y que sepa que el candado sigue activo jajaja.
            Toast.makeText(
                context,
                "🛡️ AppLocker: Sistema de vigilancia reactivado tras reinicio.",
                Toast.LENGTH_LONG
            ).show()

        }
    }
}