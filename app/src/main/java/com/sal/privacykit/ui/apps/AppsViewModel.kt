package com.sal.privacykit.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sal.privacykit.data.repository.AppRepository
import com.sal.privacykit.data.repository.AppWithProfileCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class AppsViewModel(appRepository: AppRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val apps: StateFlow<List<AppWithProfileCount>> = combine(
        appRepository.observeApps(),
        _query,
    ) { apps, query ->
        if (query.isBlank()) {
            apps
        } else {
            apps.filter { it.app.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(value: String) {
        _query.value = value
    }
}
