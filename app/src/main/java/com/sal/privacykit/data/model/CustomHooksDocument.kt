package com.sal.privacykit.data.model

import org.json.JSONArray
import org.json.JSONObject

enum class CustomHookScope {
    APP,
    GLOBAL,
}

data class CustomHookRuleSnapshot(
    val index: Int,
    val raw: JSONObject,
    val enabled: Boolean,
    val packageName: String,
    val allowGlobal: Boolean,
    val className: String,
    val member: String,
    val type: String,
    val argTypes: String,
    val rule: String,
    val value: String,
    val isDocumentationOnly: Boolean,
) {
    val scope: CustomHookScope = if (packageName.isBlank()) CustomHookScope.GLOBAL else CustomHookScope.APP
    val targetLabel: String
        get() {
            val shortClassName = className.substringAfterLast('.').ifBlank { className }
            val memberLabel = if (type == "method" && argTypes.isNotBlank()) "$member($argTypes)" else member
            return if (shortClassName.isBlank() && memberLabel.isBlank()) "Untitled rule" else listOf(shortClassName, memberLabel).filter { it.isNotBlank() }.joinToString(".")
        }
    val scopeLabel: String
        get() = if (scope == CustomHookScope.GLOBAL) "Global" else "App: $packageName"
    val typeLabel: String
        get() = if (type.isBlank()) "Unknown type" else type.replaceFirstChar { it.uppercase() }
}

data class CustomHooksDocumentSnapshot(
    val rawJson: String,
    val parseable: Boolean,
    val validation: CustomHookValidator.DetailedResult,
    val rules: List<CustomHookRuleSnapshot>,
) {
    val visibleRules: List<CustomHookRuleSnapshot> = rules.filterNot { it.isDocumentationOnly }
    val enabledCount: Int = visibleRules.count { it.enabled }
    val appScopedCount: Int = visibleRules.count { it.scope == CustomHookScope.APP }
    val globalCount: Int = visibleRules.count { it.scope == CustomHookScope.GLOBAL }
    val isValid: Boolean = parseable && validation.isValid
    val errorCount: Int = validation.errorCount
    val issuesByRule: Map<Int, List<String>> = validation.issues
        .filter { it.index != null }
        .groupBy({ it.index!! }, { it.message })
}

data class CustomHookRuleDraft(
    val enabled: Boolean = false,
    val scope: CustomHookScope = CustomHookScope.APP,
    val packageName: String = "",
    val allowGlobal: Boolean = false,
    val className: String = "",
    val member: String = "",
    val type: String = "field",
    val argTypes: String = "",
    val rule: String = "static",
    val value: String = "",
    val preservedRaw: JSONObject? = null,
)

object CustomHooksDocumentParser {
    fun parse(rawJson: String): CustomHooksDocumentSnapshot {
        val validation = CustomHookValidator.validateDetailed(rawJson)
        val trimmed = rawJson.ifBlank { "[]" }.trim()
        val array = runCatching { JSONArray(trimmed) }.getOrNull()
        if (array == null) {
            return CustomHooksDocumentSnapshot(
                rawJson = rawJson,
                parseable = false,
                validation = validation,
                rules = emptyList(),
            )
        }
        val rules = buildList {
            for (index in 0 until array.length()) {
                val rule = array.optJSONObject(index) ?: continue
                add(rule.toRuleSnapshot(index))
            }
        }
        return CustomHooksDocumentSnapshot(
            rawJson = rawJson,
            parseable = true,
            validation = validation,
            rules = rules,
        )
    }

    fun serialize(array: JSONArray): String = JSONArray().also { output ->
        for (index in 0 until array.length()) {
            output.put(array.get(index))
        }
    }.toString(2)

    fun serialize(objects: List<JSONObject>): String = JSONArray().also { output ->
        objects.forEach { output.put(JSONObject(it.toString())) }
    }.toString(2)

    fun newRuleDraft(): CustomHookRuleDraft = CustomHookRuleDraft()

    fun draftFromSnapshot(rule: CustomHookRuleSnapshot): CustomHookRuleDraft =
        CustomHookRuleDraft(
            enabled = rule.enabled,
            scope = rule.scope,
            packageName = rule.packageName,
            allowGlobal = rule.allowGlobal,
            className = rule.className,
            member = rule.member,
            type = rule.type,
            argTypes = rule.argTypes,
            rule = rule.rule,
            value = rule.value,
            preservedRaw = JSONObject(rule.raw.toString()),
        )

    fun duplicateDraftFromSnapshot(rule: CustomHookRuleSnapshot): CustomHookRuleDraft =
        draftFromSnapshot(rule).copy(
            enabled = false,
            scope = CustomHookScope.APP,
            allowGlobal = false,
        )

