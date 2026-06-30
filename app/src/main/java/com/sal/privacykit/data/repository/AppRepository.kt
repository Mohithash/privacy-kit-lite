package com.sal.privacykit.data.repository

import com.sal.privacykit.data.AppResolver
import com.sal.privacykit.data.model.AppInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AppWithProfileCount(
    val app: AppInfo,
    val profileCount: Int,
)

class AppRepository(
    private val profileRepository: ProfileRepository,
    private val appResolver: AppResolver,
) {
    fun observeApps(): Flow<List<AppWithProfileCount>> =
        profileRepository.observeAll().map { profiles ->
            val profileCounts = mutableMapOf<String, Int>()
            for (profile in profiles) {
                if (!profile.archived) {
                    profileCounts[profile.appId] = (profileCounts[profile.appId] ?: 0) + 1
                }
            }
            profileCounts
                .map { (appId, count) -> AppWithProfileCount(appResolver.resolve(appId), count) }
                .sortedBy { it.app.name.lowercase() }
        }
}
