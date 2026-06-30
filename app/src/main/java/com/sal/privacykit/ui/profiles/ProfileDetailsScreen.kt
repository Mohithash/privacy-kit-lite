package com.sal.privacykit.ui.profiles

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sal.privacykit.PrivacyKitApplication
import com.sal.privacykit.data.model.DeviceTemplateCatalog
import com.sal.privacykit.data.model.Profile
import com.sal.privacykit.ui.shortcut.ShortcutLaunchActivity
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ProfileDetailsScreen(
    profileId: Long,
    onBack: () -> Unit,
    onDeviceTemplateClick: (Long) -> Unit,
    onIdentifiersClick: (Long) -> Unit,
    onAppsUsingProfileClick: (String) -> Unit,
    onLaunchOptionsClick: (Long) -> Unit,
) {
    val application = LocalContext.current.applicationContext as PrivacyKitApplication
    val viewModel: ProfileDetailsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ProfileDetailsViewModel(
                    profileId,
                    application.container.profileRepository,
                    application.container.profileLauncher,
                )
            }
        },
    )
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()
    val current = profile ?: return
    val template = DeviceTemplateCatalog.byId(current.deviceTemplateId)
    val app = application.container.appResolver.resolve(current.appId)
    var showRenameDialog by remember { mutableStateOf(false) }
    var showLaunchConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun launchWithErrorHandling(p: Profile) {
        viewModel.launch(p, context, onError = { message -> scope.launch { snackbarHostState.showSnackbar(message) } })
    }

    fun pinShortcut(p: Profile) {
        val icon = (app.icon?.toBitmap())?.let { IconCompat.createWithBitmap(it) }
            ?: IconCompat.createWithResource(context, com.sal.privacykit.R.mipmap.ic_launcher)
        val shortcut = ShortcutInfoCompat.Builder(context, "profile_${p.id}")
            .setShortLabel(p.name)
            .setLongLabel("${app.name} (${p.name})")
            .setIcon(icon)
            .setIntent(ShortcutLaunchActivity.intentFor(context, p.id))
            .build()
        val pinned = ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        if (!pinned) {
            scope.launch { snackbarHostState.showSnackbar("This launcher doesn't support pinning shortcuts") }
        }
    }

    OverlayDialog(
        show = showLaunchConfirm,
        onDismissRequest = { showLaunchConfirm = false },
        title = "Launch with this profile?",
        summary = "${app.name} will open using the \"${current.name}\" profile.",
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(text = "Cancel", onClick = { showLaunchConfirm = false }, modifier = Modifier.weight(1f))
            androidx.compose.foundation.layout.Spacer(Modifier.padding(horizontal = 8.dp))
            TextButton(
                text = "Launch",
                onClick = {
                    launchWithErrorHandling(current)
                    showLaunchConfirm = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }

    var newName by remember(current.id, showRenameDialog) { mutableStateOf(current.name) }
    OverlayDialog(
        show = showRenameDialog,
        onDismissRequest = { showRenameDialog = false },
        title = "Rename profile",
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TextField(value = newName, onValueChange = { newName = it }, singleLine = true)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                TextButton(text = "Cancel", onClick = { showRenameDialog = false }, modifier = Modifier.weight(1f))
                androidx.compose.foundation.layout.Spacer(Modifier.padding(horizontal = 8.dp))
                TextButton(
                    text = "Save",
                    onClick = {
                        if (newName.isNotBlank()) viewModel.rename(current, newName.trim())
                        showRenameDialog = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { pinShortcut(current) }) {
                        Icon(Icons.AutoMirrored.Filled.AddToHomeScreen, contentDescription = "Add to home screen")
                    }
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Rename")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
        bottomBar = {
            Surface {
                Button(
                    onClick = {
                        if (current.skipLaunchConfirmation) launchWithErrorHandling(current) else showLaunchConfirm = true
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Icon(Icons.Filled.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Launch with this profile")
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding(),
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(shape = CircleShape, color = MiuixTheme.colorScheme.primaryContainer, modifier = Modifier.size(64.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = current.name.firstOrNull()?.uppercase() ?: "?",
                            style = MiuixTheme.textStyles.title2,
                            color = MiuixTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Text(text = current.name, style = MiuixTheme.textStyles.title3, modifier = Modifier.padding(top = 12.dp))
            }
            LazyColumn {
                item {
                    SettingsRow(
                        icon = Icons.Filled.Smartphone,
                        title = "Device Template",
                        value = current.deviceLabel(template.name, template.manufacturer),
                        onClick = { onDeviceTemplateClick(profileId) },
                    )
                }
                item {
                    SettingsRow(
                        icon = Icons.Filled.Fingerprint,
                        title = "Identifiers",
                        value = "${current.configuredIdentifierCount} Configured",
                        onClick = { onIdentifiersClick(profileId) },
                    )
                }
                item {
                    SettingsRow(
                        icon = Icons.Filled.PhoneAndroid,
                        title = "Apps Using This Profile",
                        value = app.name,
                        onClick = { onAppsUsingProfileClick(current.appId) },
                    )
                }
                item {
                    SettingsRow(
                        icon = Icons.Filled.RocketLaunch,
                        title = "Launch Options",
                        value = if (current.skipLaunchConfirmation) "Launches instantly" else "Confirms before launching",
                        onClick = { onLaunchOptionsClick(profileId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    ArrowPreference(
        title = title,
        summary = value,
        startAction = { Icon(icon, contentDescription = null, tint = MiuixTheme.colorScheme.onSurfaceVariantActions) },
        onClick = onClick,
    )
}
