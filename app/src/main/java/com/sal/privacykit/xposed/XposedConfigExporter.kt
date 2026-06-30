package com.sal.privacykit.xposed

import com.sal.privacykit.data.model.CustomHookValidator
import com.sal.privacykit.data.model.DeviceTemplateCatalog
import com.sal.privacykit.data.model.IdentifierCatalog
import com.sal.privacykit.data.model.Profile
import com.sal.privacykit.data.repository.selectActiveOrFallback
import org.json.JSONObject

object XposedConfigExporter {
    const val PREFS_GROUP = "xposed_config"
    const val KEY_CONFIG = "config_json"
    const val KEY_GLOBAL_SETTINGS = "__privacy_kit_settings__"
    const val KEY_CUSTOM_HOOKS = "customHooks"

    data class Snapshot(
        val serviceConnected: Boolean,
        val exportedAppIds: Set<String>?,
        val scopedAppIds: Set<String>?,
        val error: String? = null,
    )

    fun export(
        serviceConnection: XposedServiceConnection,
        profiles: List<Profile>,
        activeProfileByApp: Map<String, Long>,
        customHooksJson: String = "[]",
    ) {
        val service = serviceConnection.service ?: return
        val root = JSONObject()
        profiles.groupBy { it.appId }.forEach { (appId, appProfiles) ->
            val profile = selectActiveOrFallback(appProfiles, activeProfileByApp[appId]) ?: return@forEach
            root.put(appId, profileToJson(profile))
        }
        val customHooks = CustomHookValidator.parseValidatedArray(customHooksJson)
        root.put(KEY_GLOBAL_SETTINGS, JSONObject().put(KEY_CUSTOM_HOOKS, customHooks))
        service.getRemotePreferences(PREFS_GROUP).edit().putString(KEY_CONFIG, root.toString()).apply()
    }

    fun snapshot(serviceConnection: XposedServiceConnection): Snapshot {
        val service = serviceConnection.service ?: return Snapshot(false, null, null)
        val scopedAppIds = runCatching { service.scope.toSet() }.getOrNull()
        val rawConfig = runCatching {
            service.getRemotePreferences(PREFS_GROUP).getString(KEY_CONFIG, null)
        }.getOrElse { error ->
            return Snapshot(true, null, scopedAppIds, error.message)
        }
        val exportedAppIds = rawConfig?.let { raw ->
            runCatching {
                val root = JSONObject(raw)
                root.keys().asSequence().filterNot { it == KEY_GLOBAL_SETTINGS }.toSet()
            }.getOrElse { error ->
                return Snapshot(true, null, scopedAppIds, error.message)
            }
        } ?: emptySet()
        return Snapshot(true, exportedAppIds, scopedAppIds)
    }

    internal fun profileToJson(profile: Profile): JSONObject {
        val template = DeviceTemplateCatalog.byId(profile.deviceTemplateId)
        val identifiers = JSONObject()
        profile.identifierRules
            .filterKeys { it in IdentifierCatalog.allowedKeys }
            .forEach { (key, rule) ->
                identifiers.put(
                    key,
                    JSONObject().apply {
                        put("rule", rule.name)
                        profile.identifierValues[key]?.let { put("value", it) }
                    },
                )
            }
        return JSONObject().apply {
            put("profileId", profile.id)
            put("deviceModel", profile.deviceModel(template.name))
            put("deviceManufacturer", profile.deviceManufacturer(template.manufacturer))
            put("identifiers", identifiers)
        }
    }
}
