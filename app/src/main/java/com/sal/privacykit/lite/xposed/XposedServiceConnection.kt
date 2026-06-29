package com.sal.privacykit.lite.xposed

import android.util.Log
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class XposedServiceConnection {
    @Volatile
    var service: XposedService? = null
        private set

    var onStateChanged: (() -> Unit)? = null

    init {
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                this@XposedServiceConnection.service = service
                onStateChanged?.invoke()
            }

            override fun onServiceDied(service: XposedService) {
                if (this@XposedServiceConnection.service === service) {
                    this@XposedServiceConnection.service = null
                    Log.w(TAG, "LSPosed service disconnected")
                    onStateChanged?.invoke()
                }
            }
        })
    }

    fun requestScope(packageName: String, onResult: (Boolean, String?) -> Unit) {
        val current = service
        if (current == null) {
            onResult(false, "LSPosed service is not connected")
            return
        }
        val alreadyScoped = runCatching { current.scope.contains(packageName) }.getOrDefault(false)
        if (alreadyScoped) {
            onResult(true, null)
            return
        }
        runCatching {
            current.requestScope(
                listOf(packageName),
                object : XposedService.OnScopeEventListener {
                    override fun onScopeRequestApproved(approved: List<String>) {
                        onResult(packageName in approved, null)
                    }

                    override fun onScopeRequestFailed(message: String) {
                        onResult(false, message)
                    }
                },
            )
        }.onFailure {
            onResult(false, it.message ?: "LSPosed scope request failed")
        }
    }

    companion object {
        private const val TAG = "PrivacyKitLite"
    }
}
