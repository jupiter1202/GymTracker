package de.jupiter1202.gymtracker.feature.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePicker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.jupiter1202.gymtracker.feature.plans.WorkoutPlanRepository
import de.jupiter1202.gymtracker.feature.workout.WorkoutLoggingViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToActiveWorkout: (Long) -> Unit = {},
    onNavigateToPlans: () -> Unit = {},
    viewModel: WorkoutLoggingViewModel = koinViewModel()
) {
    val planRepository: WorkoutPlanRepository = koinInject<WorkoutPlanRepository>()
    val recentPlan by planRepository.getMostRecentlyUsedPlan().collectAsStateWithLifecycle(null)
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    
    var showActiveSessionDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            "Ready to train?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick-start workout card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                if (recentPlan != null) {
                    Text(
                        "Continue this plan",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        recentPlan!!.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (activeSession != null) {
                                    showActiveSessionDialog = true
                                } else {
                                    scope.launch {
                                        // Start session with recent plan
                                        val sessionId = viewModel.startSessionAndGetId(
                                            name = recentPlan!!.name,
                                            planId = recentPlan!!.id,
                                            exercises = emptyList()
                                        )
                                        onNavigateToActiveWorkout(sessionId)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Start")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = onNavigateToPlans,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Or pick a plan")
                        }
                    }
                } else {
                    Text(
                        "No recent workouts",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (activeSession != null) {
                                    showActiveSessionDialog = true
                                } else {
                                    scope.launch {
                                        val sessionId = viewModel.startSessionAndGetId(
                                            name = "Workout",
                                            planId = null,
                                            exercises = emptyList()
                                        )
                                        onNavigateToActiveWorkout(sessionId)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start empty workout")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = onNavigateToPlans,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pick a plan")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Post-hoc entry")
                }
            }
        }
    }

    // Active session alert dialog
    if (showActiveSessionDialog) {
        AlertDialog(
            onDismissRequest = { showActiveSessionDialog = false },
            title = { Text("Active Workout") },
            text = { Text("You have an active workout. Finish it first or discard it.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showActiveSessionDialog = false
                        if (activeSession != null) {
                            onNavigateToActiveWorkout(activeSession!!.id)
                        }
                    }
                ) {
                    Text("Resume")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showActiveSessionDialog = false
                        viewModel.discardSession()
                    }
                ) {
                    Text("Discard")
                }
            }
        )
    }

    // Post-hoc date picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMs = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                        showDatePicker = false
                        scope.launch {
                            viewModel.startPostHocSession(name = "Workout", planId = null, startedAt = selectedMs)
                            // Note: post-hoc sessions are auto-completed, so we don't navigate to active screen
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
