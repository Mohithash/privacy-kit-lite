package com.sal.privacykit.ui.profilemode

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sal.privacykit.PrivacyKitApplication
import com.sal.privacykit.data.model.ProfileMode
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ProfileModeScreen(profileId: Long, onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as PrivacyKitApplication
    val viewModel: ProfileModeViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ProfileModeViewModel(profileId, application.container.profileRepository) }
        },
    )
    val profile by viewModel.profile.collectAsState()
    val current = profile ?: return

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "Profile Mode",
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
                Text(
                    text = "Choose how this profile isolates app data",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            items(ProfileMode.entries.toList()) { mode ->
                RadioButtonPreference(
                    title = mode.label,
                    summary = mode.description(),
                    selected = mode == current.profileMode,
                    onClick = { viewModel.selectMode(current, mode) },
                )
            }
        }
    }
}

private fun ProfileMode.description(): String = when (this) {
    ProfileMode.ISOLATED -> "Keeps this profile's data fully separate"
    ProfileMode.HYBRID -> "Balances isolation with shared system resources"
    ProfileMode.SHARED -> "Shares data and identifiers with the main profile"
}
