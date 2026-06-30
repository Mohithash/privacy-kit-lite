package com.sal.privacykit.ui.identifiers

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sal.privacykit.PrivacyKitApplication
import com.sal.privacykit.data.model.IdentifierCatalog
import com.sal.privacykit.data.model.IdentifierItem
import com.sal.privacykit.data.model.IdentifierRuleType
import com.sal.privacykit.ui.components.SectionHeader
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun IdentifiersScreen(
    profileId: Long,
    onBack: () -> Unit,
    onIdentifierClick: (Long, String) -> Unit,
) {
    val application = LocalContext.current.applicationContext as PrivacyKitApplication
    val viewModel: IdentifiersViewModel = viewModel(
        factory = viewModelFactory {
            initializer { IdentifiersViewModel(profileId, application.container.profileRepository) }
        },
    )
    val profile by viewModel.profile.collectAsState()
    val current = profile ?: return

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "Identifiers",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
            IdentifierCatalog.groups.forEach { group ->
                item { SectionHeader(group.title) }
                items(group.items, key = { it.key }) { item ->
                    val rule = current.identifierRules[item.key] ?: item.defaultRule
                    IdentifierRow(item = item, rule = rule, onClick = { onIdentifierClick(profileId, item.key) })
                }
            }
        }
    }
}

@Composable
private fun IdentifierRow(item: IdentifierItem, rule: IdentifierRuleType, onClick: () -> Unit) {
    BasicComponent(
        title = item.label,
        summary = rule.label,
        endActions = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        },
        onClick = onClick,
    )
}
