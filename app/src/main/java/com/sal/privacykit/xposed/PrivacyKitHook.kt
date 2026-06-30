package com.sal.privacykit.xposed

import android.os.Build
import android.os.IBinder
import android.os.Parcel
import android.provider.Settings
import android.util.Log
import com.sal.privacykit.data.model.IdentifierRuleType
import com.sal.privacykit.data.model.IdentifierValueGenerator
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import org.json.JSONObject
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val HOST_PACKAGE = "com.sal.privacykit.lite"
private const val TAG = "PrivacyKitLiteHook"

class PrivacyKitHook : XposedModule() {
    private var currentPackageName: String = ""

    override fun onPackageReady(param: PackageReadyParam) {
        val packageName = param.packageName
        if (packageName == HOST_PACKAGE) return
        currentPackageName = packageName

        val root = readRootConfig()
        applyCustomHooks(root, packageName, param.classLoader)

        val config = root?.optJSONObject(packageName) ?: return
        if (config.optString("profileMode") == "SHARED") return
        val identifiers = config.optJSONObject("identifiers") ?: return
        val profileId = config.optLong("profileId", 0)
        val classLoader = param.classLoader

        hookAdvertisingIdBinder(identifiers, profileId)
        hookAndroidId(identifiers, profileId)

        hookBuildField(identifiers, "build_model", "MODEL", profileId)
        hookBuildField(identifiers, "build_brand", "BRAND", profileId)
        hookBuildField(identifiers, "build_manufacturer", "MANUFACTURER", profileId)
        hookBuildField(identifiers, "build_device", "DEVICE", profileId)
        hookBuildField(identifiers, "build_product", "PRODUCT", profileId)

        hookWifi(identifiers, profileId, classLoader)
        hookTelephony(identifiers, profileId)
    }

    private fun hookAndroidId(identifiers: JSONObject, profileId: Long) {
        hookMethod(identifiers, "android_id", profileId, "Settings.Secure.getString") { spoofed ->
            val method = Settings.Secure::class.java.getMethod(
                "getString",
                android.content.ContentResolver::class.java,
                String::class.java,
            )
            method to XposedInterface.Hooker { chain ->
                if (chain.getArg(1) == Settings.Secure.ANDROID_ID) spoofed else chain.proceed()
            }
        }
    }

    private fun hookWifi(identifiers: JSONObject, profileId: Long, classLoader: ClassLoader) {
        hookMethod(identifiers, "wifi_mac", profileId, "WifiInfo.getMacAddress") { spoofed ->
            val clazz = Class.forName("android.net.wifi.WifiInfo", false, classLoader)
            clazz.getMethod("getMacAddress") to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "wifi_bssid", profileId, "WifiInfo.getBSSID") { spoofed ->
            val clazz = Class.forName("android.net.wifi.WifiInfo", false, classLoader)
            clazz.getMethod("getBSSID") to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "wifi_ssid", profileId, "WifiInfo.getSSID") { spoofed ->
            val clazz = Class.forName("android.net.wifi.WifiInfo", false, classLoader)
            clazz.getMethod("getSSID") to XposedInterface.Hooker { "\"$spoofed\"" }
        }
    }

    private fun hookTelephony(identifiers: JSONObject, profileId: Long) {
        val clazz = android.telephony.TelephonyManager::class.java
        hookMethod(identifiers, "imei", profileId, "TelephonyManager.getImei") { spoofed ->
            clazz.getMethod("getImei") to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "imei", profileId, "TelephonyManager.getDeviceId") { spoofed ->
            clazz.getMethod("getDeviceId") to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "imei", profileId, "TelephonyManager.getImei(slot)") { spoofed ->
            clazz.getMethod("getImei", Int::class.javaPrimitiveType) to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "imei", profileId, "TelephonyManager.getDeviceId(slot)") { spoofed ->
            clazz.getMethod("getDeviceId", Int::class.javaPrimitiveType) to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "subscriber_id", profileId, "TelephonyManager.getSubscriberId") { spoofed ->
            clazz.getMethod("getSubscriberId") to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "iccid", profileId, "TelephonyManager.getSimSerialNumber") { spoofed ->
            clazz.getMethod("getSimSerialNumber") to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "phone_number", profileId, "TelephonyManager.getLine1Number") { spoofed ->
            clazz.getMethod("getLine1Number") to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "network_operator", profileId, "TelephonyManager.getNetworkOperator") { spoofed ->
            clazz.getMethod("getNetworkOperator") to XposedInterface.Hooker { spoofed }
        }
        hookMethod(identifiers, "sim_operator", profileId, "TelephonyManager.getSimOperator") { spoofed ->
            clazz.getMethod("getSimOperator") to XposedInterface.Hooker { spoofed }
        }
    }

