package com.sal.privacykit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.ui.Modifier
import com.sal.privacykit.data.LocalAppContainer
import com.sal.privacykit.data.prefs.ThemeMode
import com.sal.privacykit.ui.navigation.PrivacyKitNavHost
import com.sal.privacykit.ui.theme.PrivacyKitTheme
import top.yukonga.miuix.kmp.basic.Surface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as PrivacyKitApplication).container

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                val themeMode by container.appPreferences.themeMode.collectAsState(initial = ThemeMode.DARK)
                val systemDark = isSystemInDarkTheme()
                val darkTheme = when (themeMode) {
                    ThemeMode.SYSTEM -> systemDark
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
                PrivacyKitTheme(darkTheme = darkTheme) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        PrivacyKitNavHost()
                    }
                }
            }
        }
    }
}
