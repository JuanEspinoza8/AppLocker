package com.jair.applocker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.jair.applocker.R

/**
 * Avisa al encender el teléfono que el bloqueo sigue activo.
 *
 * No arranca nada: Android vuelve a levantar solo el servicio de accesibilidad si estaba
 * habilitado antes del reinicio.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Toast.makeText(context, R.string.boot_message, Toast.LENGTH_LONG).show()
        }
    }
}
