package com.jair.applocker.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AdminReceiver : DeviceAdminReceiver() {

    // Qué pasa cuando tu amigo le da permisos de administrador
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Modo estricto activado", Toast.LENGTH_SHORT).show()
    }

    // Qué pasa si se los quita (o intenta quitarlos)
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Modo estricto desactivado", Toast.LENGTH_SHORT).show()
    }
}