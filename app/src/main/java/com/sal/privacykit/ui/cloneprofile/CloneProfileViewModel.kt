package com.sal.privacykit.ui.cloneprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sal.privacykit.data.model.Profile
import com.sal.privacykit.data.repository.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CloneProfileViewModel(
    private val appId: String,
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    val profiles: StateFlow<List<Profile>> = profileRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clone(source: Profile, onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(profileRepository.cloneProfile(source, appId)) }
    }
}
