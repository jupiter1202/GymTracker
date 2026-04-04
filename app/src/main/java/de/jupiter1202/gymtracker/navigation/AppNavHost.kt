package de.jupiter1202.gymtracker.navigation

import androidx.compose.runtime.Composable
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
import de.jupiter1202.gymtracker.feature.plans.PlanDetailScreen
import de.jupiter1202.gymtracker.feature.plans.TemplatePreviewScreen
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

        // Plan detail route
        composable(
            route = "plan_detail/{planId}",
            arguments = listOf(navArgument("planId") { type = NavType.LongType })
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getLong("planId") ?: return@composable
            PlanDetailScreen(
                planId = planId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Template preview route
        composable(
            route = "template_preview/{templateId}",
            arguments = listOf(navArgument("templateId") { type = NavType.StringType })
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId") ?: return@composable
            TemplatePreviewScreen(
                templateId = templateId,
                onNavigateBack = { navController.popBackStack() },
                onImported = { planId ->
                    // Pop template_preview off back stack, then navigate to plan detail
                    navController.popBackStack()
                    navController.navigate("plan_detail/$planId")
                }
            )
        }
    }
}
