package com.sal.privacykit.lite

import com.sal.privacykit.lite.model.IdentifierRuleType
import com.sal.privacykit.lite.model.LiteProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class LiteProfileTest {
    @Test
    fun enabledCountExcludesRealAndBlankValues() {
        val profile = LiteProfile(
            id = 1,
            name = "Example",
            enabled = true,
            rules = mapOf(
                "real" to IdentifierRuleType.REAL,
                "blank" to IdentifierRuleType.CUSTOM,
                "spoofed" to IdentifierRuleType.STATIC,
            ),
            values = mapOf(
                "real" to "real-value",
                "blank" to "",
                "spoofed" to "generated-value",
            ),
        )

        assertEquals(1, profile.enabledCount())
    }
}
