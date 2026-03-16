package com.jair.applocker.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.jair.applocker.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    // 'suspend' indica que esta función es asíncrona.
    // Dispatchers.IO asegura que el trabajo pesado no congele la interfaz.
    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val appsList = mutableListOf<AppInfo>()

        // Pedimos TODAS las apps instaladas en el S23 Ultra
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in packages) {
            // Un celular tiene cientos de procesos de sistema invisibles.
            // Con esta condición, filtramos SOLO las apps que se pueden abrir
            // y tienen un ícono en el menú (redes sociales, juegos, utilidades).
            if (packageManager.getLaunchIntentForPackage(appInfo.packageName) != null) {
                val name = packageManager.getApplicationLabel(appInfo).toString()
                val icon = packageManager.getApplicationIcon(appInfo)

                // Creamos nuestro objeto AppInfo y lo metemos en la lista
                appsList.add(
                    AppInfo(
                        packageName = appInfo.packageName,
                        appName = name,
                        icon = icon
                    )
                )
            }
        }

        // Devolvemos la lista ordenada alfabéticamente para que sea fácil de leer
        return@withContext appsList.sortedBy { it.appName.lowercase() }
    }
}