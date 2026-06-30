package com.sal.privacykit.data.model

enum class IdentifierRuleType(val label: String) {
    REAL("Real"),
    STATIC("Static"),
    CUSTOM("Custom Value"),
}

data class IdentifierItem(
    val key: String,
    val label: String,
    val defaultRule: IdentifierRuleType,
)

data class IdentifierGroup(
    val title: String,
    val items: List<IdentifierItem>,
)

object IdentifierCatalog {
    val groups = listOf(
        IdentifierGroup(
            title = "Android ID / SSAID",
            items = listOf(
                IdentifierItem("android_id", "Android ID", IdentifierRuleType.STATIC),
            ),
        ),
        IdentifierGroup(
            title = "Advertising ID",
            items = listOf(
                IdentifierItem("advertising_id", "Advertising ID", IdentifierRuleType.STATIC),
            ),
        ),
        IdentifierGroup(
            title = "Build / Device Identity",
            items = listOf(
                IdentifierItem("build_model", "Build Model", IdentifierRuleType.STATIC),
                IdentifierItem("build_brand", "Build Brand", IdentifierRuleType.STATIC),
                IdentifierItem("build_manufacturer", "Build Manufacturer", IdentifierRuleType.STATIC),
                IdentifierItem("build_device", "Build Device", IdentifierRuleType.STATIC),
                IdentifierItem("build_product", "Build Product", IdentifierRuleType.STATIC),
            ),
        ),
        IdentifierGroup(
            title = "Wi-Fi Identifiers",
            items = listOf(
                IdentifierItem("wifi_ssid", "WiFi SSID", IdentifierRuleType.REAL),
                IdentifierItem("wifi_bssid", "WiFi BSSID", IdentifierRuleType.STATIC),
                IdentifierItem("wifi_mac", "WiFi MAC", IdentifierRuleType.STATIC),
            ),
        ),
        IdentifierGroup(
            title = "Telephony Identity",
            items = listOf(
                IdentifierItem("imei", "IMEI / Device ID", IdentifierRuleType.STATIC),
                IdentifierItem("subscriber_id", "Subscriber ID", IdentifierRuleType.STATIC),
                IdentifierItem("iccid", "SIM Serial / ICCID", IdentifierRuleType.STATIC),
                IdentifierItem("phone_number", "Phone Number", IdentifierRuleType.STATIC),
                IdentifierItem("network_operator", "Network Operator", IdentifierRuleType.STATIC),
                IdentifierItem("sim_operator", "SIM Operator", IdentifierRuleType.STATIC),
            ),
        ),
    )

    val allItems = groups.flatMap { it.items }
    val allowedKeys = allItems.map { it.key }.toSet()

    fun itemByKey(key: String): IdentifierItem = allItems.firstOrNull { it.key == key } ?: allItems.first()
}
