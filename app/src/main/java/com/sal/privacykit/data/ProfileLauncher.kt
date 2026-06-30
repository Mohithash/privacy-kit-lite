package com.sal.privacykit.data

import android.content.Context
import com.sal.privacykit.data.model.Profile
import com.sal.privacykit.data.repository.ProfileRepository

class ProfileLauncher(
    private val profileRepository: ProfileRepository,
) {
    suspend fun launch(profile: Profile, context: Context): String? {
        profileRepository.setActiveProfile(profile)
        val intent = context.packageManager.getLaunchIntentForPackage(profile.appId)
            ?: return "Couldn't resolve a launch intent for ${profile.appId}"
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return null
    }
}