    private fun hookAdvertisingIdBinder(identifiers: JSONObject, profileId: Long) {
        val spoofed = resolveValue(identifiers, "advertising_id", profileId) ?: return
        runCatching {
            val binderProxy = Class.forName("android.os.BinderProxy")
            val transact = binderProxy.getDeclaredMethod(
                "transact",
                Int::class.javaPrimitiveType,
                Parcel::class.java,
                Parcel::class.java,
                Int::class.javaPrimitiveType,
            ).apply { isAccessible = true }
            hook(transact)
                .setId("privacykitlite.advertising_id.binder")
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(XposedInterface.Hooker { chain ->
                    val code = chain.getArg(0) as? Int ?: return@Hooker chain.proceed()
                    val reply = chain.getArg(2) as? Parcel
                    val flags = chain.getArg(3) as? Int ?: return@Hooker chain.proceed()
                    val interfaceDescriptor = runCatching {
                        (chain.thisObject as? IBinder)?.interfaceDescriptor
                    }.getOrNull()
                    val transaction = AdvertisingIdBinderProtocol.classify(code, flags, interfaceDescriptor, reply)
                        ?: return@Hooker chain.proceed()
                    try {
                        when (transaction) {
                            AdvertisingIdTransaction.GET_ID ->
                                AdvertisingIdBinderProtocol.writeIdReply(reply!!, spoofed)
                            AdvertisingIdTransaction.IS_LIMIT_AD_TRACKING_ENABLED ->
                                AdvertisingIdBinderProtocol.writeLimitAdTrackingReply(reply!!, limited = true)
                        }
                        true
                    } catch (error: Throwable) {
                        log("failed to spoof Advertising ID Binder transaction $code", error)
                        chain.proceed()
                    }
                })
        }.onFailure { log("failed to hook Advertising ID binder", it) }
    }

    private fun hookBuildField(identifiers: JSONObject, key: String, fieldName: String, profileId: Long) {
        val spoofed = resolveValue(identifiers, key, profileId) ?: return
        runCatching {
            setStaticFinalField(Build::class.java, fieldName, spoofed)
        }.onFailure { log("failed to spoof Build.$fieldName", it) }
    }

    private fun hookMethod(
        identifiers: JSONObject,
        key: String,
        profileId: Long,
        description: String,
        builder: (String) -> Pair<java.lang.reflect.Method, XposedInterface.Hooker>,
    ) {
        val spoofed = resolveValue(identifiers, key, profileId) ?: return
        runCatching {
            val (method, hooker) = builder(spoofed)
            hook(method).setId("privacykitlite.$description").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(hooker)
        }.onFailure { log("failed to hook $description", it) }
    }

    private fun resolveValue(identifiers: JSONObject, key: String, profileId: Long): String? {
        val entry = identifiers.optJSONObject(key) ?: return null
        val rule = runCatching { IdentifierRuleType.valueOf(entry.optString("rule")) }.getOrNull() ?: return null
        return when (rule) {
            IdentifierRuleType.REAL -> null
            IdentifierRuleType.STATIC, IdentifierRuleType.CUSTOM -> entry.optString("value").takeIf { it.isNotBlank() }
            IdentifierRuleType.RANDOM_PER_LAUNCH -> IdentifierValueGenerator.generate(key, System.nanoTime())
            IdentifierRuleType.RANDOM_DAILY -> {
                val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                IdentifierValueGenerator.generate(key, IdentifierValueGenerator.seedFor(profileId, key) + today.toLong())
            }
        }
    }

