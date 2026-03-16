package com.jair.applocker.data.model

data class LockConfig(
    // 1. HORARIO DE BLOQUEO DE APPS
    // Guardamos los días seleccionados (1 = Lunes, 7 = Domingo)
    val lockedDays: Set<Int> = emptySet(),
    val lockStartHour: Int = 0,
    val lockStartMinute: Int = 0,
    val lockEndHour: Int = 0,
    val lockEndMinute: Int = 0,

    // 2. VENTANA DE EDICIÓN (Cuando puede modificar esta app)
    // Si no configura esto, por defecto siempre se puede editar (peligroso para él)
    val editWindowDay: Int? = null,
    val editStartHour: Int = 0,
    val editStartMinute: Int = 0,
    val editEndHour: Int = 0,
    val editEndMinute: Int = 0,

    // Switch maestro para saber si el sistema está operando
    val isSystemActive: Boolean = false
)