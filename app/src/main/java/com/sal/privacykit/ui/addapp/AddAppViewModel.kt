package com.sal.privacykit.ui.addapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sal.privacykit.data.AppResolver
import com.sal.privacykit.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddAppViewModel(appResolver: AppResolver) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val isLoading = MutableStateFlow(true)

    val apps: StateFlow<List<AppInfo>> = combine(_allApps, _query) { apps, query ->
        if (query.isBlank()) apps else apps.filter { it.name.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val resolved = withContext(Dispatchers.IO) { appResolver.listLaunchableApps() }
            _allApps.value = resolved
            isLoading.value = false
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }
}
