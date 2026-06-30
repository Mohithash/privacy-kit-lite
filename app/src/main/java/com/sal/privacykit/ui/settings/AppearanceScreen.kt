package com.sal.privacykit.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.sal.privacykit.PrivacyKitApplication
import com.sal.privacykit.data.prefs.ThemeMode
import com.sal.privacykit.ui.components.SectionHeader
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.preference.RadioButtonPreference

@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as PrivacyKitApplication
    val preferences = application.container.appPreferences
    val scope = rememberCoroutineScope()

    val themeMode by preferences.themeMode.collectAsState(initial = ThemeMode.DARK)

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "Appearance",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
            item { SectionHeader("Theme") }
            items(ThemeMode.entries.toList()) { mode ->
                RadioButtonPreference(
                    title = mode.label(),
                    selected = themeMode == mode,
                    onClick = { scope.launch { preferences.setThemeMode(mode) } },
                )
            }
        }
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System default"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
