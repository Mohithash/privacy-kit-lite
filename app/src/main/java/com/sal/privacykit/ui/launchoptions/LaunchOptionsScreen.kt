package com.sal.privacykit.ui.launchoptions

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sal.privacykit.PrivacyKitApplication
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
fun LaunchOptionsScreen(profileId: Long, onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as PrivacyKitApplication
    val viewModel: LaunchOptionsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { LaunchOptionsViewModel(profileId, application.container.profileRepository) }
        },
    )
    val profile by viewModel.profile.collectAsState()
    val current = profile ?: return

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "Launch Options",
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
                    title = "Launch instantly",
                    summary = "Skip the confirmation dialog when launching this profile",
                    checked = current.skipLaunchConfirmation,
                    onCheckedChange = { viewModel.setSkipConfirmation(current, it) },
                )
            }
        }
    }
}
