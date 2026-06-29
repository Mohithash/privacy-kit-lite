package com.sal.privacykit.lite.data

import android.content.Context
import com.sal.privacykit.lite.model.IdentifierCatalog
import com.sal.privacykit.lite.model.IdentifierRuleType
import com.sal.privacykit.lite.model.IdentifierValueGenerator
import com.sal.privacykit.lite.model.LiteProfile
import org.json.JSONArray
import org.json.JSONObject

class LiteProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("privacy_kit_lite_profiles", Context.MODE_PRIVATE)

    fun load(): List<LiteProfile> {
        val raw = prefs.getString(KEY_PROFILES, null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val packageName = obj.optString("packageName").takeIf { it.isNotBlank() } ?: continue
                val id = obj.optLong("id", packageName.hashCode().toLong())
                val rulesJson = obj.optJSONObject("rules") ?: JSONObject()
                val valuesJson = obj.optJSONObject("values") ?: JSONObject()
                val rules = IdentifierCatalog.items.associate { item ->
                    val rawRule = rulesJson.optString(item.key, if (item.defaultEnabled) "STATIC" else "REAL")
                    item.key to runCatching { IdentifierRuleType.valueOf(rawRule) }.getOrDefault(IdentifierRuleType.REAL)
                }
                val values = IdentifierCatalog.items.associate { item ->
                    item.key to valuesJson.optString(item.key, IdentifierValueGenerator.generate(item.key, IdentifierValueGenerator.seedFor(id, item.key)))
                }
                add(
                    LiteProfile(
                        id = id,
                        packageName = packageName,
                        label = obj.optString("label", packageName),
                        enabled = obj.optBoolean("enabled", true),
                        rules = rules,
                        values = values,
                    ),
                )
            }
        }.sortedBy { it.label.lowercase() }
    }

    fun save(profiles: List<LiteProfile>) {
        val array = JSONArray()
        profiles.sortedBy { it.label.lowercase() }.forEach { profile ->
            array.put(
                JSONObject()
                    .put("id", profile.id)
                    .put("packageName", profile.packageName)
                    .put("label", profile.label)
                    .put("enabled", profile.enabled)
                    .put("rules", JSONObject().apply {
                        profile.rules.forEach { (key, rule) ->
                            if (key in IdentifierCatalog.keys) put(key, rule.name)
                        }
                    })
                    .put("values", JSONObject().apply {
                        profile.values.forEach { (key, value) ->
                            if (key in IdentifierCatalog.keys) put(key, value)
                        }
                    }),
            )
        }
        prefs.edit().putString(KEY_PROFILES, array.toString()).apply()
    }

    fun create(packageName: String, label: String): LiteProfile {
        val id = System.currentTimeMillis()
        val rules = IdentifierCatalog.items.associate { it.key to if (it.defaultEnabled) IdentifierRuleType.STATIC else IdentifierRuleType.REAL }
        val values = IdentifierCatalog.items.associate { it.key to IdentifierValueGenerator.generate(it.key, IdentifierValueGenerator.seedFor(id, it.key)) }
        return LiteProfile(id, packageName, label, enabled = true, rules = rules, values = values)
    }

    fun regenerate(profile: LiteProfile): LiteProfile {
        return profile.copy(values = regenerateGeneratedValues(profile, System.nanoTime()))
    }

    companion object {
        private const val KEY_PROFILES = "profiles_json"
    }
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
