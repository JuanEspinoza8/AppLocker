package com.jair.applocker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    // Esta función se ejecuta sola cuando el S23 Ultra termina de encenderse
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Acá vamos a poner la lógica para reactivar el bloqueo si el temporizador seguía activo
        }
    }
}