package com.zelenbo.app.presentation.navigation

sealed class AppRoute(val route: String) {
    data object Dashboard : AppRoute("dashboard")
    data object Services : AppRoute("services")
    data object Settings : AppRoute("settings")
    data object Logs : AppRoute("logs")
    data object About : AppRoute("about")
}

