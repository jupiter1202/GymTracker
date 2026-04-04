package de.jupiter1202.gymtracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import de.jupiter1202.gymtracker.feature.dashboard.DashboardScreen
import de.jupiter1202.gymtracker.feature.exercises.ExercisesScreen
import de.jupiter1202.gymtracker.feature.history.HistoryScreen
import de.jupiter1202.gymtracker.feature.plans.PlansScreen
import de.jupiter1202.gymtracker.feature.settings.SettingsScreen

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = BottomNavDestination.Dashboard.route,
        modifier = modifier
    ) {
        composable(BottomNavDestination.Dashboard.route) { DashboardScreen() }
        composable(BottomNavDestination.Exercises.route) { ExercisesScreen() }
        composable(BottomNavDestination.Plans.route) { PlansScreen() }
        composable(BottomNavDestination.History.route) { HistoryScreen() }
        composable(BottomNavDestination.Settings.route) { SettingsScreen() }
    }
}
