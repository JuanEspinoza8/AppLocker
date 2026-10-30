package com.jair.applocker.data.model

import android.graphics.drawable.Drawable

/** App instalada que se puede elegir para bloquear. */
data class AppInfo(
    /** Identificador único, por ejemplo `com.instagram.android`. */
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    /** Si el usuario la eligió para bloquear. */
    var isLocked: Boolean = false
)
