package com.sal.privacykit.ui.launchoptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sal.privacykit.data.model.Profile
import com.sal.privacykit.data.repository.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LaunchOptionsViewModel(
    profileId: Long,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    val profile: StateFlow<Profile?> = profileRepository.observeById(profileId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setSkipConfirmation(profile: Profile, skip: Boolean) {
        viewModelScope.launch {
            profileRepository.setSkipLaunchConfirmation(profile, skip)
        }
    }
}
