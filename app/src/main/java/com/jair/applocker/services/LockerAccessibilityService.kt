package com.jair.applocker.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.jair.applocker.LockActivity
import com.jair.applocker.data.repository.LockSettingsRepository
import com.jair.applocker.domain.LockPolicy
import com.jair.applocker.domain.LockSettings
import kotlinx.coroutines.*

class LockerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val lockPolicy = LockPolicy()

    // Última configuración leída; null hasta que DataStore emite por primera vez
    @Volatile
    private var settings: LockSettings? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val repository = LockSettingsRepository(applicationContext)
        serviceScope.launch { repository.settings.collect { settings = it } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val eventPackage = event.packageName?.toString() ?: return
            val currentSettings = settings ?: return

            if (eventPackage == packageName || eventPackage.contains("systemui")) return

            if (lockPolicy.shouldBlock(eventPackage, currentSettings)) {
                launchLockScreen()
            }
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
