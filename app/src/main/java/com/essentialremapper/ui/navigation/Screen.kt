package com.essentialremapper.ui.navigation

sealed class Screen(val route: String) {
    data object OnboardingWelcome : Screen("onboarding_welcome")
    data object OnboardingDetection : Screen("onboarding_detection")
    data object OnboardingConfirmation : Screen("onboarding_confirmation")
    data object OnboardingOverview : Screen("onboarding_overview")
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object Diagnostic : Screen("diagnostic")
}
