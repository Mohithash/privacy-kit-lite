package com.sal.privacykit

import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.os.Process
import com.sal.privacykit.data.AppContainer

class PrivacyKitApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        // Keep initialization scoped to the main app process. Some Android components can run in
        // secondary processes, while WorkManager/startup initializers are main-process scoped.
        if (isMainProcess()) {
            container = AppContainer(this)
        }
    }

    private fun isMainProcess(): Boolean {
        val processName = if (Build.VERSION.SDK_INT >= 28) {
            getProcessName()
        } else {
            val activityManager = getSystemService(ActivityManager::class.java)
            activityManager?.runningAppProcesses?.firstOrNull { it.pid == Process.myPid() }?.processName
        }
        return processName == null || processName == packageName
    }
}
