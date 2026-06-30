package com.sal.privacykit.lite

import com.sal.privacykit.lite.data.LiteStateCodec
import com.sal.privacykit.lite.model.IdentifierRuleType
import com.sal.privacykit.lite.model.LiteAppAssignment
import com.sal.privacykit.lite.model.LiteProfile
import com.sal.privacykit.lite.model.LiteState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LiteStateCodecTest {
    @Test
    fun roundTripPreservesProfilesAndAssignments() {
        val state = LiteState(
            profiles = listOf(
                LiteProfile(
                    id = 7,
                    name = "Banking",
                    enabled = true,
                    rules = mapOf("android_id" to IdentifierRuleType.STATIC),
                    values = mapOf("android_id" to "abcd1234"),
                ),
            ),
            assignments = listOf(LiteAppAssignment("example.app", "Example", profileId = 7, enabled = true)),
        )

        val decoded = LiteStateCodec.decode(LiteStateCodec.encode(state))

        assertNotNull(decoded)
        assertEquals("Banking", decoded!!.profiles.single().name)
        assertEquals("example.app", decoded.assignments.single().packageName)
        assertEquals(7, decoded.assignments.single().profileId)
    }

    @Test
    fun legacyAppProfilesBecomeReusableProfilesWithAssignments() {
        val legacy = """
            [
              {
                "id": 99,
                "packageName": "legacy.app",
                "label": "Legacy App",
                "enabled": true,
                "rules": {"android_id": "STATIC"},
                "values": {"android_id": "legacy-value"}
              }
            ]
        """.trimIndent()

        val decoded = LiteStateCodec.decodeLegacyProfiles(legacy)

        assertNotNull(decoded)
        assertEquals("Legacy App", decoded!!.profiles.single().name)
        assertEquals("legacy.app", decoded.assignments.single().packageName)
        assertEquals(decoded.profiles.single().id, decoded.assignments.single().profileId)
    }
}
