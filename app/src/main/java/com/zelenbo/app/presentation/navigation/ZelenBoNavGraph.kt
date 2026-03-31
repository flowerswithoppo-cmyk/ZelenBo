package com.zelenbo.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zelenbo.app.presentation.about.AboutScreen
import com.zelenbo.app.presentation.dashboard.DashboardScreen
import com.zelenbo.app.presentation.logs.LogsScreen
import com.zelenbo.app.presentation.services.ServicesScreen
import com.zelenbo.app.presentation.settings.SettingsScreen

@Composable
fun ZelenBoNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoute.Dashboard.route,
        modifier = modifier
    ) {
        composable(AppRoute.Dashboard.route) {
            DashboardScreen(onNavigate = { navController.navigate(it.route) })
        }
        composable(AppRoute.Services.route) {
            ServicesScreen(onNavigate = { navController.navigate(it.route) })
        }
        composable(AppRoute.Settings.route) {
            SettingsScreen(onNavigate = { navController.navigate(it.route) })
        }
        composable(AppRoute.Logs.route) {
            LogsScreen(onNavigate = { navController.navigate(it.route) })
        }
        composable(AppRoute.About.route) {
            AboutScreen(onNavigate = { navController.navigate(it.route) })
        }
    }
}