    private fun readRootConfig(): JSONObject? {
        val prefs = getRemotePreferences(XposedConfigExporter.PREFS_GROUP)
        val raw = prefs.getString(XposedConfigExporter.KEY_CONFIG, null) ?: return null
        return runCatching { JSONObject(raw) }.getOrNull()
    }

    private fun applyCustomHooks(root: JSONObject?, packageName: String, classLoader: ClassLoader) {
        val rules = root?.optJSONObject(XposedConfigExporter.KEY_GLOBAL_SETTINGS)
            ?.optJSONArray(XposedConfigExporter.KEY_CUSTOM_HOOKS) ?: return
        for (i in 0 until rules.length()) {
            val rule = rules.optJSONObject(i) ?: continue
            if (!rule.optBoolean("enabled", true)) continue
            val targetPackage = rule.optString("packageName").trim()
            if (targetPackage.isNotEmpty() && targetPackage != packageName) continue

            val className = rule.optString("className").trim()
            val member = rule.optString("member").trim()
            val type = rule.optString("type").trim()
            val value = resolveCustomHookValue(rule, className, member)
            runCatching {
                val clazz = Class.forName(className, false, classLoader)
                when (type) {
                    "field" -> {
                        val field = clazz.getDeclaredField(member)
                        setStaticFinalField(clazz, member, coerceValue(field.type, value))
                    }
                    "method" -> {
                        val argTypes = rule.optString("argTypes")
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .map(::resolveType)
                            .toTypedArray()
                        val method = clazz.getMethod(member, *argTypes)
                        val coerced = coerceValue(method.returnType, value)
                        hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                            .intercept(XposedInterface.Hooker { coerced })
                    }
                    else -> error("unknown custom hook type '$type'")
                }
            }.onFailure { log("failed to apply custom hook $className.$member", it) }
        }
    }

    private fun resolveCustomHookValue(rule: JSONObject, className: String, member: String): String {
        val ruleType = rule.optString("rule", "static")
        val randomValues = rule.optJSONArray("randomValues")
        if (ruleType == "static" || randomValues == null || randomValues.length() == 0) {
            return rule.optString("value")
        }
        val seed = when (ruleType) {
            "random_per_launch" -> System.nanoTime()
            "random_daily" -> {
                val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                today.toLong() + (className + member + currentPackageName).hashCode()
            }
            else -> return rule.optString("value")
        }
        val index = (kotlin.math.abs(seed) % randomValues.length()).toInt()
        return randomValues.optString(index, rule.optString("value"))
    }

    private fun resolveType(name: String): Class<*> = when (name) {
        "int" -> Int::class.javaPrimitiveType!!
        "long" -> Long::class.javaPrimitiveType!!
        "boolean" -> Boolean::class.javaPrimitiveType!!
        "byte" -> Byte::class.javaPrimitiveType!!
        "short" -> Short::class.javaPrimitiveType!!
        "float" -> Float::class.javaPrimitiveType!!
        "double" -> Double::class.javaPrimitiveType!!
        "char" -> Char::class.javaPrimitiveType!!
        "String" -> String::class.java
        else -> Class.forName(name)
    }

    private fun coerceValue(targetType: Class<*>, value: String): Any = when (targetType) {
        Int::class.javaPrimitiveType, Integer::class.java -> value.toInt()
        Long::class.javaPrimitiveType, java.lang.Long::class.java -> value.toLong()
        Boolean::class.javaPrimitiveType, java.lang.Boolean::class.java -> value.toBoolean()
        Float::class.javaPrimitiveType, java.lang.Float::class.java -> value.toFloat()
        Double::class.javaPrimitiveType, java.lang.Double::class.java -> value.toDouble()
        else -> value
    }

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

    private fun log(message: String, throwable: Throwable? = null) {
        if (throwable == null) {
            Log.i(TAG, message)
        } else {
            Log.w(TAG, message, throwable)
        }
    }
}
