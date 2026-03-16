package com.jair.applocker.data.model

import android.graphics.drawable.Drawable

// En Kotlin, una 'data class' es ideal para almacenar estado.
// Es el equivalente a una 'interface' o 'type' en TypeScript.
data class AppInfo(
    val packageName: String, // El ID único, ej: "com.instagram.android"
    val appName: String,     // El nombre legible, ej: "Instagram"
    val icon: Drawable,      // El logo de la app para mostrarlo en la lista
    var isLocked: Boolean = false // Para saber si tu amigo la tildó para bloquear
)