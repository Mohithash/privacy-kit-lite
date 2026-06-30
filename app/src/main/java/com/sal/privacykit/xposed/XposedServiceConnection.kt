package com.sal.privacykit.xposed

import android.util.Log
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

private const val TAG = "PrivacyKitXposed"

/**
 * Holds the live binder connection to the LSPosed framework. The connection only becomes
 * available once the module is enabled for this app and the framework has bound to our
 * auto-merged XposedProvider, so callers must tolerate [service] being null.
 */
class XposedServiceConnection {
    @Volatile
    var service: XposedService? = null
        private set

    /** Set by callers that need to re-sync state once the framework binder arrives. */
    var onBind: (() -> Unit)? = null

    init {
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                this@XposedServiceConnection.service = service
                onBind?.invoke()
            }

            override fun onServiceDied(service: XposedService) {
                if (this@XposedServiceConnection.service === service) {
                    this@XposedServiceConnection.service = null
                    Log.w(TAG, "LSPosed service disconnected")
                }
            }
        })
    }

    /**
     * Asks LSPosed to add [packageName] to this module's hook scope at runtime, for apps the user
     * picks beyond the static scope.list baked into the APK. Requires the user to approve the
     * request in LSPosed Manager; some frameworks also require restarting the target app/process
     * before the new scope takes effect. Silently no-ops if the framework isn't connected yet.
     */
    fun requestScope(packageName: String, onResult: (approved: Boolean, message: String?) -> Unit = { _, _ -> }) {
        val current = service
        if (current == null) {
            Log.w(TAG, "Cannot request scope for $packageName: LSPosed service is not connected")
            onResult(false, "LSPosed service is not connected")
            return
        }
        // LSPosed re-prompts the user on every requestScope() call regardless of prior approval,
        // so check the current scope first to avoid re-asking for an app that's already granted.
        val alreadyInScope = runCatching { current.scope }.getOrNull()?.contains(packageName) == true
        if (alreadyInScope) {
            onResult(true, null)
            return
        }
        current.requestScope(
            listOf(packageName),
            object : XposedService.OnScopeEventListener {
                override fun onScopeRequestApproved(approved: List<String>) {
                    onResult(packageName in approved, null)
                }

                override fun onScopeRequestFailed(message: String) {
                    Log.w(TAG, "LSPosed scope request failed for $packageName: $message")
                    onResult(false, message)
                }
            },
        )
    }
}
