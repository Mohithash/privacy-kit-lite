package com.sal.privacykit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.sal.privacykit.data.model.AppInfo
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppIconBadge(app: AppInfo, modifier: Modifier = Modifier, size: Int = 40) {
    val icon = app.icon
    Box(
        modifier = modifier
            .size(size.dp)
            .background(MiuixTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            val bitmap = remember(icon) { icon.toBitmap().asImageBitmap() }
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size((size * 0.8f).dp),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Apps,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size((size * 0.55f).dp),
            )
        }
    }
}

@Composable
fun DeviceIconBadge(modifier: Modifier = Modifier, size: Int = 40) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(MiuixTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Smartphone,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size((size * 0.55f).dp),
        )
    }
}

@Composable
fun GenericIconBadge(icon: ImageVector, modifier: Modifier = Modifier, size: Int = 40) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(MiuixTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size((size * 0.55f).dp),
        )
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    SmallTitle(text = title, modifier = modifier)
}

val ScreenContentPadding = PaddingValues(bottom = 16.dp)
