package com.sal.privacykit.ui.profilestab

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
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
import com.sal.privacykit.ui.components.DeviceIconBadge
import com.sal.privacykit.ui.components.SearchField
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ProfilesTabScreen(
    onGroupClick: (Long) -> Unit,
    onCreateProfile: () -> Unit,
) {
    val application = LocalContext.current.applicationContext as PrivacyKitApplication
    val viewModel: ProfilesTabViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ProfilesTabViewModel(application.container.profileRepository) }
        },
    )
    val groups by viewModel.groups.collectAsState()
    val query by viewModel.query.collectAsState()

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "Profiles",
                actions = {
                    IconButton(onClick = onCreateProfile) {
                        Icon(Icons.Filled.Add, contentDescription = "Create profile")
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
                    placeholder = "Search profiles",
                )
            }
            items(groups, key = { it.groupKey }) { group ->
                val label = if (group.profileCount == 1) "1 App" else "${group.profileCount} Apps"
                BasicComponent(
                    title = group.templateName,
                    summary = label,
                    startAction = { DeviceIconBadge() },
                    endActions = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    },
                    onClick = { onGroupClick(group.representativeProfileId) },
                )
            }
        }
    }
}
