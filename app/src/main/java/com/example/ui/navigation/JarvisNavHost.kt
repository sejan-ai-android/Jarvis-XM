package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.core.di.AppContainer
import com.example.ui.chat.ChatScreen
import com.example.ui.chat.ChatViewModel
import com.example.ui.onboarding.ApiKeyOnboardingScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel

object JarvisDestinations {
    const val ONBOARDING = "onboarding"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
}

@Composable
fun JarvisNavHost(
    container: AppContainer,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val preferences by container.userPreferencesRepository.userPreferencesFlow.collectAsState(
        initial = null
    )

    // Wait until preferences are loaded
    if (preferences == null) return

    val startDestination = if (preferences!!.hasCompletedOnboarding) {
        JarvisDestinations.CHAT
    } else {
        JarvisDestinations.ONBOARDING
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(JarvisDestinations.ONBOARDING) {
            ApiKeyOnboardingScreen(
                keyRepository = container.keyRepository,
                userPreferencesRepository = container.userPreferencesRepository,
                onComplete = {
                    navController.navigate(JarvisDestinations.CHAT) {
                        popUpTo(JarvisDestinations.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(JarvisDestinations.CHAT) {
            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModel.Factory(
                    container.conversationRepository,
                    container.geminiRepository,
                    container.keyRepository,
                    container.userPreferencesRepository,
                    container.voiceManager
                )
            )

            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToSettings = {
                    navController.navigate(JarvisDestinations.SETTINGS)
                }
            )
        }

        composable(JarvisDestinations.SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(
                    container.keyRepository,
                    container.userPreferencesRepository
                )
            )

            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
