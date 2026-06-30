package com.sal.privacykit.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sal.privacykit.PrivacyKitApplication
import com.sal.privacykit.ui.addapp.AddAppScreen
import com.sal.privacykit.ui.apps.AppsScreen
import com.sal.privacykit.ui.cloneprofile.CloneProfileScreen
import com.sal.privacykit.ui.createprofile.CreateProfileScreen
import com.sal.privacykit.ui.customdevice.CustomDeviceScreen
import com.sal.privacykit.ui.devicetemplate.DeviceTemplateScreen
import com.sal.privacykit.ui.identifiers.IdentifierRuleScreen
import com.sal.privacykit.ui.identifiers.IdentifiersScreen
import com.sal.privacykit.ui.launchoptions.LaunchOptionsScreen
import com.sal.privacykit.ui.onboarding.OnboardingScreen
import com.sal.privacykit.ui.profilemode.ProfileModeScreen
import com.sal.privacykit.ui.profiles.AppProfilesScreen
import com.sal.privacykit.ui.profiles.ProfileDetailsScreen
import com.sal.privacykit.ui.profilestab.ProfilesTabScreen
import com.sal.privacykit.ui.settings.AppearanceScreen
import com.sal.privacykit.ui.settings.CustomHooksScreen
import com.sal.privacykit.ui.settings.DeveloperSettingsScreen
import com.sal.privacykit.ui.settings.SettingsScreen
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.APPS, "Apps", Icons.Filled.Apps),
    BottomTab(Routes.PROFILES, "Profiles", Icons.Filled.Person),
    BottomTab(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

@Composable
fun PrivacyKitNavHost(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val application = context.applicationContext as PrivacyKitApplication
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val hasSeenOnboarding by application.container.appPreferences.hasSeenOnboarding.collectAsState(initial = null)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull { it.route in TOP_LEVEL_ROUTES }?.route
    val seenOnboarding = hasSeenOnboarding ?: return
    val startDestination = if (seenOnboarding) Routes.APPS else Routes.ONBOARDING

    Scaffold(
        bottomBar = {
            if (currentRoute in TOP_LEVEL_ROUTES) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = tab.icon,
                            label = tab.label,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onContinue = {
                        scope.launch {
                            application.container.appPreferences.setHasSeenOnboarding(true)
                            navController.navigate(Routes.APPS) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                        }
                    },
                )
            }
            composable(Routes.APPS) {
                AppsScreen(
                    onAppClick = { appId -> navController.navigate(Routes.appProfiles(appId)) },
                    onAddApp = { navController.navigate(Routes.ADD_APP) },
                )
            }
            composable(Routes.PROFILES) {
                ProfilesTabScreen(
                    onGroupClick = { profileId -> navController.navigate(Routes.profileDetails(profileId)) },
                    onCreateProfile = { navController.navigate(Routes.ADD_APP) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onAppearanceClick = { navController.navigate(Routes.APPEARANCE) },
                    onDeveloperSettingsClick = { navController.navigate(Routes.DEVELOPER_SETTINGS) },
                )
            }
            composable(Routes.APPEARANCE) {
                AppearanceScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.DEVELOPER_SETTINGS) {
                DeveloperSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onCustomHooksClick = { navController.navigate(Routes.CUSTOM_HOOKS) },
                )
            }
            composable(Routes.CUSTOM_HOOKS) {
                CustomHooksScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ADD_APP) {
                AddAppScreen(
                    onBack = { navController.popBackStack() },
                    onAppSelected = { packageName ->
                        navController.popBackStack()
                        navController.navigate(Routes.createProfile(packageName))
                    },
                )
            }
            composable(
                route = Routes.APP_PROFILES,
                arguments = listOf(navArgument("appId") { type = NavType.StringType }),
            ) { entry ->
                val appId = entry.arguments?.getString("appId").orEmpty()
                AppProfilesScreen(
                    appId = appId,
                    onBack = { navController.popBackStack() },
                    onProfileClick = { profileId -> navController.navigate(Routes.profileDetails(profileId)) },
                    onCreateProfile = { id -> navController.navigate(Routes.createProfile(id)) },
                )
            }
            composable(
                route = Routes.PROFILE_DETAILS,
                arguments = listOf(navArgument("profileId") { type = NavType.LongType }),
            ) { entry ->
                val profileId = entry.arguments?.getLong("profileId") ?: return@composable
                ProfileDetailsScreen(
                    profileId = profileId,
                    onBack = { navController.popBackStack() },
                    onDeviceTemplateClick = { id -> navController.navigate(Routes.deviceTemplate(id)) },
                    onIdentifiersClick = { id -> navController.navigate(Routes.identifiers(id)) },
                    onProfileModeClick = { id -> navController.navigate(Routes.profileMode(id)) },
                    onAppsUsingProfileClick = { appId -> navController.navigate(Routes.appProfiles(appId)) },
                    onLaunchOptionsClick = { id -> navController.navigate(Routes.launchOptions(id)) },
                )
            }
            composable(
                route = Routes.PROFILE_MODE,
                arguments = listOf(navArgument("profileId") { type = NavType.LongType }),
            ) { entry ->
                val profileId = entry.arguments?.getLong("profileId") ?: return@composable
                ProfileModeScreen(profileId = profileId, onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.LAUNCH_OPTIONS,
                arguments = listOf(navArgument("profileId") { type = NavType.LongType }),
            ) { entry ->
                val profileId = entry.arguments?.getLong("profileId") ?: return@composable
                LaunchOptionsScreen(profileId = profileId, onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.DEVICE_TEMPLATE,
                arguments = listOf(navArgument("profileId") { type = NavType.LongType }),
            ) { entry ->
                val profileId = entry.arguments?.getLong("profileId") ?: return@composable
                DeviceTemplateScreen(profileId = profileId, onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.IDENTIFIERS,
                arguments = listOf(navArgument("profileId") { type = NavType.LongType }),
            ) { entry ->
                val profileId = entry.arguments?.getLong("profileId") ?: return@composable
                IdentifiersScreen(
                    profileId = profileId,
                    onBack = { navController.popBackStack() },
                    onIdentifierClick = { id, key -> navController.navigate(Routes.identifierRule(id, key)) },
                )
            }
            composable(
                route = Routes.IDENTIFIER_RULE,
                arguments = listOf(
                    navArgument("profileId") { type = NavType.LongType },
                    navArgument("identifierKey") { type = NavType.StringType },
                ),
            ) { entry ->
                val profileId = entry.arguments?.getLong("profileId") ?: return@composable
                val identifierKey = entry.arguments?.getString("identifierKey") ?: return@composable
                IdentifierRuleScreen(profileId = profileId, identifierKey = identifierKey, onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.CREATE_PROFILE,
                arguments = listOf(navArgument("appId") { type = NavType.StringType }),
            ) { entry ->
                val appId = entry.arguments?.getString("appId").orEmpty()
                CreateProfileScreen(
                    appId = appId,
                    onBack = { navController.popBackStack() },
                    onProfileCreated = { profileId ->
                        navController.popBackStack()
                        navController.navigate(Routes.profileDetails(profileId))
                    },
                    onCloneExisting = { id -> navController.navigate(Routes.cloneProfile(id)) },
                    onCustomDevice = { id -> navController.navigate(Routes.customDevice(id)) },
                )
            }
            composable(
                route = Routes.CLONE_PROFILE,
                arguments = listOf(navArgument("appId") { type = NavType.StringType }),
            ) { entry ->
                val appId = entry.arguments?.getString("appId").orEmpty()
                CloneProfileScreen(
                    appId = appId,
                    onBack = { navController.popBackStack() },
                    onCloned = { profileId ->
                        navController.popBackStack()
                        navController.popBackStack()
                        navController.navigate(Routes.profileDetails(profileId))
                    },
                )
            }
            composable(
                route = Routes.CUSTOM_DEVICE,
                arguments = listOf(navArgument("appId") { type = NavType.StringType }),
            ) { entry ->
                val appId = entry.arguments?.getString("appId").orEmpty()
                CustomDeviceScreen(
                    appId = appId,
                    onBack = { navController.popBackStack() },
                    onCreated = { profileId ->
                        navController.popBackStack()
                        navController.popBackStack()
                        navController.navigate(Routes.profileDetails(profileId))
                    },
                )
            }
        }
    }
}
