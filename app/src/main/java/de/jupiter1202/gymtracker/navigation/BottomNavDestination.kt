package de.jupiter1202.gymtracker.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Dashboard("dashboard", "Dashboard", Icons.Default.Home),
    Exercises("exercises", "Exercises", Icons.Default.Star),
    Plans("plans", "Plans", Icons.Default.DateRange),
    History("history", "History", Icons.AutoMirrored.Filled.List),
    Settings("settings", "Settings", Icons.Default.Settings)
}
