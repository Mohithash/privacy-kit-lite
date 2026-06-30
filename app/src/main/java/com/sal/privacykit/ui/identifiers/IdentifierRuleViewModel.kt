package com.sal.privacykit.ui.identifiers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sal.privacykit.data.model.IdentifierRuleType
import com.sal.privacykit.data.model.Profile
import com.sal.privacykit.data.repository.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IdentifierRuleViewModel(
    profileId: Long,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    val profile: StateFlow<Profile?> = profileRepository.observeById(profileId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectRule(profile: Profile, key: String, rule: IdentifierRuleType) {
        viewModelScope.launch {
            profileRepository.updateIdentifierRule(profile, key, rule)
        }
    }

    fun setCustomValue(profile: Profile, key: String, value: String) {
        viewModelScope.launch {
            profileRepository.updateIdentifierValue(profile, key, value)
        }
    }
}
