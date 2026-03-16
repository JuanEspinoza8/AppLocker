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

    // Variables en memoria para acceso hiper-rápido
    private var lockedApps: Set<String> = emptySet()
    private var lockedDays: Set<String> = emptySet()
    private var lockStartTime: String = "00:00"
    private var lockEndTime: String = "00:00"

    override fun onServiceConnected() {
        super.onServiceConnected()
        dataStoreManager = DataStoreManager(applicationContext)

        // Escuchamos TODAS las configuraciones al mismo tiempo
        serviceScope.launch {
            launch { dataStoreManager.lockedAppsFlow.collect { lockedApps = it } }
            launch { dataStoreManager.lockedDaysFlow.collect { lockedDays = it } }
            launch { dataStoreManager.lockStartTimeFlow.collect { lockStartTime = it } }
            launch { dataStoreManager.lockEndTimeFlow.collect { lockEndTime = it } }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            if (packageName == "com.jair.applocker" || packageName.contains("systemui")) return

            // 1. ¿Está en la lista de apps prohibidas?
            if (lockedApps.contains(packageName)) {
                // 2. ¿Estamos dentro del día y la hora de castigo?
                if (isLockActiveNow()) {
                    launchLockScreen()
                }
            }
        }
    }

    // --- EL CEREBRO DEL RELOJ ---
    private fun isLockActiveNow(): Boolean {
        if (lockedDays.isEmpty()) return false // Si no configuró días, no bloqueamos

        val calendar = Calendar.getInstance()

        // 1. Verificar el día
        val currentDayAndroid = calendar.get(Calendar.DAY_OF_WEEK)
        // Android toma Domingo=1, Lunes=2. Nosotros usamos Lunes=1... Domingo=7
        val currentDayMapped = if (currentDayAndroid == Calendar.SUNDAY) 7 else currentDayAndroid - 1

        if (!lockedDays.contains(currentDayMapped.toString())) {
            return false // Hoy no es día de bloqueo
        }

        // 2. Verificar la hora (Pasamos todo a minutos para compararlo fácil)
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTotalMinutes = (currentHour * 60) + currentMinute

        val startParts = lockStartTime.split(":")
        val startTotalMinutes = (startParts[0].toInt() * 60) + startParts[1].toInt()

        val endParts = lockEndTime.split(":")
        val endTotalMinutes = (endParts[0].toInt() * 60) + endParts[1].toInt()

        // Evaluamos si el horario cruza la medianoche (ej: 22:00 a 02:00) o es normal (09:00 a 18:00)
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