package de.jupiter1202.gymtracker.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
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
        composable(BottomNavDestination.Plans.route) {
            PlansScreen(
                onPlanClick = { planId -> navController.navigate("plan_detail/$planId") },
                onTemplateClick = { templateId -> navController.navigate("template_preview/$templateId") }
            )
        }
        composable(BottomNavDestination.History.route) { HistoryScreen() }
        composable(BottomNavDestination.Settings.route) { SettingsScreen() }

        // Plan detail route (placeholder — replaced in 03-05)
        composable(
            route = "plan_detail/{planId}",
            arguments = listOf(navArgument("planId") { type = NavType.LongType })
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getLong("planId") ?: return@composable
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Plan Detail — id: $planId")
            }
        }

        // Template preview route (placeholder — replaced in 03-06)
        composable(
            route = "template_preview/{templateId}",
            arguments = listOf(navArgument("templateId") { type = NavType.StringType })
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId") ?: return@composable
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Template Preview — $templateId")
            }
        }
    }
}
