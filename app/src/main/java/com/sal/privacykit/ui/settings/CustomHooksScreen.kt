package com.sal.privacykit.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sal.privacykit.PrivacyKitApplication
import com.sal.privacykit.data.model.CustomHookValidator
import com.sal.privacykit.ui.components.SectionHeader
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val EXAMPLE_HOOKS = """[
  {
    "enabled": false,
    "packageName": "com.example.app",
    "className": "android.os.Build",
    "member": "MODEL",
    "type": "field",
    "value": "Pixel 9 Pro"
  },
  {
    "enabled": false,
    "packageName": "com.example.app",
    "className": "android.telephony.TelephonyManager",
    "member": "getDeviceId",
    "type": "method",
    "argTypes": "",
    "value": "000000000000000"
  }
]"""

@Composable
fun CustomHooksScreen(onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as PrivacyKitApplication
    val container = application.container
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedJson by container.appPreferences.customHooksJson.collectAsState(initial = "[]")
    var jsonInput by remember { mutableStateOf(savedJson) }

    LaunchedEffect(savedJson) {
        jsonInput = savedJson
    }

    val validation = remember(jsonInput) { CustomHookValidator.validate(jsonInput) }
    val hasUnsavedChanges = jsonInput != savedJson

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun save() {
        val result = CustomHookValidator.validate(jsonInput)
        if (!result.isValid) {
            showMessage(result.message ?: "Custom hooks JSON is invalid")
            return
        }
        scope.launch {
            container.appPreferences.setCustomHooksJson(jsonInput)
            container.profileRepository.resync()
            snackbarHostState.showSnackbar("Custom hooks saved")
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "Custom Hooks",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding(), bottom = 16.dp),
        ) {
            item { SectionHeader("Current state") }
            item {
                BasicComponent(
                    title = if (hasUnsavedChanges) "Unsaved" else "Saved",
                    summary = if (hasUnsavedChanges) "Working copy differs from saved JSON" else "Working copy matches saved JSON",
                    startAction = {
                        Icon(
                            if (hasUnsavedChanges) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            }
            item {
                BasicComponent(
                    title = if (validation.isValid) "Valid" else "Invalid",
                    summary = validation.message ?: "No validation issues",
                    startAction = {
                        Icon(
                            if (validation.isValid) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            }
            item { SectionHeader("Raw JSON") }
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    TextField(
                        value = jsonInput,
                        onValueChange = { jsonInput = it },
                        label = "Hook rules",
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 14,
                    )
                    TextButton(
                        text = "Save",
                        onClick = ::save,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        TextButton(
                            text = "Revert",
                            onClick = { jsonInput = savedJson },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            text = "Load example",
                            onClick = { jsonInput = EXAMPLE_HOOKS },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item { SectionHeader("Schema") }
            item {
                BasicComponent(
                    title = "Supported rule fields",
                    summary = "enabled, packageName, className, member, type, argTypes, value, rule, randomValues",
                    startAction = {
                        Icon(Icons.Filled.Code, contentDescription = null, tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
                    },
                )
            }
        }
    }
}
