package com.jair.applocker.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.jair.applocker.LockActivity
import com.jair.applocker.data.local.DataStoreManager
import kotlinx.coroutines.*
import java.util.Calendar

class LockerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var dataStoreManager: DataStoreManager

    // Variables de Bloqueo
    private var lockedApps: Set<String> = emptySet()
    private var lockedDays: Set<String> = emptySet()
    private var lockStartTime: String = "00:00"
    private var lockEndTime: String = "00:00"

    // Variables de la Ventana de Edición (Salida)
    private var editDay: Int? = null
    private var editStartTime: String = "00:00"
    private var editEndTime: String = "00:00"

    override fun onServiceConnected() {
        super.onServiceConnected()
        dataStoreManager = DataStoreManager(applicationContext)

        // Escuchamos TODA la base de datos en tiempo real
        serviceScope.launch {
            launch { dataStoreManager.lockedAppsFlow.collect { lockedApps = it } }
            launch { dataStoreManager.lockedDaysFlow.collect { lockedDays = it } }
            launch { dataStoreManager.lockStartTimeFlow.collect { lockStartTime = it } }
            launch { dataStoreManager.lockEndTimeFlow.collect { lockEndTime = it } }

            launch { dataStoreManager.editDayFlow.collect { editDay = it } }
            launch { dataStoreManager.editStartTimeFlow.collect { editStartTime = it } }
            launch { dataStoreManager.editEndTimeFlow.collect { editEndTime = it } }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            // Evitamos que la app se bloquee a sí misma o al teclado/sistema base
            if (packageName == "com.jair.applocker" || packageName.contains("systemui")) return

            // --- SISTEMA ANTI-TRAMPAS (Bloqueo de Ajustes) ---
            // Si intenta abrir la Configuración del celular y NO es hora de editar: Bloqueo directo.
            if (packageName == "com.android.settings" || packageName == "com.samsung.android.settings") {
                if (!isEditWindowActiveNow()) {
                    launchLockScreen()
                    return // Cortamos la ejecución acá
                }
            }

            // --- SISTEMA DE BLOQUEO NORMAL ---
            if (lockedApps.contains(packageName)) {
                if (isLockActiveNow()) {
                    launchLockScreen()
                }
            }
        }
    }

    // --- CEREBRO 1: ¿Es hora de bloqueo de apps? ---
    private fun isLockActiveNow(): Boolean {
        if (lockedDays.isEmpty()) return false

        val calendar = Calendar.getInstance()
        val currentDayAndroid = calendar.get(Calendar.DAY_OF_WEEK)
        val currentDayMapped = if (currentDayAndroid == Calendar.SUNDAY) 7 else currentDayAndroid - 1

        if (!lockedDays.contains(currentDayMapped.toString())) return false

        val currentTotalMinutes = (calendar.get(Calendar.HOUR_OF_DAY) * 60) + calendar.get(Calendar.MINUTE)
        val startParts = lockStartTime.split(":")
        val startTotalMinutes = (startParts[0].toInt() * 60) + startParts[1].toInt()
        val endParts = lockEndTime.split(":")
        val endTotalMinutes = (endParts[0].toInt() * 60) + endParts[1].toInt()

        return if (startTotalMinutes <= endTotalMinutes) {
            currentTotalMinutes in startTotalMinutes..endTotalMinutes
        } else {
            currentTotalMinutes >= startTotalMinutes || currentTotalMinutes <= endTotalMinutes
        }
    }

    // --- CEREBRO 2: ¿Es la Ventana de Edición permitida? ---
    private fun isEditWindowActiveNow(): Boolean {
        if (editDay == null) return true // Si no configuró salida, siempre está libre

        val calendar = Calendar.getInstance()
        val currentDayAndroid = calendar.get(Calendar.DAY_OF_WEEK)
        val currentDayMapped = if (currentDayAndroid == Calendar.SUNDAY) 7 else currentDayAndroid - 1

        if (currentDayMapped != editDay) return false

        val currentTotalMinutes = (calendar.get(Calendar.HOUR_OF_DAY) * 60) + calendar.get(Calendar.MINUTE)
        val startParts = editStartTime.split(":")
        val startTotalMinutes = (startParts[0].toInt() * 60) + startParts[1].toInt()
        val endParts = editEndTime.split(":")
        val endTotalMinutes = (endParts[0].toInt() * 60) + endParts[1].toInt()

        return if (startTotalMinutes <= endTotalMinutes) {
            currentTotalMinutes in startTotalMinutes..endTotalMinutes
        } else {
            currentTotalMinutes >= startTotalMinutes || currentTotalMinutes <= endTotalMinutes
        }
    }

    private fun launchLockScreen() {
        val intent = Intent(this, LockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}