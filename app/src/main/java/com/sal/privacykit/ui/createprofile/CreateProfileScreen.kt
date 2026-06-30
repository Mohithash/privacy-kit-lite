package com.sal.privacykit.ui.createprofile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sal.privacykit.PrivacyKitApplication
import com.sal.privacykit.data.model.DeviceTemplate
import com.sal.privacykit.data.model.DeviceTemplateCatalog
import com.sal.privacykit.ui.components.SectionHeader
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CreateProfileScreen(
    appId: String,
    onBack: () -> Unit,
    onProfileCreated: (Long) -> Unit,
    onCloneExisting: (String) -> Unit,
    onCustomDevice: (String) -> Unit,
) {
    val application = LocalContext.current.applicationContext as PrivacyKitApplication
    val viewModel: CreateProfileViewModel = viewModel(
        factory = viewModelFactory {
            initializer { CreateProfileViewModel(appId, application.container.profileRepository) }
        },
    )
    var showAllTemplates by remember { mutableStateOf(false) }
    val popularTemplates = listOf("pixel_9_pro", "galaxy_s25_ultra", "nothing_phone_4")
        .map(DeviceTemplateCatalog::byId)
    val templatesToShow = if (showAllTemplates) DeviceTemplateCatalog.templates else popularTemplates

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "Create Profile",
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
                OptionRow(
                    icon = Icons.AutoMirrored.Filled.ViewList,
                    title = "From Template",
                    subtitle = "Start with a real device template",
                    onClick = { showAllTemplates = !showAllTemplates },
                )
            }
            item {
                OptionRow(
                    icon = Icons.Filled.ContentCopy,
                    title = "Clone Existing Profile",
                    subtitle = "Create a new profile from an existing one",
                    onClick = { onCloneExisting(appId) },
                )
            }
            item {
                OptionRow(
                    icon = Icons.Filled.Tune,
                    title = "Custom Device",
                    subtitle = "Define a custom model and manufacturer",
                    onClick = { onCustomDevice(appId) },
                )
            }
            item { SectionHeader(if (showAllTemplates) "All Templates" else "Popular Templates") }
            items(templatesToShow, key = { it.id }) { template ->
                TemplateOptionRow(
                    template = template,
                    onClick = { viewModel.createFromTemplate(template.id, template.name, onProfileCreated) },
                )
            }
        }
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    ArrowPreference(
        title = title,
        summary = subtitle,
        startAction = { Icon(icon, contentDescription = null, tint = MiuixTheme.colorScheme.onSurfaceVariantActions) },
        onClick = onClick,
    )
}

@Composable
private fun TemplateOptionRow(template: DeviceTemplate, onClick: () -> Unit) {
    ArrowPreference(
        title = template.name,
        startAction = { Icon(Icons.Filled.Smartphone, contentDescription = null, tint = MiuixTheme.colorScheme.onSurfaceVariantActions) },
        onClick = onClick,
    )
}
