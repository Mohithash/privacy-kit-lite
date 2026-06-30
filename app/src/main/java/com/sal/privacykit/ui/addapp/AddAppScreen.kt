package com.sal.privacykit.ui.addapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sal.privacykit.PrivacyKitApplication
import com.sal.privacykit.ui.components.AppIconBadge
import com.sal.privacykit.ui.components.SearchField
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AddAppScreen(
    onBack: () -> Unit,
    onAppSelected: (String) -> Unit,
) {
    val application = LocalContext.current.applicationContext as PrivacyKitApplication
    val viewModel: AddAppViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AddAppViewModel(application.container.appResolver) }
        },
    )
    val apps by viewModel.apps.collectAsState()
    val query by viewModel.query.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "Add App",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                return@Box
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    SearchField(
                        value = query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = "Search installed apps",
                    )
                }
                items(apps, key = { it.packageName }) { app ->
                    BasicComponent(
                        title = app.name,
                        summary = app.packageName,
                        startAction = { AppIconBadge(app) },
                        onClick = { onAppSelected(app.packageName) },
                    )
                }
                if (apps.isEmpty()) {
                    item {
                        Text(
                            text = "No apps found",
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}
