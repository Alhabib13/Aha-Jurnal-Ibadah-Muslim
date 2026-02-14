package com.islami.Aha.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.islami.Aha.ui.components.AhaBottomNavBar
import com.islami.Aha.ui.theme.HabitIslamiTheme
import com.islami.Aha.ui.theme.ThemeManager

@Composable
fun AhaApp() {
    val isDarkMode by ThemeManager.isDarkMode.collectAsStateWithLifecycle()

    HabitIslamiTheme(darkTheme = isDarkMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val scaffoldInsets = if (shouldShowBottomNav(currentRoute)) {
                WindowInsets(0, 0, 0, 0).only(WindowInsetsSides.Horizontal)
            } else if (currentRoute == Screen.AddHabit.route) {
                WindowInsets(0, 0, 0, 0).only(WindowInsetsSides.Horizontal)
            } else if (currentRoute == Screen.Settings.route) {
                WindowInsets(0, 0, 0, 0).only(WindowInsetsSides.Horizontal)
            } else if (currentRoute == Screen.Home.route || currentRoute == Screen.Profile.route) {
                ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            } else {
                ScaffoldDefaults.contentWindowInsets
            }

            Scaffold(
                contentWindowInsets = scaffoldInsets,
                bottomBar = {
                    if (shouldShowBottomNav(currentRoute)) {
                        AhaBottomNavBar(
                            currentRoute = currentRoute ?: Screen.Home.route,
                            onItemSelected = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            ) { innerPadding ->
                AhaNavHost(
                    navController = navController,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

val bottomNavRoutes = setOf(
    Screen.Home.route,
    Screen.Statistic.route,
    Screen.Notification.route,
    Screen.Profile.route
)

fun shouldShowBottomNav(route: String?): Boolean {
    return route in bottomNavRoutes
}
