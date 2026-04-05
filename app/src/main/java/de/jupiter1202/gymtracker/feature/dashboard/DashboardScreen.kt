package de.jupiter1202.gymtracker.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.jupiter1202.gymtracker.feature.plans.WorkoutPlanRepository
import de.jupiter1202.gymtracker.feature.workout.WorkoutLoggingViewModel
import de.jupiter1202.gymtracker.ui.theme.EmeraldLabelMuted
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToActiveWorkout: (Long) -> Unit = {},
    onNavigateToPlans: () -> Unit = {},
    viewModel: WorkoutLoggingViewModel = koinViewModel()
) {
    val planRepository: WorkoutPlanRepository = koinInject()
    val recentPlan by planRepository.getMostRecentlyUsedPlan().collectAsStateWithLifecycle(null)
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()

    var showActiveSessionDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val today = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // — Greeting —
        item {
            Column {
                Text(
                    "GYMTRACKER",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldLabelMuted
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "READY TO TRAIN?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    today.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // — Quick start card —
        item {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (recentPlan != null) {
                        Text(
                            "CONTINUE PLAN",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldLabelMuted
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            recentPlan!!.name.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (activeSession != null) showActiveSessionDialog = true
                                else scope.launch {
                                    val id = viewModel.startSessionAndGetId(
                                        name = recentPlan!!.name,
                                        planId = recentPlan!!.id,
                                        exercises = emptyList()
                                    )
                                    onNavigateToActiveWorkout(id)
                                }
                            },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor   = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("START WORKOUT", style = MaterialTheme.typography.labelLarge)
                        }
                        TextButton(
                            onClick = onNavigateToPlans,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "PICK A DIFFERENT PLAN",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            "START YOUR FIRST WORKOUT",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldLabelMuted
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "NO RECENT PLAN",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (activeSession != null) showActiveSessionDialog = true
                                else scope.launch {
                                    val id = viewModel.startSessionAndGetId(
                                        name = "Workout",
                                        planId = null,
                                        exercises = emptyList()
                                    )
                                    onNavigateToActiveWorkout(id)
                                }
                            },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor   = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("START EMPTY WORKOUT", style = MaterialTheme.typography.labelLarge)
                        }
                        TextButton(
                            onClick = onNavigateToPlans,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "BROWSE PLANS",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    TextButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "LOG PAST WORKOUT",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // — Stats row —
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCard("THIS WEEK", "—", "SESSIONS", Modifier.weight(1f))
                StatCard("TOTAL", "—", "VOLUME", Modifier.weight(1f))
                StatCard("CURRENT", "—", "STREAK", Modifier.weight(1f))
            }
        }

        // — Section header —
        item {
            Text(
                "QUICK ACTIONS",
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldLabelMuted
            )
        }

        // — Action hints —
        item {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ActionRow("BUILD A PLAN", "CREATE & MANAGE WORKOUT PROGRAMS", onNavigateToPlans)
                }
            }
        }
    }

    // Active session dialog
    if (showActiveSessionDialog) {
        AlertDialog(
            onDismissRequest = { showActiveSessionDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(4.dp),
            title = {
                Text("ACTIVE WORKOUT", style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Text(
                    "You have an active workout. Resume it or discard to start fresh.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showActiveSessionDialog = false
                    activeSession?.let { onNavigateToActiveWorkout(it.id) }
                }) {
                    Text("RESUME", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showActiveSessionDialog = false
                    viewModel.discardSession()
                }) {
                    Text("DISCARD", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // Post-hoc date picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMs = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                    scope.launch {
                        viewModel.startPostHocSession(
                            name = "Workout",
                            planId = null,
                            startedAt = selectedMs
                        )
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("CANCEL") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldLabelMuted
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace
            )
            Text(
                unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
