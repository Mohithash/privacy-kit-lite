package com.sal.privacykit.ui.createprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sal.privacykit.data.repository.ProfileRepository
import kotlinx.coroutines.launch

class CreateProfileViewModel(
    private val appId: String,
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    fun createFromTemplate(templateId: String, templateName: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            onCreated(
                profileRepository.createProfile(
                    name = templateName,
                    appId = appId,
                    deviceTemplateId = templateId,
                ),
            )
        }
    }
}
