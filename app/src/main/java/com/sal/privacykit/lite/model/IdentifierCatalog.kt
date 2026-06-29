package com.sal.privacykit.lite.model

data class IdentifierItem(
    val key: String,
    val label: String,
    val defaultEnabled: Boolean = true,
)

object IdentifierCatalog {
    val items = listOf(
        IdentifierItem("android_id", "Android ID"),
        IdentifierItem("advertising_id", "Advertising ID"),
        IdentifierItem("media_drm_id", "Media DRM ID"),
        IdentifierItem("build_serial", "Build serial"),
        IdentifierItem("build_model", "Build model"),
        IdentifierItem("build_brand", "Build brand"),
        IdentifierItem("build_manufacturer", "Build manufacturer"),
        IdentifierItem("build_fingerprint", "Build fingerprint"),
        IdentifierItem("build_board", "Build board"),
        IdentifierItem("build_device", "Build device"),
        IdentifierItem("build_product", "Build product"),
        IdentifierItem("build_hardware", "Build hardware"),
        IdentifierItem("build_id", "Build ID"),
        IdentifierItem("build_type", "Build type"),
        IdentifierItem("build_tags", "Build tags"),
        IdentifierItem("build_display", "Build display"),
        IdentifierItem("build_bootloader", "Build bootloader"),
        IdentifierItem("build_radio_version", "Build radio version"),
        IdentifierItem("wifi_mac", "Wi-Fi MAC"),
        IdentifierItem("wifi_bssid", "Wi-Fi BSSID"),
        IdentifierItem("wifi_ssid", "Wi-Fi SSID", defaultEnabled = false),
        IdentifierItem("bluetooth_mac", "Bluetooth MAC"),
        IdentifierItem("hostname", "Hostname"),
        IdentifierItem("imei", "IMEI"),
        IdentifierItem("meid", "MEID"),
        IdentifierItem("subscriber_id", "Subscriber ID"),
        IdentifierItem("iccid", "ICCID"),
        IdentifierItem("phone_number", "Phone number"),
    )

    val keys: Set<String> = items.map { it.key }.toSet()
}
