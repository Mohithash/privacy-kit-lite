package com.sal.privacykit.ui.identifiers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sal.privacykit.data.model.Profile
import com.sal.privacykit.data.repository.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class IdentifiersViewModel(
    profileId: Long,
    profileRepository: ProfileRepository,
) : ViewModel() {

    val profile: StateFlow<Profile?> = profileRepository.observeById(profileId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
