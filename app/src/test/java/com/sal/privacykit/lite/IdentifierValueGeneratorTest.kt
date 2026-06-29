package com.sal.privacykit.lite

import com.sal.privacykit.lite.model.IdentifierValueGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentifierValueGeneratorTest {
    @Test
    fun generatedValuesAreDeterministic() {
        assertEquals(
            IdentifierValueGenerator.generate("android_id", 42L),
            IdentifierValueGenerator.generate("android_id", 42L),
        )
    }

    @Test
    fun imeiHasExpectedShape() {
        val imei = IdentifierValueGenerator.generate("imei", 7L)
        assertTrue(imei.matches(Regex("\\d{15}")))
        assertTrue(hasValidLuhnChecksum(imei))
    }

    @Test
    fun generatedMacIsLocallyAdministeredAndUnicast() {
        val firstOctet = IdentifierValueGenerator.generate("wifi_mac", 11L)
            .substringBefore(':')
            .toInt(16)
        assertEquals(0, firstOctet and 0x01)
        assertEquals(0x02, firstOctet and 0x02)
    }

    private fun hasValidLuhnChecksum(value: String): Boolean {
        val sum = value.reversed().mapIndexed { index, char ->
            var digit = char - '0'
            if (index % 2 == 1) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            digit
        }.sum()
        return sum % 10 == 0
    }
}
