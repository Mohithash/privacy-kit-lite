package com.sal.privacykit.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.sal.privacykit.PrivacyKitApplication
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
fun DeveloperSettingsScreen(onBack: () -> Unit, onCustomHooksClick: () -> Unit) {
    val application = LocalContext.current.applicationContext as PrivacyKitApplication
    val container = application.container
    val scope = rememberCoroutineScope()
    val profileOrganizationEnabled by container.appPreferences.profileOrganizationEnabled.collectAsState(initial = true)

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "Developer Settings",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
            item {
                SwitchPreference(
                    checked = profileOrganizationEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { container.appPreferences.setProfileOrganizationEnabled(enabled) }
                    },
                    title = "Profile tags & notes",
                    summary = "Shows archive, color tags, and notes controls on each app's profile list.",
                )
            }
            item {
                ArrowPreference(
                    title = "Custom Hooks",
                    summary = "Advanced JSON method and field hooks",
                    onClick = onCustomHooksClick,
                )
            }
        }
    }
}
