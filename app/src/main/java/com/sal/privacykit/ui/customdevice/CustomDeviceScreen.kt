package com.sal.privacykit.ui.customdevice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sal.privacykit.PrivacyKitApplication
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.SmallTopAppBar

@Composable
fun CustomDeviceScreen(
    appId: String,
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
) {
    val application = LocalContext.current.applicationContext as PrivacyKitApplication
    val viewModel: CustomDeviceViewModel = viewModel(
        factory = viewModelFactory {
            initializer { CustomDeviceViewModel(appId, application.container.profileRepository) }
        },
    )

    var profileName by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("") }
    var deviceManufacturer by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "Custom Device",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding()).padding(16.dp),
        ) {
            TextField(
                value = profileName,
                onValueChange = { profileName = it },
                label = "Profile name",
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
            TextField(
                value = deviceName,
                onValueChange = { deviceName = it },
                label = "Device name",
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
            TextField(
                value = deviceManufacturer,
                onValueChange = { deviceManufacturer = it },
                label = "Manufacturer",
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            )
            Button(
                onClick = {
                    viewModel.createCustomProfile(
                        profileName = profileName.ifBlank { deviceName },
                        deviceName = deviceName,
                        deviceManufacturer = deviceManufacturer.ifBlank { "Custom" },
                        onCreated = onCreated,
                    )
                },
                enabled = profileName.isNotBlank() && deviceName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Create Profile")
            }
        }
    }
}
