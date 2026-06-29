package com.sal.privacykit.lite.model

import java.util.Random

object IdentifierValueGenerator {
    fun generate(key: String, seed: Long): String {
        val random = Random(seed)
        return when (key) {
            "android_id" -> hex(random, 8)
            "advertising_id" -> uuid(random)
            "media_drm_id" -> hex(random, 16)
            "build_serial" -> serial(random)
            "build_model" -> models[random.nextInt(models.size)]
            "build_brand", "build_manufacturer" -> brands[random.nextInt(brands.size)]
            "build_fingerprint" -> "generic/sdk_phone64/sdk_phone64:14/UE1A.${230100 + random.nextInt(9000)}.${100 + random.nextInt(900)}/${1000000 + random.nextInt(9000000)}:user/release-keys"
            "build_board" -> "board_${hex(random, 3)}"
            "build_device" -> "device_${hex(random, 3)}"
            "build_product" -> "product_${hex(random, 3)}"
            "build_hardware" -> "qcom_${hex(random, 3)}"
            "build_id" -> "UE1A.${230100 + random.nextInt(9000)}.${100 + random.nextInt(900)}"
            "build_type" -> "user"
            "build_tags" -> "release-keys"
            "build_display" -> "UE1A.${230100 + random.nextInt(9000)}.${100 + random.nextInt(900)}"
            "build_bootloader" -> hex(random, 4)
            "build_radio_version" -> "${1 + random.nextInt(9)}.${random.nextInt(100)}.${random.nextInt(10)}.${random.nextInt(100)}"
            "wifi_mac", "wifi_bssid", "bluetooth_mac" -> mac(random)
            "wifi_ssid" -> "Network-${hex(random, 3)}"
            "hostname" -> "android-${hex(random, 8)}"
            "imei" -> imei(random)
            "meid" -> hex(random, 7).uppercase()
            "subscriber_id" -> digits(random, 15)
            "iccid" -> digits(random, 19)
            "phone_number" -> "+1555${digits(random, 7)}"
            else -> hex(random, 8)
        }
    }

    fun seedFor(profileId: Long, key: String): Long = profileId * 31 + key.hashCode()

    private fun hex(random: Random, bytes: Int): String {
        val data = ByteArray(bytes)
        random.nextBytes(data)
        return data.joinToString("") { "%02x".format(it) }
    }

    private fun uuid(random: Random): String {
        val data = ByteArray(16)
        random.nextBytes(data)
        data[6] = ((data[6].toInt() and 0x0f) or 0x40).toByte()
        data[8] = ((data[8].toInt() and 0x3f) or 0x80).toByte()
        val text = data.joinToString("") { "%02x".format(it) }
        return "${text.substring(0, 8)}-${text.substring(8, 12)}-${text.substring(12, 16)}-${text.substring(16, 20)}-${text.substring(20, 32)}"
    }

    private fun mac(random: Random): String {
        val data = ByteArray(6)
        random.nextBytes(data)
        data[0] = (data[0].toInt() and 0xfe or 0x02).toByte()
        return data.joinToString(":") { "%02X".format(it) }
    }

    private fun digits(random: Random, count: Int): String =
        (1..count).map { ('0'.code + random.nextInt(10)).toChar() }.joinToString("")

    private fun imei(random: Random): String {
        val body = digits(random, 14).map { it - '0' }
        var sum = 0
        body.reversed().forEachIndexed { index, digit ->
            var value = digit
            if (index % 2 == 0) {
                value *= 2
                if (value > 9) value -= 9
            }
            sum += value
        }
        return body.joinToString("") + ((10 - sum % 10) % 10)
    }

    private fun serial(random: Random): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..11).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }

    private val brands = listOf("Google", "Samsung", "OnePlus", "Nothing", "Asus")
    private val models = listOf("Pixel 9 Pro", "Galaxy S25", "OnePlus 15", "Nothing Phone 4", "ROG Phone 9")
}