    fun draftToJson(draft: CustomHookRuleDraft): JSONObject =
        (draft.preservedRaw?.let { JSONObject(it.toString()) } ?: JSONObject()).apply {
            put("enabled", draft.enabled)
            put("packageName", if (draft.scope == CustomHookScope.GLOBAL) "" else draft.packageName.trim())
            if (draft.scope == CustomHookScope.GLOBAL) {
                put("allowGlobal", draft.allowGlobal)
            } else {
                remove("allowGlobal")
            }
            put("className", draft.className.trim())
            put("member", draft.member.trim())
            put("type", draft.type.trim())
            put("argTypes", draft.argTypes.trim())
            put("rule", "static")
            remove("randomValues")
            put("value", draft.value)
        }

    fun toMutableArray(rawJson: String): JSONArray = JSONArray(rawJson.ifBlank { "[]" }.trim())

    private fun JSONObject.toRuleSnapshot(index: Int): CustomHookRuleSnapshot {
        return CustomHookRuleSnapshot(
            index = index,
            raw = JSONObject(toString()),
            enabled = optBoolean("enabled", true),
            packageName = optString("packageName").trim(),
            allowGlobal = optBoolean("allowGlobal", false),
            className = optString("className").trim(),
            member = optString("member").trim(),
            type = optString("type").trim(),
            argTypes = optString("argTypes").trim(),
            rule = optString("rule", "static").trim().ifBlank { "static" },
            value = optString("value"),
            isDocumentationOnly = !has("className") && (has("_section") || has("_note")),
        )
    }
}

data class CustomHookMutationResult(
    val rawJson: String? = null,
    val message: String? = null,
) {
    val isSuccess: Boolean get() = rawJson != null
}

object CustomHooksDocumentEditor {
    fun newFieldDraft(): CustomHookRuleDraft = CustomHookRuleDraft(type = "field")

    fun newMethodDraft(): CustomHookRuleDraft = CustomHookRuleDraft(type = "method")

    fun appendRule(rawJson: String, draft: CustomHookRuleDraft): CustomHookMutationResult =
        mutate(rawJson) { rules ->
            rules.add(CustomHooksDocumentParser.draftToJson(draft))
        }

    fun replaceRule(rawJson: String, index: Int, draft: CustomHookRuleDraft): CustomHookMutationResult =
        mutate(rawJson) { rules ->
            if (index !in rules.indices) error("Rule ${index + 1} no longer exists.")
            rules[index] = CustomHooksDocumentParser.draftToJson(draft)
        }

    fun toggleRuleEnabled(rawJson: String, index: Int, enabled: Boolean): CustomHookMutationResult =
        mutate(rawJson) { rules ->
            if (index !in rules.indices) error("Rule ${index + 1} no longer exists.")
            rules[index] = JSONObject(rules[index].toString()).apply {
                put("enabled", enabled)
            }
        }

    fun duplicateRule(rawJson: String, index: Int): CustomHookMutationResult =
        mutate(rawJson) { rules ->
            if (index !in rules.indices) error("Rule ${index + 1} no longer exists.")
            rules.add(index + 1, JSONObject(rules[index].toString()).apply { put("enabled", false) })
        }

    fun deleteRule(rawJson: String, index: Int): CustomHookMutationResult =
        mutate(rawJson) { rules ->
            if (index !in rules.indices) error("Rule ${index + 1} no longer exists.")
            rules.removeAt(index)
        }

    private fun mutate(
        rawJson: String,
        transform: (MutableList<JSONObject>) -> Unit,
    ): CustomHookMutationResult {
        val trimmed = rawJson.ifBlank { "[]" }.trim()
        val array = runCatching { JSONArray(trimmed) }.getOrElse {
            return CustomHookMutationResult(message = CustomHookValidator.validateDetailed(rawJson).firstMessage ?: "Invalid custom hooks JSON")
        }
        val rules = buildList {
            for (index in 0 until array.length()) {
                val rule = array.optJSONObject(index) ?: run {
                    return CustomHookMutationResult(
                        message = CustomHookValidator.validateDetailed(rawJson).firstMessage
                            ?: "Custom hooks JSON must contain only objects.",
                    )
                }
                add(JSONObject(rule.toString()))
            }
        }.toMutableList()
        transform(rules)
        val candidate = CustomHooksDocumentParser.serialize(rules)
        val validation = CustomHookValidator.validate(candidate)
        return if (validation.isValid) {
            CustomHookMutationResult(rawJson = candidate)
        } else {
            CustomHookMutationResult(message = validation.message ?: "Custom hooks JSON is invalid")
        }
    }
}
