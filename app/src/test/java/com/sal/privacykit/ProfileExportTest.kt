package com.sal.privacykit

import com.sal.privacykit.data.model.IdentifierRuleType
import com.sal.privacykit.data.model.Profile
import com.sal.privacykit.data.model.ProfileMode
import com.sal.privacykit.data.repository.selectActiveOrFallback
import com.sal.privacykit.xposed.XposedConfigExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileExportTest {
    @Test
    fun activeProfileSelectionPrefersActiveNonArchivedProfile() {
        val first = profile(id = 1, name = "First")
        val second = profile(id = 2, name = "Second")

        assertEquals(second, selectActiveOrFallback(listOf(first, second), activeId = 2))
    }

    @Test
    fun activeProfileSelectionFallsBackPastArchivedProfiles() {
        val archived = profile(id = 1, name = "Archived", archived = true)
        val active = profile(id = 2, name = "Active")

        assertEquals(active, selectActiveOrFallback(listOf(archived, active), activeId = 1))
    }

    @Test
    fun xposedProfileExportPersistsProfileIdentityAndAllowedIdentifiers() {
        val exported = XposedConfigExporter.profileToJson(
            profile(
                id = 5,
                name = "Social",
                rules = mapOf(
                    "android_id" to IdentifierRuleType.CUSTOM,
                    "advertising_id" to IdentifierRuleType.STATIC,
                    "firebase_installation_id" to IdentifierRuleType.CUSTOM,
                ),
                values = mapOf(
                    "android_id" to "custom-id",
                    "advertising_id" to "00000000-0000-4000-8000-000000000000",
                    "firebase_installation_id" to "removed-id",
                ),
            ),
        )

        assertEquals(5L, exported.getLong("profileId"))
        assertEquals(ProfileMode.ISOLATED.name, exported.getString("profileMode"))
        val identifiers = exported.getJSONObject("identifiers")
        assertEquals("custom-id", identifiers.getJSONObject("android_id").getString("value"))
        assertFalse(identifiers.has("firebase_installation_id"))
        assertTrue(identifiers.has("advertising_id"))
    }

    private fun profile(
        id: Long,
        name: String,
        archived: Boolean = false,
        rules: Map<String, IdentifierRuleType> = mapOf("advertising_id" to IdentifierRuleType.STATIC),
        values: Map<String, String> = mapOf("advertising_id" to "00000000-0000-4000-8000-000000000000"),
    ) = Profile(
        id = id,
        name = name,
        appId = "example.app",
        deviceTemplateId = "pixel_9_pro",
        profileMode = ProfileMode.ISOLATED,
        identifierRules = rules,
        identifierValues = values,
        createdAt = 1_700_000_000_000,
        archived = archived,
    )
}
