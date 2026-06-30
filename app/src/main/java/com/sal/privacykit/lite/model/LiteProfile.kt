package com.sal.privacykit.lite.model

data class LiteProfile(
    val id: Long,
    val name: String,
    val enabled: Boolean,
    val rules: Map<String, IdentifierRuleType>,
    val values: Map<String, String>,
) {
    fun enabledCount(): Int = rules.count { (key, rule) ->
        rule != IdentifierRuleType.REAL && !values[key].isNullOrBlank()
    }
}

data class LiteAppAssignment(
    val packageName: String,
    val label: String,
    val profileId: Long,
    val enabled: Boolean = true,
)

data class LiteState(
    val profiles: List<LiteProfile>,
    val assignments: List<LiteAppAssignment>,
) {
    fun selectedProfile(id: Long?): LiteProfile? =
        profiles.firstOrNull { it.id == id } ?: profiles.firstOrNull()

    fun assignmentsFor(profileId: Long): List<LiteAppAssignment> =
        assignments.filter { it.profileId == profileId }.sortedBy { it.label.lowercase() }
}
