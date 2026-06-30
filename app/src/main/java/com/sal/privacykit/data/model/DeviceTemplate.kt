package com.sal.privacykit.data.model

data class DeviceTemplate(
    val id: String,
    val name: String,
    val manufacturer: String,
)

object DeviceTemplateCatalog {
    val templates = listOf(
        DeviceTemplate("pixel_9_pro", "Pixel 9 Pro", "Google"),
        DeviceTemplate("pixel_10_pro", "Pixel 10 Pro", "Google"),
        DeviceTemplate("galaxy_s25_ultra", "Galaxy S25 Ultra", "Samsung"),
        DeviceTemplate("galaxy_s26_ultra", "Galaxy S26 Ultra", "Samsung"),
        DeviceTemplate("oneplus_15", "OnePlus 15", "OnePlus"),
        DeviceTemplate("nothing_phone_4", "Nothing Phone 4", "Nothing"),
        DeviceTemplate("xiaomi_16", "Xiaomi 16", "Xiaomi"),
        DeviceTemplate("rog_phone_9", "ROG Phone 9", "Asus"),
        DeviceTemplate("custom_device", "Custom Device", "Custom"),
    )

    fun byId(id: String): DeviceTemplate = templates.firstOrNull { it.id == id } ?: templates.first()
}
