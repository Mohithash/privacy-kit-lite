package com.sal.privacykit.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Shield
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sal.privacykit.PrivacyKitApplication
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val LSPOSED_MANAGER_PACKAGE = "org.lsposed.manager"

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    val application = LocalContext.current.applicationContext as PrivacyKitApplication
    val container = application.container
    var lsposedActive by remember { mutableStateOf(container.xposedServiceConnection.service != null) }

    LaunchedEffect(Unit) {
        while (!lsposedActive) {
            delay(500)
            lsposedActive = container.xposedServiceConnection.service != null
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                modifier = Modifier.size(40.dp).padding(bottom = 16.dp),
                tint = MiuixTheme.colorScheme.primary,
            )
            Text(
                text = "Before you start",
                fontSize = MiuixTheme.textStyles.title1.fontSize,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Privacy Kit Lite spoofs per-app identifiers through LSPosed. Enable the module once, then choose apps and assign profiles.",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )

            BasicComponent(
                title = "LSPosed module",
                summary = if (lsposedActive) "Enabled for Privacy Kit Lite" else "Not enabled yet -- open LSPosed Manager and enable this module, then restart it",
                startAction = {
                    Icon(
                        imageVector = if (lsposedActive) Icons.Filled.CheckCircle else Icons.Filled.Error,
                        contentDescription = null,
                        tint = if (lsposedActive) Color(0xFF4CAF50) else Color(0xFFE94634),
                    )
                },
                endActions = {
                    if (!lsposedActive) {
                        TextButton(
                            text = "Open",
                            onClick = {
                                val intent = application.packageManager.getLaunchIntentForPackage(LSPOSED_MANAGER_PACKAGE)
                                if (intent != null) {
                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    application.startActivity(intent)
                                }
                            },
                        )
                    }
                },
            )

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(if (lsposedActive) "Continue" else "Continue anyway")
            }
        }
    }
}
