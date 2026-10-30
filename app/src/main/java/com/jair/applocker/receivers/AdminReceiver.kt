package com.jair.applocker.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Administrador del dispositivo. Mientras está activo, Android no deja desinstalar la app
 * sin desactivarlo antes desde Ajustes, que el servicio bloquea fuera de la ventana de edición.
 */
class AdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Modo estricto activado", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Modo estricto desactivado", Toast.LENGTH_SHORT).show()
    }
}
