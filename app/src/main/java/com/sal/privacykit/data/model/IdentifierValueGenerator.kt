package com.sal.privacykit.data.model

import java.util.Random

object IdentifierValueGenerator {
    fun generate(key: String, seed: Long): String {
        val random = Random(seed)
        return when (key) {
            "android_id" -> hex(random, 16)
            "advertising_id" -> uuid(random)
            "build_model" -> randomDeviceModel(random)
            "build_brand", "build_manufacturer" -> randomBrand(random)
            "build_device" -> "device_${hex(random, 3)}"
            "build_product" -> "product_${hex(random, 3)}"
            "wifi_mac", "wifi_bssid" -> randomMac(random)
            "wifi_ssid" -> "Network-${hex(random, 3)}"
            "imei" -> randomImei(random)
            "subscriber_id" -> randomDigits(random, 15)
            "iccid" -> randomDigits(random, 19)
            "phone_number" -> "+1555${randomDigits(random, 7)}"
            "network_operator", "sim_operator" -> randomOperator(random)
            else -> hex(random, 16)
        }
    }

    fun seedFor(profileId: Long, key: String): Long = profileId * 31 + key.hashCode()

    private fun hex(random: Random, bytes: Int): String {
        val buf = ByteArray(bytes)
        random.nextBytes(buf)
        return buf.joinToString("") { "%02x".format(it) }
    }

    private fun uuid(random: Random): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()
        val value = bytes.joinToString("") { "%02x".format(it) }
        return "${value.substring(0, 8)}-${value.substring(8, 12)}-${value.substring(12, 16)}-${value.substring(16, 20)}-${value.substring(20, 32)}"
    }

    private fun randomMac(random: Random): String {
        val bytes = ByteArray(6)
        random.nextBytes(bytes)
        bytes[0] = (bytes[0].toInt() and 0xFE or 0x02).toByte()
        return bytes.joinToString(":") { "%02X".format(it) }
    }

    private fun randomDigits(random: Random, count: Int): String =
        (1..count).map { ('0' + random.nextInt(10)) }.joinToString("")

    private fun randomImei(random: Random): String {
        val digits = randomDigits(random, 14).map { it - '0' }
        var sum = 0
        digits.reversed().forEachIndexed { index, digit ->
            var d = digit
            if (index % 2 == 0) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
        }
        val checkDigit = (10 - sum % 10) % 10
        return digits.joinToString("") + checkDigit
    }

    private fun randomOperator(random: Random): String = operators[random.nextInt(operators.size)]

    private val operators = listOf("310260", "310410", "311480", "302720", "40445")
    private val brands = listOf("Google", "Samsung", "OnePlus", "Xiaomi", "Nothing", "Asus")
    private val models = listOf("Pixel 9 Pro", "Galaxy S25 Ultra", "OnePlus 15", "Xiaomi 16", "Nothing Phone 4", "ROG Phone 9")

    private fun randomBrand(random: Random): String = brands[random.nextInt(brands.size)]
    private fun randomDeviceModel(random: Random): String = models[random.nextInt(models.size)]
}
