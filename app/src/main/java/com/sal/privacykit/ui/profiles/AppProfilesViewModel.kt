package com.sal.privacykit.ui.profiles

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sal.privacykit.data.ProfileLauncher
import com.sal.privacykit.data.model.Profile
import com.sal.privacykit.data.model.ProfileTagColor
import com.sal.privacykit.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppProfilesViewModel(
    private val appId: String,
    private val profileRepository: ProfileRepository,
    private val profileLauncher: ProfileLauncher,
) : ViewModel() {
    private val allProfiles: StateFlow<List<Profile>> = profileRepository.observeForApp(appId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived

    val profiles: StateFlow<List<Profile>> = combine(allProfiles, _showArchived) { all, showArchived ->
        if (showArchived) all else all.filterNot { it.archived }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleShowArchived() {
        _showArchived.value = !_showArchived.value
    }

    fun launch(profile: Profile, context: Context, onError: (String) -> Unit = {}) {
        viewModelScope.launch { profileLauncher.launch(profile, context)?.let(onError) }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch { profileRepository.delete(profile) }
    }

    fun duplicateProfile(profile: Profile, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch { onCreated(profileRepository.cloneProfile(profile, profile.appId)) }
    }

    fun toggleArchived(profile: Profile) {
        viewModelScope.launch { profileRepository.setArchived(profile, !profile.archived) }
    }

    fun cycleTagColor(profile: Profile) {
        viewModelScope.launch {
            val next = when (profile.tagColor) {
                null -> ProfileTagColor.RED
                ProfileTagColor.RED -> ProfileTagColor.BLUE
                ProfileTagColor.BLUE -> ProfileTagColor.GREEN
                ProfileTagColor.GREEN -> null
            }
            profileRepository.setTagColor(profile, next)
        }
    }

    fun setNotes(profile: Profile, notes: String?) {
        viewModelScope.launch { profileRepository.setNotes(profile, notes) }
    }
}
