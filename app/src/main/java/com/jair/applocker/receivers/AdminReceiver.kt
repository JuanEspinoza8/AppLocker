package com.jair.applocker.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.jair.applocker.R

/**
 * Administrador del dispositivo. Mientras está activo, Android no deja desinstalar la app
 * sin desactivarlo antes desde Ajustes, que el servicio bloquea fuera de la ventana de edición.
 */
class AdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, R.string.admin_enabled, Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, R.string.admin_disabled, Toast.LENGTH_SHORT).show()
    }
}
