package com.sal.privacykit.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sal.privacykit.PrivacyKitApplication
import com.sal.privacykit.ui.components.SectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val REPO_URL = "https://github.com/Mohithash/privacy-kit-lite"
private const val TELEGRAM_URL = "https://t.me/couponxdealer"

private data class SettingsEntry(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

@Composable
fun SettingsScreen(
    onAppearanceClick: () -> Unit,
    onDeveloperSettingsClick: () -> Unit,
) {
    val application = LocalContext.current.applicationContext as PrivacyKitApplication
    val context = LocalContext.current
    val container = application.container
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAbout by remember { mutableStateOf(false) }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    val exportProfilesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val json = container.profileRepository.exportToJson()
                withContext(Dispatchers.IO) {
                    val out = context.contentResolver.openOutputStream(uri) ?: error("Could not open export destination")
                    out.use { it.write(json.toByteArray()) }
                }
                showMessage("Profiles exported")
            }.onFailure { showMessage("Export failed: ${it.userMessage()}") }
        }
    }

    val importProfilesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val json = withContext(Dispatchers.IO) {
                    val input = context.contentResolver.openInputStream(uri) ?: error("Could not read selected file")
                    input.use { it.readBytes().decodeToString() }
                }
                val count = container.profileRepository.importFromJson(json)
                showMessage("Imported $count profile(s)")
            }.onFailure { showMessage("Import failed: ${it.userMessage()}") }
        }
    }

    val versionName = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "0.3.0"

    OverlayDialog(
        show = showAbout,
        onDismissRequest = { showAbout = false },
        title = "Privacy Kit Lite",
        summary = "Version $versionName\n\n" +
            "Privacy Kit Lite keeps the full Privacy Kit profile workflow for local LSPosed identifier spoofing, with a smaller public hook set and no networked services.\n\n" +
            "1000 Phones. One Device.",
    ) {
        TextButton(
            text = "GitHub",
            onClick = { openUrl(REPO_URL) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
        TextButton(
            text = "Star my repo",
            onClick = { openUrl(REPO_URL) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
        TextButton(
            text = "Telegram (@couponxdealer)",
            onClick = { openUrl(TELEGRAM_URL) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            TextButton(
                text = "Close",
                onClick = { showAbout = false },
                modifier = Modifier.weight(1f),
            )
        }
    }

    Scaffold(
        topBar = { SmallTopAppBar(title = "Settings") },
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding(), bottom = 16.dp),
        ) {
            item { SectionHeader("General") }
            item {
                SettingsRow(SettingsEntry(Icons.Filled.Palette, "Appearance", "Theme", onAppearanceClick))
            }
            item {
                SettingsRow(
                    SettingsEntry(Icons.Filled.FileDownload, "Import Profiles", "Import profiles from file") {
                        importProfilesLauncher.launch(arrayOf("application/json"))
                    },
                )
            }
            item {
                SettingsRow(
                    SettingsEntry(Icons.Filled.FileUpload, "Export Profiles", "Export profiles to file") {
                        exportProfilesLauncher.launch("privacy_kit_lite_profiles.json")
                    },
                )
            }
            item { SectionHeader("Advanced") }
            item {
                SettingsRow(
                    SettingsEntry(Icons.Filled.Code, "Developer Settings", "Custom hooks and organization options", onDeveloperSettingsClick),
                )
            }
            item { SectionHeader("Support") }
            item {
                SettingsRow(SettingsEntry(Icons.Filled.Star, "Star my repo", REPO_URL) { openUrl(REPO_URL) })
            }
            item { SectionHeader("About") }
            item {
                SettingsRow(
                    SettingsEntry(Icons.Filled.Info, "About Privacy Kit Lite", "Version, support, and links") {
                        showAbout = true
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(entry: SettingsEntry) {
    ArrowPreference(
        title = entry.title,
        summary = entry.subtitle,
        startAction = {
            Icon(entry.icon, contentDescription = null, tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
        },
        onClick = entry.onClick,
    )
}

private fun Throwable.userMessage(): String =
    localizedMessage?.takeIf { it.isNotBlank() } ?: "Unexpected error"
