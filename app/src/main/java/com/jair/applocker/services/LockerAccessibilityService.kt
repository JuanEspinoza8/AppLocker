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

    // Bloqueo de apps
    private var lockedApps: Set<String> = emptySet()
    private var lockedDays: Set<String> = emptySet()
    private var lockStartTime: String = "00:00"
    private var lockEndTime: String = "00:00"

    // Salidas permitidas
    private var editDay: Int? = null
    private var editStartTime: String = "00:00"
    private var editEndTime: String = "00:00"
    private var holidayUntil: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        dataStoreManager = DataStoreManager(applicationContext)

        serviceScope.launch {
            launch { dataStoreManager.lockedAppsFlow.collect { lockedApps = it } }
            launch { dataStoreManager.lockedDaysFlow.collect { lockedDays = it } }
            launch { dataStoreManager.lockStartTimeFlow.collect { lockStartTime = it } }
            launch { dataStoreManager.lockEndTimeFlow.collect { lockEndTime = it } }

            launch { dataStoreManager.editDayFlow.collect { editDay = it } }
            launch { dataStoreManager.editStartTimeFlow.collect { editStartTime = it } }
            launch { dataStoreManager.editEndTimeFlow.collect { editEndTime = it } }
            launch { dataStoreManager.holidayUntilFlow.collect { holidayUntil = it } }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            if (packageName == "com.jair.applocker" || packageName.contains("systemui")) return

            // PROTECCIÓN DE AJUSTES (Para evitar desinstalación forzada)
            if (packageName == "com.android.settings" || packageName == "com.samsung.android.settings") {
                // Si el sistema no está en modo "Editable" (sin franco y sin ventana de edición), bloqueamos
                if (!isSystemEditableNow()) {
                    launchLockScreen()
                    return
                }
            }

            // BLOQUEO DE APPS NORMAL
            if (lockedApps.contains(packageName)) {
                if (isLockActiveNow()) {
                    launchLockScreen()
                }
            }
        }
    }

    // Calcula si estamos dentro de la Ventana de Edición o del PIN de Vacaciones
    private fun isSystemEditableNow(): Boolean {
        // 1. ¿Está de vacaciones/franco?
        if (System.currentTimeMillis() < holidayUntil) return true

        // 2. ¿Es el horario de edición semanal?
        if (editDay != null) {
            val calendar = Calendar.getInstance()
            val currentDayMapped = if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else calendar.get(Calendar.DAY_OF_WEEK) - 1

            if (currentDayMapped == editDay) {
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
        }
        return false // Si no hay franco ni es hora de edición, está bloqueado
    }

    private fun isLockActiveNow(): Boolean {
        if (lockedDays.isEmpty()) return false

        val calendar = Calendar.getInstance()
        val currentDayMapped = if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else calendar.get(Calendar.DAY_OF_WEEK) - 1

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