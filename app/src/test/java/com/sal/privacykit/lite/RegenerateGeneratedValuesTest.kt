package com.sal.privacykit.lite

import com.sal.privacykit.lite.data.regenerateGeneratedValues
import com.sal.privacykit.lite.model.IdentifierRuleType
import com.sal.privacykit.lite.model.LiteProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RegenerateGeneratedValuesTest {
    @Test
    fun regenerationChangesOnlyGeneratedValues() {
        val profile = LiteProfile(
            id = 42,
            name = "Example",
            enabled = true,
            rules = mapOf(
                "android_id" to IdentifierRuleType.STATIC,
                "advertising_id" to IdentifierRuleType.CUSTOM,
                "wifi_ssid" to IdentifierRuleType.REAL,
            ),
            values = mapOf(
                "android_id" to "old-generated-value",
                "advertising_id" to "keep-custom-value",
                "wifi_ssid" to "keep-real-value",
            ),
        )

        val regenerated = regenerateGeneratedValues(profile, nonce = 100)

        assertNotEquals("old-generated-value", regenerated["android_id"])
        assertEquals("keep-custom-value", regenerated["advertising_id"])
        assertEquals("keep-real-value", regenerated["wifi_ssid"])
    }
}
