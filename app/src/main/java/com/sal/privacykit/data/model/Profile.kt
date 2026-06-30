package com.sal.privacykit.data.model

enum class ProfileTagColor { RED, BLUE, GREEN }

data class Profile(
    val id: Long,
    val name: String,
    val appId: String,
    val deviceTemplateId: String,
    val profileMode: ProfileMode,
    val identifierRules: Map<String, IdentifierRuleType>,
    val createdAt: Long,
    val customDeviceName: String? = null,
    val customDeviceManufacturer: String? = null,
    val skipLaunchConfirmation: Boolean = false,
    val identifierValues: Map<String, String> = emptyMap(),
    val archived: Boolean = false,
    val tagColor: ProfileTagColor? = null,
    val notes: String? = null,
) {
    val configuredIdentifierCount: Int get() = identifierRules.size

    fun deviceModel(catalogName: String): String =
        customDeviceName?.takeIf { it.isNotBlank() }
            ?: identifierValues["build_model"]?.takeIf { it.isNotBlank() }
            ?: catalogName

    fun deviceManufacturer(catalogManufacturer: String): String =
        customDeviceManufacturer?.takeIf { it.isNotBlank() }
            ?: identifierValues["build_manufacturer"]?.takeIf { it.isNotBlank() }
            ?: identifierValues["build_brand"]?.takeIf { it.isNotBlank() }
            ?: catalogManufacturer

    fun deviceLabel(catalogName: String, catalogManufacturer: String? = null): String {
        val model = deviceModel(catalogName)
        if (deviceTemplateId != "custom_device") return model
        val manufacturer = catalogManufacturer?.let(::deviceManufacturer)?.takeIf { it.isNotBlank() }
        return when {
            manufacturer == null || manufacturer.equals("Custom", ignoreCase = true) -> model
            model.contains(manufacturer, ignoreCase = true) -> model
            else -> "$manufacturer $model"
        }
    }
}
