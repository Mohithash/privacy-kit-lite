package com.sal.privacykit.ui.apps

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.runtime.collectAsState
import com.sal.privacykit.PrivacyKitApplication
import com.sal.privacykit.ui.components.AppIconBadge
import com.sal.privacykit.ui.components.SearchField
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppsScreen(onAppClick: (String) -> Unit, onAddApp: () -> Unit) {
    val application = LocalContext.current.applicationContext as PrivacyKitApplication
    val viewModel: AppsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AppsViewModel(application.container.appRepository) }
        },
    )
    val apps by viewModel.apps.collectAsState()
    val query by viewModel.query.collectAsState()

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "Apps",
                actions = {
                    IconButton(onClick = onAddApp) {
                        Icon(Icons.Filled.Add, contentDescription = "Add app")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding(), bottom = 16.dp),
        ) {
            item {
                SearchField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = "Search apps",
                )
            }
            items(apps, key = { it.app.id }) { entry ->
                val label = if (entry.profileCount == 1) "1 Profile" else "${entry.profileCount} Profiles"
                BasicComponent(
                    title = entry.app.name,
                    summary = label,
                    startAction = { AppIconBadge(entry.app) },
                    endActions = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    },
                    onClick = { onAppClick(entry.app.id) },
                )
            }
        }
    }
}
