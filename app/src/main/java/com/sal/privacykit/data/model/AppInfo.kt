package com.sal.privacykit.data.model

import android.graphics.drawable.Drawable

/**
 * A resolved installed app. [id] and [packageName] are the same value — appId across the data
 * layer is the real Android package name, not a synthetic catalog id.
 */
data class AppInfo(
    val id: String,
    val name: String,
    val packageName: String = id,
    val icon: Drawable? = null,
)
