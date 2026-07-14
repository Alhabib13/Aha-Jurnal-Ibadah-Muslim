package com.islami.Aha.app

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import com.islami.Aha.ui.addhabit.AddHabitScreen
import com.islami.Aha.ui.auth.LoginScreen
import com.islami.Aha.ui.auth.RegisterScreen
import com.islami.Aha.ui.home.HomeScreen
import com.islami.Aha.ui.notification.NotificationScreen
import com.islami.Aha.ui.profile.ProfileScreen
import com.islami.Aha.ui.settings.LegalDocumentActivity
import com.islami.Aha.ui.settings.SettingsScreen
import com.islami.Aha.ui.splash.SplashScreen
import com.islami.Aha.ui.statistic.StatisticScreen

private const val TRANSIENT_SNACKBAR_KEY = "transient_snackbar"

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Statistic : Screen("statistic")
    object AddHabit : Screen("add_habit")
    object Notification : Screen("notification")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
}

@Composable
fun AhaNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(500)) },
        exitTransition = { fadeOut(animationSpec = tween(500)) },
        popEnterTransition = { fadeIn(animationSpec = tween(500)) },
        popExitTransition = { fadeOut(animationSpec = tween(500)) }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            com.islami.Aha.ui.onboarding.OnboardingScreen(
                onFinish = {
                    com.islami.Aha.util.UserPreferencesManager.setHasSeenOnboarding()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onNavigateToSettings = {
                    com.islami.Aha.util.UserPreferencesManager.setHasSeenOnboarding()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Login.route) { backStackEntry ->
            val context = LocalContext.current
            val snackbarMessage by backStackEntry.savedStateHandle
                .getStateFlow<String?>(TRANSIENT_SNACKBAR_KEY, null)
                .collectAsStateWithLifecycle()
            LoginScreen(
                transientSnackbarMessage = snackbarMessage,
                onTransientSnackbarShown = {
                    backStackEntry.savedStateHandle.remove<String>(TRANSIENT_SNACKBAR_KEY)
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        TRANSIENT_SNACKBAR_KEY,
                        context.getString(com.islami.Aha.R.string.auth_login_success_snackbar)
                    )
                }
            )
        }

        composable(Screen.Register.route) {
            val context = LocalContext.current
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        TRANSIENT_SNACKBAR_KEY,
                        context.getString(com.islami.Aha.R.string.auth_register_success_snackbar)
                    )
                },
                onPrivacyPolicyClick = {
                    context.startActivity(
                        Intent(context, LegalDocumentActivity::class.java).apply {
                            putExtra(LegalDocumentActivity.EXTRA_DOCUMENT_TYPE, LegalDocumentActivity.DOC_PRIVACY)
                        }
                    )
                },
                onTermsClick = {
                    context.startActivity(
                        Intent(context, LegalDocumentActivity::class.java).apply {
                            putExtra(LegalDocumentActivity.EXTRA_DOCUMENT_TYPE, LegalDocumentActivity.DOC_TERMS)
                        }
                    )
                }
            )
        }

        composable(Screen.Home.route) { backStackEntry ->
            val snackbarMessage by backStackEntry.savedStateHandle
                .getStateFlow<String?>(TRANSIENT_SNACKBAR_KEY, null)
                .collectAsStateWithLifecycle()
            HomeScreen(
                transientSnackbarMessage = snackbarMessage,
                onTransientSnackbarShown = {
                    backStackEntry.savedStateHandle.remove<String>(TRANSIENT_SNACKBAR_KEY)
                },
                onNavigateToAddHabit = { navController.navigate(Screen.AddHabit.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Statistic.route) {
            StatisticScreen() // Navigasi dikelola oleh BottomNav
        }

        composable(Screen.AddHabit.route) {
            val context = LocalContext.current
            AddHabitScreen(
                onNavigateBack = { navController.popBackStack() },
                onHabitSaved = {
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        TRANSIENT_SNACKBAR_KEY,
                        context.getString(com.islami.Aha.R.string.add_habit_saved_snackbar_default)
                    )
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Notification.route) {
            NotificationScreen() // Navigasi dikelola oleh BottomNav
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToLogin = { navController.navigate(Screen.Login.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = { message ->
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                    if (!message.isNullOrBlank()) {
                        navController.currentBackStackEntry?.savedStateHandle?.set(
                            TRANSIENT_SNACKBAR_KEY,
                            message
                        )
                    }
                }
            )
        }
    }
}
