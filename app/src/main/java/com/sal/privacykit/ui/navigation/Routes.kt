package com.sal.privacykit.ui.navigation

object Routes {
    const val APPS = "apps"
    const val PROFILES = "profiles"
    const val SETTINGS = "settings"
    const val APPEARANCE = "appearance"
    const val ADD_APP = "add_app"
    const val ONBOARDING = "onboarding"
    const val DEVELOPER_SETTINGS = "developer_settings"
    const val CUSTOM_HOOKS = "custom_hooks"

    const val APP_PROFILES = "app_profiles/{appId}"
    const val PROFILE_DETAILS = "profile_details/{profileId}"
    const val DEVICE_TEMPLATE = "device_template/{profileId}"
    const val IDENTIFIERS = "identifiers/{profileId}"
    const val IDENTIFIER_RULE = "identifier_rule/{profileId}/{identifierKey}"
    const val CREATE_PROFILE = "create_profile/{appId}"
    const val PROFILE_MODE = "profile_mode/{profileId}"
    const val CLONE_PROFILE = "clone_profile/{appId}"
    const val CUSTOM_DEVICE = "custom_device/{appId}"
    const val LAUNCH_OPTIONS = "launch_options/{profileId}"

    fun appProfiles(appId: String) = "app_profiles/$appId"
    fun profileDetails(profileId: Long) = "profile_details/$profileId"
    fun deviceTemplate(profileId: Long) = "device_template/$profileId"
    fun identifiers(profileId: Long) = "identifiers/$profileId"
    fun identifierRule(profileId: Long, identifierKey: String) = "identifier_rule/$profileId/$identifierKey"
    fun createProfile(appId: String) = "create_profile/$appId"
    fun profileMode(profileId: Long) = "profile_mode/$profileId"
    fun cloneProfile(appId: String) = "clone_profile/$appId"
    fun customDevice(appId: String) = "custom_device/$appId"
    fun launchOptions(profileId: Long) = "launch_options/$profileId"
}

val TOP_LEVEL_ROUTES = setOf(Routes.APPS, Routes.PROFILES, Routes.SETTINGS)
