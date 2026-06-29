package com.sal.privacykit.lite.model

data class LiteProfile(
    val id: Long,
    val packageName: String,
    val label: String,
    val enabled: Boolean,
    val rules: Map<String, IdentifierRuleType>,
    val values: Map<String, String>,
) {
    fun enabledCount(): Int = rules.count { (key, rule) ->
        rule != IdentifierRuleType.REAL && !values[key].isNullOrBlank()
    }
}
