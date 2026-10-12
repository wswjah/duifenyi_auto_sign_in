package com.duifenyi.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.duifenyi.app.ui.screen.HomeScreen
import com.duifenyi.app.ui.screen.LocationConfigScreen
import com.duifenyi.app.ui.screen.LoginScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val LOCATION_CONFIG = "location_config"
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToLocationConfig = {
                    navController.navigate(Routes.LOCATION_CONFIG)
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOCATION_CONFIG) {
            LocationConfigScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
