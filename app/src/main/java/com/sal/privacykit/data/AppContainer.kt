package com.sal.privacykit.data

import android.content.Context
import com.sal.privacykit.data.db.AppDatabase
import com.sal.privacykit.data.prefs.AppPreferences
import com.sal.privacykit.data.repository.AppRepository
import com.sal.privacykit.data.repository.ProfileRepository
import com.sal.privacykit.xposed.XposedServiceConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val database = AppDatabase.get(context)
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val appPreferences = AppPreferences(context)
    val appResolver = AppResolver(context)
    val xposedServiceConnection = XposedServiceConnection()
    val profileRepository = ProfileRepository(database, xposedServiceConnection, appPreferences)
    val appRepository = AppRepository(profileRepository, appResolver)
    val profileLauncher = ProfileLauncher(profileRepository)

    init {
        xposedServiceConnection.onBind = {
            applicationScope.launch { profileRepository.resync() }
        }
        applicationScope.launch {
            profileRepository.backfillMissingStaticValues()
            profileRepository.fixTemplateMismatchedValues()
            profileRepository.observeAll().first()
        }
    }
}
