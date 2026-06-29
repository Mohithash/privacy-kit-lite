package com.sal.privacykit.lite.xposed

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.media.MediaDrm
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import com.sal.privacykit.lite.model.IdentifierRuleType
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import org.json.JSONObject
import java.lang.reflect.Field
import java.lang.reflect.Modifier

private const val HOST_PACKAGE = "com.sal.privacykit.lite"
private const val TAG = "PrivacyKitLiteHook"

class PrivacyKitLiteHook : XposedModule() {
    override fun onPackageReady(param: PackageReadyParam) {
        val packageName = param.packageName
        if (packageName == HOST_PACKAGE) return
        val root = readRootConfig() ?: return
        val config = root.optJSONObject(packageName) ?: return
        val identifiers = config.optJSONObject("identifiers") ?: return
        val profileId = config.optLong("profileId", packageName.hashCode().toLong())

        log("applying profile $profileId to $packageName")
        hookAdvertisingIdBinder(identifiers, profileId)
        hookAndroidId(identifiers, profileId)
        hookHostname(identifiers, profileId)
        hookBuildValues(identifiers, profileId)
        hookNetworkValues(identifiers, profileId, param.classLoader)
        hookMediaDrm(identifiers, profileId)
        hookTelephony(identifiers, profileId)
    }

    private fun hookAndroidId(identifiers: JSONObject, profileId: Long) {
        hookMethod(identifiers, "android_id", profileId, "Settings.Secure.getString") { spoofed ->
            Settings.Secure::class.java.getMethod("getString", ContentResolver::class.java, String::class.java) to
                XposedInterface.Hooker { chain ->
                    if (chain.getArg(1) == Settings.Secure.ANDROID_ID) spoofed else chain.proceed()
                }
        }
    }

    private fun hookHostname(identifiers: JSONObject, profileId: Long) {
        hookMethod(identifiers, "hostname", profileId, "Settings.Global.getString") { spoofed ->
            Settings.Global::class.java.getMethod("getString", ContentResolver::class.java, String::class.java) to
                XposedInterface.Hooker { chain ->
                    if (chain.getArg(1) in setOf("device_name", "hostname")) spoofed else chain.proceed()
                }
        }
    }

    private fun hookBuildValues(identifiers: JSONObject, profileId: Long) {
        mapOf(
            "build_model" to "MODEL",
            "build_brand" to "BRAND",
            "build_manufacturer" to "MANUFACTURER",
            "build_fingerprint" to "FINGERPRINT",
            "build_serial" to "SERIAL",
            "build_board" to "BOARD",
            "build_device" to "DEVICE",
            "build_product" to "PRODUCT",
            "build_hardware" to "HARDWARE",
            "build_id" to "ID",
            "build_type" to "TYPE",
            "build_tags" to "TAGS",
            "build_display" to "DISPLAY",
            "build_bootloader" to "BOOTLOADER",
        ).forEach { (key, fieldName) ->
            resolveValue(identifiers, key, profileId)?.let { value ->
                runCatching { setStaticFinalField(Build::class.java, fieldName, value) }
                    .onFailure { log("failed to set Build.$fieldName", it) }
            }
        }
        hookMethod(identifiers, "build_serial", profileId, "Build.getSerial") { spoofed ->
            Build::class.java.getMethod("getSerial") to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "build_radio_version", profileId, "Build.getRadioVersion") { spoofed ->
            Build::class.java.getMethod("getRadioVersion") to XposedInterface.Hooker { spoofed }
        }
    }

    private fun hookNetworkValues(identifiers: JSONObject, profileId: Long, classLoader: ClassLoader) {
        hookMethod(identifiers, "wifi_mac", profileId, "WifiInfo.getMacAddress") { spoofed ->
            Class.forName("android.net.wifi.WifiInfo", false, classLoader).getMethod("getMacAddress") to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "wifi_bssid", profileId, "WifiInfo.getBSSID") { spoofed ->
            Class.forName("android.net.wifi.WifiInfo", false, classLoader).getMethod("getBSSID") to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "wifi_ssid", profileId, "WifiInfo.getSSID") { spoofed ->
            Class.forName("android.net.wifi.WifiInfo", false, classLoader).getMethod("getSSID") to XposedInterface.Hooker { "\"$spoofed\"" }
        }
        hookMethod(identifiers, "bluetooth_mac", profileId, "BluetoothAdapter.getAddress") { spoofed ->
            Class.forName("android.bluetooth.BluetoothAdapter", false, classLoader).getMethod("getAddress") to XposedInterface.Hooker { spoofed }
        }
    }

    private fun hookMediaDrm(identifiers: JSONObject, profileId: Long) {
        hookMethod(identifiers, "media_drm_id", profileId, "MediaDrm.getPropertyByteArray") { spoofed ->
            val bytes = hexToBytes(spoofed)
            MediaDrm::class.java.getMethod("getPropertyByteArray", String::class.java) to
                XposedInterface.Hooker { chain ->
                    if (chain.getArg(0) == "deviceUniqueId") bytes else chain.proceed()
                }
        }
    }

