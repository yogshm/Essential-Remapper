package com.essentialremapper.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.essentialremapper.data.repository.SettingsRepository
import com.essentialremapper.domain.device.DeviceRepository
import com.essentialremapper.ui.home.HomeScreen
import com.essentialremapper.ui.onboarding.OnboardingConfirmationScreen
import com.essentialremapper.ui.onboarding.OnboardingDetectionScreen
import com.essentialremapper.ui.onboarding.OnboardingOverviewScreen
import com.essentialremapper.ui.onboarding.OnboardingWelcomeScreen
import com.essentialremapper.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    settingsRepository: SettingsRepository,
    deviceRepository: DeviceRepository
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val settings by settingsRepository.settings.collectAsState()

    val detectedProfile = deviceRepository.getDetectedProfile()
    val currentProfile = deviceRepository.getProfileById(settings.selectedDeviceId)

    val startDestination = if (settings.isSetupCompleted) {
        Screen.Home.route
    } else {
        Screen.OnboardingWelcome.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.OnboardingWelcome.route) {
            OnboardingWelcomeScreen(
                onNext = { navController.navigate(Screen.OnboardingDetection.route) }
            )
        }

        composable(Screen.OnboardingDetection.route) {
            OnboardingDetectionScreen(
                detectedProfile = detectedProfile,
                onNext = { navController.navigate(Screen.OnboardingConfirmation.route) }
            )
        }

        composable(Screen.OnboardingConfirmation.route) {
            OnboardingConfirmationScreen(
                initialProfile = detectedProfile,
                deviceRepository = deviceRepository,
                onConfirm = { selected ->
                    scope.launch {
                        settingsRepository.setSelectedDevice(selected.id)
                        settingsRepository.setPositions(
                            portrait = selected.defaultPortraitPosition,
                            landscape = selected.defaultLandscapePosition
                        )
                    }
                    navController.navigate(Screen.OnboardingOverview.route)
                }
            )
        }

        composable(Screen.OnboardingOverview.route) {
            OnboardingOverviewScreen(
                onComplete = {
                    scope.launch {
                        settingsRepository.setSetupCompleted(true)
                    }
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.OnboardingWelcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                settings = settings,
                deviceProfile = currentProfile,
                onActionSelected = { gesture, action ->
                    scope.launch {
                        settingsRepository.setAction(gesture, action)
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToDiagnostic = {
                    navController.navigate(Screen.Diagnostic.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                settings = settings,
                deviceProfile = currentProfile,
                onUpdateSettings = { transform ->
                    scope.launch {
                        settingsRepository.updateSettings(transform)
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Diagnostic.route) {
            com.essentialremapper.ui.diagnostic.DiagnosticScreen(
                deviceProfile = currentProfile,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
