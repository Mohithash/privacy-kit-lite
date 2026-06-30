package com.sal.privacykit.lite.data

import android.content.Context
import com.sal.privacykit.lite.model.IdentifierCatalog
import com.sal.privacykit.lite.model.IdentifierRuleType
import com.sal.privacykit.lite.model.IdentifierValueGenerator
import com.sal.privacykit.lite.model.LiteAppAssignment
import com.sal.privacykit.lite.model.LiteProfile
import com.sal.privacykit.lite.model.LiteState
import org.json.JSONArray
import org.json.JSONObject

class LiteProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("privacy_kit_lite_profiles", Context.MODE_PRIVATE)

    fun loadState(): LiteState {
        val rawState = prefs.getString(KEY_STATE, null)
        if (rawState != null) {
            LiteStateCodec.decode(rawState)?.let { return it.normalized() }
        }
        val legacy = prefs.getString(KEY_LEGACY_PROFILES, null)
        return legacy?.let(LiteStateCodec::decodeLegacyProfiles)?.normalized() ?: LiteState(emptyList(), emptyList())
    }

    fun saveState(state: LiteState) {
        prefs.edit()
            .putString(KEY_STATE, LiteStateCodec.encode(state.normalized()))
            .remove(KEY_LEGACY_PROFILES)
            .apply()
    }

    fun createProfile(name: String): LiteProfile {
        val id = System.currentTimeMillis()
        val rules = IdentifierCatalog.items.associate { item ->
            item.key to if (item.defaultEnabled) IdentifierRuleType.STATIC else IdentifierRuleType.REAL
        }
        val values = IdentifierCatalog.items.associate { item ->
            item.key to IdentifierValueGenerator.generate(item.key, IdentifierValueGenerator.seedFor(id, item.key))
        }
        return LiteProfile(id, name.ifBlank { "New profile" }, enabled = true, rules = rules, values = values)
    }

    fun duplicate(profile: LiteProfile, name: String): LiteProfile {
        val id = System.currentTimeMillis()
        return profile.copy(id = id, name = name.ifBlank { "${profile.name} Copy" })
    }

    fun regenerate(profile: LiteProfile): LiteProfile =
        profile.copy(values = regenerateGeneratedValues(profile, System.nanoTime()))

    companion object {
        private const val KEY_STATE = "state_json_v2"
        private const val KEY_LEGACY_PROFILES = "profiles_json"
    }
}

object LiteStateCodec {
    fun encode(state: LiteState): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put(
            "profiles",
            JSONArray().apply {
                state.profiles.sortedBy { it.name.lowercase() }.forEach { profile ->
                    put(profileToJson(profile))
                }
            },
        )
        root.put(
            "assignments",
            JSONArray().apply {
                state.assignments.sortedWith(compareBy({ it.label.lowercase() }, { it.packageName })).forEach { assignment ->
                    put(assignmentToJson(assignment))
                }
            },
        )
        return root.toString()
    }

    fun decode(raw: String): LiteState? {
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val profiles = decodeProfiles(root.optJSONArray("profiles") ?: JSONArray())
        val profileIds = profiles.map { it.id }.toSet()
        val assignments = decodeAssignments(root.optJSONArray("assignments") ?: JSONArray())
            .filter { it.profileId in profileIds }
        return LiteState(profiles, assignments)
    }

    fun decodeLegacyProfiles(raw: String): LiteState? {
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return null
        val profiles = mutableListOf<LiteProfile>()
        val assignments = mutableListOf<LiteAppAssignment>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val packageName = obj.optString("packageName").takeIf { it.isNotBlank() } ?: continue
            val id = obj.optLong("id", packageName.hashCode().toLong())
            val label = obj.optString("label", packageName)
            profiles += profileFromJson(obj, fallbackId = id, fallbackName = label)
            assignments += LiteAppAssignment(
                packageName = packageName,
                label = label,
                profileId = id,
                enabled = true,
            )
        }
        return LiteState(profiles, assignments)
    }

    private fun decodeProfiles(array: JSONArray): List<LiteProfile> = buildList {
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            add(profileFromJson(obj))
        }
    }

    private fun decodeAssignments(array: JSONArray): List<LiteAppAssignment> = buildList {
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val packageName = obj.optString("packageName").takeIf { it.isNotBlank() } ?: continue
            add(
                LiteAppAssignment(
                    packageName = packageName,
                    label = obj.optString("label", packageName),
                    profileId = obj.optLong("profileId"),
                    enabled = obj.optBoolean("enabled", true),
                ),
            )
        }
    }

    private fun profileFromJson(
        obj: JSONObject,
        fallbackId: Long = System.currentTimeMillis(),
        fallbackName: String = "Profile",
    ): LiteProfile {
        val id = obj.optLong("id", fallbackId)
        val rulesJson = obj.optJSONObject("rules") ?: JSONObject()
        val valuesJson = obj.optJSONObject("values") ?: JSONObject()
        val rules = IdentifierCatalog.items.associate { item ->
            val defaultRule = if (item.defaultEnabled) "STATIC" else "REAL"
            val rawRule = rulesJson.optString(item.key, defaultRule)
            item.key to runCatching { IdentifierRuleType.valueOf(rawRule) }.getOrDefault(IdentifierRuleType.REAL)
        }
        val values = IdentifierCatalog.items.associate { item ->
            item.key to valuesJson.optString(
                item.key,
                IdentifierValueGenerator.generate(item.key, IdentifierValueGenerator.seedFor(id, item.key)),
            )
        }
        return LiteProfile(
            id = id,
            name = obj.optString("name", obj.optString("label", fallbackName)).ifBlank { fallbackName },
            enabled = obj.optBoolean("enabled", true),
            rules = rules,
            values = values,
        )
    }

    private fun profileToJson(profile: LiteProfile): JSONObject =
        JSONObject()
            .put("id", profile.id)
            .put("name", profile.name)
            .put("enabled", profile.enabled)
            .put(
                "rules",
                JSONObject().apply {
                    profile.rules.forEach { (key, rule) ->
                        if (key in IdentifierCatalog.keys) put(key, rule.name)
                    }
                },
            )
            .put(
                "values",
                JSONObject().apply {
                    profile.values.forEach { (key, value) ->
                        if (key in IdentifierCatalog.keys) put(key, value)
                    }
                },
            )

    private fun assignmentToJson(assignment: LiteAppAssignment): JSONObject =
        JSONObject()
            .put("packageName", assignment.packageName)
            .put("label", assignment.label)
            .put("profileId", assignment.profileId)
            .put("enabled", assignment.enabled)
}

private fun LiteState.normalized(): LiteState {
    val distinctProfiles = profiles
        .distinctBy { it.id }
        .sortedBy { it.name.lowercase() }
    val ids = distinctProfiles.map { it.id }.toSet()
    val distinctAssignments = assignments
        .filter { it.profileId in ids }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
    return LiteState(distinctProfiles, distinctAssignments)
}

internal fun regenerateGeneratedValues(profile: LiteProfile, nonce: Long): Map<String, String> =
    IdentifierCatalog.items.associate { item ->
        val current = profile.values[item.key].orEmpty()
        val value = if (profile.rules[item.key] == IdentifierRuleType.STATIC) {
            IdentifierValueGenerator.generate(item.key, IdentifierValueGenerator.seedFor(profile.id + nonce, item.key))
        } else {
            current
        }
        item.key to value
    }
