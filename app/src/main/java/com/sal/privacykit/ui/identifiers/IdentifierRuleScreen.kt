package com.sal.privacykit.ui.identifiers

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sal.privacykit.PrivacyKitApplication
import com.sal.privacykit.data.model.IdentifierCatalog
import com.sal.privacykit.data.model.IdentifierRuleType
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun IdentifierRuleScreen(
    profileId: Long,
    identifierKey: String,
    onBack: () -> Unit,
) {
    val application = LocalContext.current.applicationContext as PrivacyKitApplication
    val viewModel: IdentifierRuleViewModel = viewModel(
        factory = viewModelFactory {
            initializer { IdentifierRuleViewModel(profileId, application.container.profileRepository) }
        },
    )
    val profile by viewModel.profile.collectAsState()
    val current = profile ?: return
    val item = IdentifierCatalog.itemByKey(identifierKey)
    val selectedRule = current.identifierRules[identifierKey] ?: item.defaultRule
    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = item.label,
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
                Text(
                    text = "Choose how this identifier behaves",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            items(IdentifierRuleType.entries) { rule ->
                RadioButtonPreference(
                    title = rule.label,
                    selected = rule == selectedRule,
                    onClick = { viewModel.selectRule(current, identifierKey, rule) },
                )
            }
            if (selectedRule == IdentifierRuleType.CUSTOM) {
                item {
                    var value by remember(current.id, identifierKey) {
                        mutableStateOf(current.identifierValues[identifierKey].orEmpty())
                    }
                    Surface(
                        color = MiuixTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextField(
                                value = value,
                                onValueChange = {
                                    value = it
                                    viewModel.setCustomValue(current, identifierKey, it)
                                },
                                singleLine = true,
                                textStyle = MiuixTheme.textStyles.main.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { clipboard.setText(AnnotatedString(value)) }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy value")
                            }
                        }
                    }
                }
            }
        }
    }
}
