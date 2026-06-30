package com.sal.privacykit.data.model

import org.json.JSONArray
import org.json.JSONObject

object CustomHookValidator {
    private const val MAX_JSON_BYTES = 128 * 1024
    private const val MAX_RULES = 200
    private const val MAX_STRING_LENGTH = 512
    private val packageNameRegex = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")
    private val classNameRegex = Regex("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")
    private val memberRegex = Regex("[A-Za-z_$][A-Za-z0-9_$]*")
    private val allowedTypes = setOf("field", "method")
    private val allowedRules = setOf("static", "random_per_launch", "random_daily")

    data class Result(val isValid: Boolean, val message: String? = null) {
        companion object {
            val Valid = Result(true)
        }
    }

    data class ValidationIssue(val index: Int? = null, val message: String)

    data class DetailedResult(val isValid: Boolean, val issues: List<ValidationIssue>) {
        val errorCount: Int get() = issues.size
        val firstMessage: String? get() = issues.firstOrNull()?.message
    }

    fun validate(rawJson: String): Result {
        val detailed = validateDetailed(rawJson)
        return if (detailed.isValid) Result.Valid else Result(false, detailed.firstMessage)
    }

    fun validateDetailed(rawJson: String): DetailedResult {
        val trimmed = rawJson.ifBlank { "[]" }.trim()
        if (trimmed.toByteArray(Charsets.UTF_8).size > MAX_JSON_BYTES) {
            return DetailedResult(false, listOf(ValidationIssue(message = "Custom hooks JSON is too large. Keep it under ${MAX_JSON_BYTES / 1024} KB.")))
        }
        val array = runCatching { JSONArray(trimmed) }.getOrElse { error ->
            return DetailedResult(false, listOf(ValidationIssue(message = "Invalid JSON: ${error.message ?: "could not parse array"}")))
        }
        if (array.length() > MAX_RULES) {
            return DetailedResult(false, listOf(ValidationIssue(message = "Too many custom hooks. Keep the list to $MAX_RULES rules or fewer.")))
        }

        val issues = mutableListOf<ValidationIssue>()
        val activeTargets = mutableSetOf<String>()
        for (index in 0 until array.length()) {
            val rule = array.optJSONObject(index)
            val ruleNumber = index + 1
            if (rule == null) {
                issues += ValidationIssue(ruleNumber, "Rule $ruleNumber must be a JSON object.")
                continue
            }
            val enabled = rule.optBoolean("enabled", true)
            if (!enabled && isDocumentationOnly(rule)) continue

            val packageName = rule.optString("packageName").trim()
            val className = rule.optString("className").trim()
            val member = rule.optString("member").trim()
            val type = rule.optString("type").trim()
            val hookRule = rule.optString("rule", "static").trim().ifBlank { "static" }

            if (className.isBlank()) issues += ValidationIssue(ruleNumber, "Rule $ruleNumber is missing className.")
            if (className.isNotBlank() && !classNameRegex.matches(className)) issues += ValidationIssue(ruleNumber, "Rule $ruleNumber has an invalid className.")
            if (member.isBlank()) issues += ValidationIssue(ruleNumber, "Rule $ruleNumber is missing member.")
            if (member.isNotBlank() && !memberRegex.matches(member)) issues += ValidationIssue(ruleNumber, "Rule $ruleNumber has an invalid member name.")
            if (type.isBlank()) issues += ValidationIssue(ruleNumber, "Rule $ruleNumber is missing type.")
            if (type.isNotBlank() && type !in allowedTypes) issues += ValidationIssue(ruleNumber, "Rule $ruleNumber type must be \"field\" or \"method\".")
            if (hookRule !in allowedRules) issues += ValidationIssue(ruleNumber, "Rule $ruleNumber rule must be static, random_per_launch, or random_daily.")

            if (packageName.isBlank()) {
                if (enabled && !rule.optBoolean("allowGlobal", false)) {
                    issues += ValidationIssue(ruleNumber, "Rule $ruleNumber applies to every app. Add \"allowGlobal\": true to confirm.")
                }
            } else if (!packageNameRegex.matches(packageName)) {
                issues += ValidationIssue(ruleNumber, "Rule $ruleNumber has an invalid packageName.")
            }

            if (type == "method") {
                val argError = validateArgTypes(index, rule.optString("argTypes"))
                if (argError != null) issues += ValidationIssue(ruleNumber, argError.message.orEmpty())
            }

            if (hookRule == "static") {
                if (!rule.has("value")) {
                    issues += ValidationIssue(ruleNumber, "Rule $ruleNumber is missing value.")
                }
                val value = rule.opt("value")
                if (value !is String) issues += ValidationIssue(ruleNumber, "Rule $ruleNumber value must be a string.")
                if (value is String && value.length > MAX_STRING_LENGTH) {
                    issues += ValidationIssue(ruleNumber, "Rule $ruleNumber value is too long.")
                }
            } else {
                val values = rule.optJSONArray("randomValues")
                    ?: run {
                        issues += ValidationIssue(ruleNumber, "Rule $ruleNumber needs randomValues for $hookRule.")
                        null
                    }
                if (values != null && values.length() == 0) issues += ValidationIssue(ruleNumber, "Rule $ruleNumber randomValues cannot be empty.")
                if (values != null) {
                    for (valueIndex in 0 until values.length()) {
                        val value = values.opt(valueIndex)
                        if (value !is String) issues += ValidationIssue(ruleNumber, "Rule $ruleNumber randomValues must contain strings only.")
                        if (value is String && value.length > MAX_STRING_LENGTH) issues += ValidationIssue(ruleNumber, "Rule $ruleNumber randomValues entry is too long.")
                    }
                }
            }

            if (enabled) {
                val target = listOf(packageName, className, member, type, rule.optString("argTypes").trim())
                    .joinToString("|")
                if (!activeTargets.add(target)) {
                    issues += ValidationIssue(ruleNumber, "Rule $ruleNumber duplicates an enabled hook target.")
                }
            }
        }
        return DetailedResult(issues.isEmpty(), issues)
    }

    fun parseValidatedArray(rawJson: String): JSONArray =
        if (validate(rawJson).isValid) JSONArray(rawJson.ifBlank { "[]" }.trim()) else JSONArray()

    private fun isDocumentationOnly(rule: JSONObject): Boolean =
        !rule.has("className") && (rule.has("_section") || rule.has("_note"))

    private fun validateArgTypes(index: Int, rawArgTypes: String): Result? {
        if (rawArgTypes.length > MAX_STRING_LENGTH) return Result(false, "Rule ${index + 1} argTypes is too long.")
        rawArgTypes.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { type ->
                val isPrimitive = type in setOf("int", "long", "boolean", "byte", "short", "float", "double", "char", "String")
                if (!isPrimitive && !classNameRegex.matches(type)) {
                    return Result(false, "Rule ${index + 1} has invalid argTypes entry \"$type\".")
                }
            }
        return null
    }
}
