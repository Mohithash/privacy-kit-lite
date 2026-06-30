package com.sal.privacykit.lite

import com.sal.privacykit.lite.model.IdentifierRuleType
import com.sal.privacykit.lite.model.LiteAppAssignment
import com.sal.privacykit.lite.model.LiteProfile
import com.sal.privacykit.lite.model.LiteState
import com.sal.privacykit.lite.xposed.XposedConfigExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class XposedConfigExporterTest {
    @Test
    fun exportUsesAssignmentsToRouteProfilesToPackages() {
        val state = LiteState(
            profiles = listOf(
                LiteProfile(
                    id = 5,
                    name = "Social",
                    enabled = true,
                    rules = mapOf("android_id" to IdentifierRuleType.CUSTOM),
                    values = mapOf("android_id" to "custom-id"),
                ),
            ),
            assignments = listOf(
                LiteAppAssignment("social.one", "Social One", profileId = 5),
                LiteAppAssignment("social.two", "Social Two", profileId = 5),
            ),
        )

        val exported = XposedConfigExporter.exportJson(state)

        assertNotNull(exported.optJSONObject("social.one"))
        assertNotNull(exported.optJSONObject("social.two"))
        assertEquals(
            "custom-id",
            exported.getJSONObject("social.one")
                .getJSONObject("identifiers")
                .getJSONObject("android_id")
                .getString("value"),
        )
    }

    @Test
    fun disabledProfilesAndAssignmentsAreNotExported() {
        val state = LiteState(
            profiles = listOf(
                LiteProfile(
                    id = 1,
                    name = "Disabled",
                    enabled = false,
                    rules = mapOf("android_id" to IdentifierRuleType.STATIC),
                    values = mapOf("android_id" to "unused"),
                ),
                LiteProfile(
                    id = 2,
                    name = "Enabled",
                    enabled = true,
                    rules = mapOf("android_id" to IdentifierRuleType.STATIC),
                    values = mapOf("android_id" to "used"),
                ),
            ),
            assignments = listOf(
                LiteAppAssignment("disabled.profile", "Disabled Profile", profileId = 1),
                LiteAppAssignment("disabled.assignment", "Disabled Assignment", profileId = 2, enabled = false),
            ),
        )

        val exported = XposedConfigExporter.exportJson(state)

        assertFalse(exported.has("disabled.profile"))
        assertFalse(exported.has("disabled.assignment"))
    }
}
