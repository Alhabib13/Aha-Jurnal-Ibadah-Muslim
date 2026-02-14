package com.islami.Aha.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.islami.Aha.ui.addhabit.AddHabitScreen
import com.islami.Aha.ui.auth.LoginScreen
import com.islami.Aha.ui.auth.RegisterScreen
import com.islami.Aha.ui.home.HomeScreen
import com.islami.Aha.ui.notification.NotificationScreen
import com.islami.Aha.ui.profile.ProfileScreen
import com.islami.Aha.ui.settings.SettingsScreen
import com.islami.Aha.ui.splash.SplashScreen
import com.islami.Aha.ui.statistic.StatisticScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
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
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToAddHabit = { navController.navigate(Screen.AddHabit.route) }
            )
        }

        composable(Screen.Statistic.route) {
            StatisticScreen() // Navigasi dikelola oleh BottomNav
        }

        composable(Screen.AddHabit.route) {
            AddHabitScreen(
                onNavigateBack = { navController.popBackStack() },
                onHabitSaved = { navController.popBackStack() }
            )
        }

        composable(Screen.Notification.route) {
            NotificationScreen() // Navigasi dikelola oleh BottomNav
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onLogout = {}
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
