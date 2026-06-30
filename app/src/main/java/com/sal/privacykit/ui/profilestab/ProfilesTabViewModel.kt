package com.sal.privacykit.ui.profilestab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sal.privacykit.data.model.DeviceTemplateCatalog
import com.sal.privacykit.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TemplateGroup(
    val groupKey: String,
    val templateName: String,
    val profileCount: Int,
    val representativeProfileId: Long,
)

class ProfilesTabViewModel(profileRepository: ProfileRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val groups: StateFlow<List<TemplateGroup>> = combine(
        profileRepository.observeAll(),
        _query,
    ) { profiles, query ->
        profiles
            .filterNot { it.archived }
            .groupBy { profile ->
                val template = DeviceTemplateCatalog.byId(profile.deviceTemplateId)
                profile.deviceLabel(template.name, template.manufacturer)
            }
            .map { (deviceLabel, group) ->
                TemplateGroup(
                    groupKey = deviceLabel,
                    templateName = deviceLabel,
                    profileCount = group.size,
                    representativeProfileId = group.first().id,
                )
            }
            .filter { it.templateName.contains(query, ignoreCase = true) }
            .sortedByDescending { it.profileCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(value: String) {
        _query.value = value
    }
}
