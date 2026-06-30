package com.sal.privacykit.ui.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sal.privacykit.PrivacyKitApplication
import com.sal.privacykit.data.model.DeviceTemplateCatalog
import com.sal.privacykit.data.model.Profile
import com.sal.privacykit.ui.components.AppIconBadge
import com.sal.privacykit.ui.components.tagDotColor
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
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
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppProfilesScreen(
    appId: String,
    onBack: () -> Unit,
    onProfileClick: (Long) -> Unit,
    onCreateProfile: (String) -> Unit,
) {
    val application = LocalContext.current.applicationContext as PrivacyKitApplication
    val viewModel: AppProfilesViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                AppProfilesViewModel(
                    appId,
                    application.container.profileRepository,
                    application.container.profileLauncher,
                )
            }
        },
    )
    val context = LocalContext.current
    val profiles by viewModel.profiles.collectAsState()
    val showArchived by viewModel.showArchived.collectAsState()
    val organizationEnabled by application.container.appPreferences.profileOrganizationEnabled.collectAsState(initial = true)
    val app = application.container.appResolver.resolve(appId)
    var pendingLaunch by remember { mutableStateOf<Profile?>(null) }
    var editingNotesFor by remember { mutableStateOf<Profile?>(null) }
    var notesInput by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun launchWithErrorHandling(profile: Profile) {
        viewModel.launch(profile, context, onError = { message -> scope.launch { snackbarHostState.showSnackbar(message) } })
    }

    OverlayDialog(
        show = pendingLaunch != null,
        onDismissRequest = { pendingLaunch = null },
        title = "Launch with this profile?",
        summary = pendingLaunch?.let { "${app.name} will open using the \"${it.name}\" profile." },
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(text = "Cancel", onClick = { pendingLaunch = null }, modifier = Modifier.weight(1f))
            androidx.compose.foundation.layout.Spacer(Modifier.padding(horizontal = 8.dp))
            TextButton(
                text = "Launch",
                onClick = {
                    pendingLaunch?.let(::launchWithErrorHandling)
                    pendingLaunch = null
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }

    OverlayDialog(
        show = editingNotesFor != null,
        onDismissRequest = { editingNotesFor = null },
        title = "Edit notes",
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TextField(value = notesInput, onValueChange = { notesInput = it })
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                TextButton(text = "Cancel", onClick = { editingNotesFor = null }, modifier = Modifier.weight(1f))
                androidx.compose.foundation.layout.Spacer(Modifier.padding(horizontal = 8.dp))
                TextButton(
                    text = "Save",
                    onClick = {
                        editingNotesFor?.let { viewModel.setNotes(it, notesInput) }
                        editingNotesFor = null
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
                    if (organizationEnabled) {
                        IconButton(onClick = { viewModel.toggleShowArchived() }) {
                            Icon(
                                if (showArchived) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showArchived) "Hide archived" else "Show archived",
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
        bottomBar = {
            Surface {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Button(onClick = { onCreateProfile(appId) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  Create Profile")
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppIconBadge(app, size = 64)
                Text(text = app.name, style = MiuixTheme.textStyles.title3, modifier = Modifier.padding(top = 12.dp))
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 16.dp),
            ) {
                items(profiles, key = { it.id }) { profile ->
                    ProfileRow(
                        profile = profile,
                        organizationEnabled = organizationEnabled,
                        onClick = { onProfileClick(profile.id) },
                        onLaunch = {
                            if (profile.skipLaunchConfirmation) launchWithErrorHandling(profile) else pendingLaunch = profile
                        },
                        onDelete = { viewModel.deleteProfile(profile) },
                        onDuplicate = {
                            viewModel.duplicateProfile(profile) { id ->
                                scope.launch { snackbarHostState.showSnackbar("Profile duplicated") }
                                onProfileClick(id)
                            }
                        },
                        onToggleArchived = { viewModel.toggleArchived(profile) },
                        onCycleTagColor = { viewModel.cycleTagColor(profile) },
                        onEditNotes = {
                            notesInput = profile.notes.orEmpty()
                            editingNotesFor = profile
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(
    profile: Profile,
    organizationEnabled: Boolean,
    onClick: () -> Unit,
    onLaunch: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onToggleArchived: () -> Unit,
    onCycleTagColor: () -> Unit,
    onEditNotes: () -> Unit,
) {
    val template = DeviceTemplateCatalog.byId(profile.deviceTemplateId)
    val deviceLabel = profile.deviceLabel(template.name, template.manufacturer)

    BasicComponent(
        title = profile.name,
        summary = "$deviceLabel - ${profile.profileMode.label}",
        startAction = {
            Box(
                modifier = Modifier.size(40.dp).background(MiuixTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = profile.name.firstOrNull()?.uppercase() ?: "?",
                    color = MiuixTheme.colorScheme.onSecondaryContainer,
                    style = MiuixTheme.textStyles.subtitle,
                )
            }
        },
        endActions = {
            if (organizationEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 8.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(profile.tagColor?.let(::tagDotColor) ?: MiuixTheme.colorScheme.outline, CircleShape)
                        .clickable(onClick = onCycleTagColor),
                )
            }
            if (profile.archived) {
                TextButton(text = "Unarchive", onClick = onToggleArchived)
            } else {
                Button(onClick = onLaunch, minHeight = 36.dp) {
                    Text("Launch")
                }
            }
            OverlayIconDropdownMenu(
                entry = DropdownEntry(
                    items = buildList {
                        if (organizationEnabled) {
                            add(
                                DropdownItem(
                                    text = if (profile.archived) "Unarchive" else "Archive",
                                    icon = { modifier ->
                                        Icon(
                                            if (profile.archived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                                            contentDescription = null,
                                            modifier = modifier,
                                        )
                                    },
                                    onClick = onToggleArchived,
                                ),
                            )
                            add(
                                DropdownItem(
                                    text = "Edit notes",
                                    icon = { modifier -> Icon(Icons.Filled.EditNote, contentDescription = null, modifier = modifier) },
                                    onClick = onEditNotes,
                                ),
                            )
                        }
                        add(
                            DropdownItem(
                                text = "Duplicate",
                                icon = { modifier -> Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = modifier) },
                                onClick = onDuplicate,
                            ),
                        )
                        add(
                            DropdownItem(
                                text = "Delete",
                                icon = { modifier -> Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = modifier) },
                                onClick = onDelete,
                            ),
                        )
                    },
                ),
            ) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
            }
        },
        bottomAction = if (organizationEnabled && !profile.notes.isNullOrBlank()) {
            {
                Text(
                    text = profile.notes!!,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 56.dp, bottom = 8.dp),
                )
            }
        } else {
            null
        },
        onClick = onClick,
    )
    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
}
