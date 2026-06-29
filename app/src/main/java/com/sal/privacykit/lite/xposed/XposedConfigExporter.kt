package com.sal.privacykit.lite.xposed

import com.sal.privacykit.lite.model.IdentifierRuleType
import com.sal.privacykit.lite.model.LiteProfile
import org.json.JSONObject

object XposedConfigExporter {
    const val PREFS_GROUP = "xposed_config"
    const val KEY_CONFIG = "config_json"

    fun export(connection: XposedServiceConnection, profiles: List<LiteProfile>): Boolean {
        val service = connection.service ?: return false
        return runCatching {
            val root = JSONObject()
            profiles.filter { it.enabled }.forEach { profile ->
                root.put(profile.packageName, profileToJson(profile))
            }
            service.getRemotePreferences(PREFS_GROUP)
                .edit()
                .putString(KEY_CONFIG, root.toString())
                .apply()
            true
        }.getOrDefault(false)
    }

    private fun profileToJson(profile: LiteProfile): JSONObject {
        val identifiers = JSONObject()
        profile.rules.forEach { (key, rule) ->
            identifiers.put(
                key,
                JSONObject().apply {
                    put("rule", rule.name)
                    if (rule != IdentifierRuleType.REAL) put("value", profile.values[key].orEmpty())
                },
            )
        }
        return JSONObject()
            .put("profileId", profile.id)
            .put("identifiers", identifiers)
    }
}
