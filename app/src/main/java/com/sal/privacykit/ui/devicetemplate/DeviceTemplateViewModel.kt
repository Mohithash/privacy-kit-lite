package com.sal.privacykit.ui.devicetemplate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sal.privacykit.data.model.Profile
import com.sal.privacykit.data.repository.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeviceTemplateViewModel(
    profileId: Long,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    val profile: StateFlow<Profile?> = profileRepository.observeById(profileId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectTemplate(profile: Profile, templateId: String) {
        viewModelScope.launch {
            profileRepository.updateDeviceTemplate(profile, templateId)
        }
    }
}
