package com.sal.privacykit.lite.xposed

import com.sal.privacykit.lite.model.IdentifierRuleType
import com.sal.privacykit.lite.model.LiteProfile
import com.sal.privacykit.lite.model.LiteState
import org.json.JSONObject

object XposedConfigExporter {
    const val PREFS_GROUP = "xposed_config"
    const val KEY_CONFIG = "config_json"

    fun export(connection: XposedServiceConnection, state: LiteState): Boolean {
        val service = connection.service ?: return false
        return runCatching {
            service.getRemotePreferences(PREFS_GROUP)
                .edit()
                .putString(KEY_CONFIG, exportJson(state).toString())
                .apply()
            true
        }.getOrDefault(false)
    }

    internal fun exportJson(state: LiteState): JSONObject {
        val profilesById = state.profiles.associateBy { it.id }
        val root = JSONObject()
        state.assignments
            .filter { it.enabled }
            .forEach { assignment ->
                val profile = profilesById[assignment.profileId] ?: return@forEach
                if (profile.enabled) {
                    root.put(assignment.packageName, profileToJson(profile))
                }
            }
        return root
    }

    internal fun profileToJson(profile: LiteProfile): JSONObject {
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
            .put("profileName", profile.name)
            .put("identifiers", identifiers)
    }
}
