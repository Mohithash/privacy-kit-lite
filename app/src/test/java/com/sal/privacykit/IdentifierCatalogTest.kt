package com.sal.privacykit

import com.sal.privacykit.data.model.IdentifierCatalog
import com.sal.privacykit.data.model.IdentifierRuleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentifierCatalogTest {
    @Test
    fun liteCatalogKeepsOnlyFiveCoreGroups() {
        assertEquals(
            listOf(
                "Android ID / SSAID",
                "Advertising ID",
                "Build / Device Identity",
                "Wi-Fi Identifiers",
                "Telephony Identity",
            ),
            IdentifierCatalog.groups.map { it.title },
        )
        assertEquals(5, IdentifierCatalog.groups.size)
    }

    @Test
    fun liteCatalogExcludesRemovedIdentifierFamilies() {
        val forbiddenKeys = setOf(
            "firebase_installation_id",
            "app_set_id",
            "gsf_id",
            "media_drm_id",
            "bluetooth_mac",
            "hostname",
            "location",
        )

        assertTrue(forbiddenKeys.none { it in IdentifierCatalog.allowedKeys })
        assertFalse(IdentifierCatalog.allowedKeys.isEmpty())
    }

    @Test
    fun identifierRulesOnlyExposeRealStaticAndCustom() {
        assertEquals(
            listOf(IdentifierRuleType.REAL, IdentifierRuleType.STATIC, IdentifierRuleType.CUSTOM),
            IdentifierRuleType.entries.toList(),
        )
        assertEquals(IdentifierRuleType.STATIC, IdentifierCatalog.itemByKey("advertising_id").defaultRule)
    }
}