    private fun hookTelephony(identifiers: JSONObject, profileId: Long) {
        val manager = TelephonyManager::class.java
        hookMethod(identifiers, "imei", profileId, "TelephonyManager.getImei") { spoofed ->
            manager.getMethod("getImei") to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "imei", profileId, "TelephonyManager.getDeviceId") { spoofed ->
            manager.getMethod("getDeviceId") to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "imei", profileId, "TelephonyManager.getImei(slot)") { spoofed ->
            manager.getMethod("getImei", Int::class.javaPrimitiveType) to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "imei", profileId, "TelephonyManager.getDeviceId(slot)") { spoofed ->
            manager.getMethod("getDeviceId", Int::class.javaPrimitiveType) to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "meid", profileId, "TelephonyManager.getMeid") { spoofed ->
            manager.getMethod("getMeid") to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "meid", profileId, "TelephonyManager.getMeid(slot)") { spoofed ->
            manager.getMethod("getMeid", Int::class.javaPrimitiveType) to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "subscriber_id", profileId, "TelephonyManager.getSubscriberId") { spoofed ->
            manager.getMethod("getSubscriberId") to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "iccid", profileId, "TelephonyManager.getSimSerialNumber") { spoofed ->
            manager.getMethod("getSimSerialNumber") to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "phone_number", profileId, "TelephonyManager.getLine1Number") { spoofed ->
            manager.getMethod("getLine1Number") to XposedInterface.Hooker { spoofed }
        }
    }

    @SuppressLint("PrivateApi")
    private fun hookAdvertisingIdBinder(identifiers: JSONObject, profileId: Long) {
        val spoofed = resolveValue(identifiers, "advertising_id", profileId) ?: return
        runCatching {
            val binderProxy = Class.forName("android.os.BinderProxy")
            val transact = binderProxy.getDeclaredMethod(
                "transact",
                Int::class.javaPrimitiveType,
                android.os.Parcel::class.java,
                android.os.Parcel::class.java,
                Int::class.javaPrimitiveType,
            ).apply { isAccessible = true }
            hook(transact)
                .setId("privacykitlite.advertising_id.binder")
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(
                    XposedInterface.Hooker { chain ->
                        val code = chain.getArg(0) as? Int ?: return@Hooker chain.proceed()
                        val reply = chain.getArg(2) as? android.os.Parcel
                        val flags = chain.getArg(3) as? Int ?: return@Hooker chain.proceed()
                        val descriptor = runCatching { (chain.thisObject as? IBinder)?.interfaceDescriptor }.getOrNull()
                        when (AdvertisingIdBinderProtocol.classify(code, flags, descriptor, reply)) {
                            AdvertisingIdTransaction.GET_ID -> {
                                AdvertisingIdBinderProtocol.writeIdReply(reply!!, spoofed)
                                true
                            }
                            AdvertisingIdTransaction.IS_LIMIT_AD_TRACKING_ENABLED -> {
                                AdvertisingIdBinderProtocol.writeLimitAdTrackingReply(reply!!, limited = true)
                                true
                            }
                            null -> chain.proceed()
                        }
                    },
                )
        }.onFailure { log("failed to install Advertising ID binder hook", it) }
    }

    private inline fun hookMethod(
        identifiers: JSONObject,
        key: String,
        profileId: Long,
        description: String,
        builder: (String) -> Pair<java.lang.reflect.Method, XposedInterface.Hooker>,
    ) {
        val value = resolveValue(identifiers, key, profileId) ?: return
        runCatching {
            val (method, hooker) = builder(value)
            hook(method)
                .setId("privacykitlite.${key}.${description.hashCode()}")
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(hooker)
        }.onFailure { log("failed to hook $description", it) }
    }

    private fun resolveValue(identifiers: JSONObject, key: String, profileId: Long): String? {
        val entry = identifiers.optJSONObject(key) ?: return null
        val rule = runCatching { IdentifierRuleType.valueOf(entry.optString("rule")) }.getOrNull() ?: return null
        if (rule == IdentifierRuleType.REAL) return null
        return entry.optString("value").takeIf { it.isNotBlank() }
    }

    private fun readRootConfig(): JSONObject? {
        val raw = getRemotePreferences(XposedConfigExporter.PREFS_GROUP)
            .getString(XposedConfigExporter.KEY_CONFIG, null)
            ?: return null
        return runCatching { JSONObject(raw) }.getOrNull()
    }

    @SuppressLint("DiscouragedPrivateApi")
    private fun setStaticFinalField(clazz: Class<*>, fieldName: String, value: Any) {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        runCatching {
            val accessFlags: Field = Field::class.java.getDeclaredField("accessFlags")
            accessFlags.isAccessible = true
            accessFlags.setInt(field, field.modifiers and Modifier.FINAL.inv())
        }
        field.set(null, value)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.filter { it.isLetterOrDigit() }
        return ByteArray(clean.length / 2) { i -> clean.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }

    private fun log(message: String) {
        Log.i(TAG, message)
    }

    private fun log(message: String, throwable: Throwable) {
        Log.w(TAG, message, throwable)
    }
}
