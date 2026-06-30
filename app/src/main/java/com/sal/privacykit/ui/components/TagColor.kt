package com.sal.privacykit.ui.components

import androidx.compose.ui.graphics.Color
import com.sal.privacykit.data.model.ProfileTagColor

fun tagDotColor(tag: ProfileTagColor): Color = when (tag) {
    ProfileTagColor.RED -> Color(0xFFE94634)
    ProfileTagColor.BLUE -> Color(0xFF277AF7)
    ProfileTagColor.GREEN -> Color(0xFF4CAF50)
}
