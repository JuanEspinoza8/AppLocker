package com.jair.applocker.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.jair.applocker.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    /**
     * Apps instaladas que tienen ícono en el launcher, ordenadas por nombre. Corre en
     * [Dispatchers.IO] porque cargar etiquetas e íconos es lento.
     */
    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val appsList = mutableListOf<AppInfo>()

        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in packages) {
            // Se descartan servicios y procesos del sistema que no se pueden abrir
            if (packageManager.getLaunchIntentForPackage(appInfo.packageName) != null) {
                val name = packageManager.getApplicationLabel(appInfo).toString()
                val icon = packageManager.getApplicationIcon(appInfo)

                appsList.add(
                    AppInfo(
                        packageName = appInfo.packageName,
                        appName = name,
                        icon = icon
                    )
                )
            }
        }

        return@withContext appsList.sortedBy { it.appName.lowercase() }
    }
}
