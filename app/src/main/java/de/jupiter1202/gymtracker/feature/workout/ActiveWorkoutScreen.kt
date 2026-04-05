package de.jupiter1202.gymtracker.feature.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.jupiter1202.gymtracker.core.database.entities.Exercise
import de.jupiter1202.gymtracker.core.UnitConverter
import de.jupiter1202.gymtracker.core.constants.MUSCLE_GROUPS
import de.jupiter1202.gymtracker.feature.exercises.ExerciseViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    sessionId: Long,
    onFinished: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: WorkoutLoggingViewModel = koinViewModel()
) {
    // Resume session on first composition
    LaunchedEffect(sessionId) {
        // Only resume from database if activeSession is null (crash recovery)
        // Fresh starts already have exercises populated by startSessionAndGetId
        if (viewModel.activeSession.value == null) {
            viewModel.resumeSession(sessionId)
        }
    }

    // State management
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val sections by viewModel.exerciseSections.collectAsStateWithLifecycle()
    val elapsed by viewModel.elapsedMs.collectAsStateWithLifecycle()
    val restState by viewModel.restTimerState.collectAsStateWithLifecycle()
    val weightUnit by viewModel.weightUnit.collectAsStateWithLifecycle()

    var showExercisePicker by remember { mutableStateOf(false) }
    var confirmFinishDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(formatElapsed(elapsed))
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                val canFinish = viewModel.finishSession()
                                if (canFinish) {
                                    onFinished(sessionId)
                                } else {
                                    confirmFinishDialog = true
                                }
                            }
                        ) {
                            Text("Finish")
                        }
                    }
                )

                // Rest timer banner - pinned here, not scrollable
                AnimatedVisibility(visible = restState is RestTimerState.Running) {
                    RestTimerBanner(
                        state = restState as? RestTimerState.Running,
                        onSkip = { viewModel.skipRestTimer() },
                        onExtend = { viewModel.extendRestTimer(30) }
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            sections.forEach { section ->
                // Section header with exercise name and previous performance
                item(key = "header_${section.exercise.id}") {
                    ExerciseSectionHeader(
                        exercise = section.exercise,
                        previousPerformance = section.previousPerformance,
                        onRemove = { viewModel.removeExercise(section.exercise) }
                    )
                }

                // Logged sets (read-only rows with long-press to delete)
                items(section.loggedSets, key = { "set_${it.id}" }) { set ->
                    LoggedSetRow(
                        set = set,
                        weightUnit = weightUnit,
                        onDelete = { viewModel.deleteSet(set, section.exercise.id) }
                    )
                }

                // Pending set input row
                item(key = "pending_${section.exercise.id}") {
                    PendingSetRow(
                        input = section.pendingInput,
                        weightUnit = weightUnit,
                        onWeightChange = { viewModel.updatePendingWeight(section.exercise.id, it) },
                        onRepsChange = { viewModel.updatePendingReps(section.exercise.id, it) },
                        onLog = { viewModel.logSet(section.exercise.id) }
                    )
                }

                // "Mark Exercise Done" button - triggers rest timer without logging a set
                item(key = "done_${section.exercise.id}") {
                    DoneButton(
                        onClick = { viewModel.markExerciseDone(section.exercise.id) }
                    )
                }

                // "+ Add set" button
                item(key = "addset_${section.exercise.id}") {
                    TextButton(
                        onClick = { /* Placeholder - would add new pending row */ },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add set")
                    }
                }
            }

            // "+ Add exercise" button at bottom
            item(key = "add_exercise") {
                OutlinedButton(
                    onClick = { showExercisePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add exercise")
                }
            }
        }
    }

    // Exercise picker bottom sheet
    if (showExercisePicker) {
        ExercisePickerSheet(
            onExerciseSelected = { exercise ->
                viewModel.addExercise(exercise)
                showExercisePicker = false
            },
            onDismiss = { showExercisePicker = false }
        )
    }

    // Confirm finish dialog
    if (confirmFinishDialog) {
        AlertDialog(
            onDismissRequest = { confirmFinishDialog = false },
            title = { Text("Exercises with no sets") },
            text = { Text("Some exercises have no sets logged. Finish anyway?") },
            dismissButton = {
                TextButton(onClick = { confirmFinishDialog = false }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmFinishDialog = false
                        // Force finish without checking for empty sections
                        viewModel.finishSession()
                        onFinished(sessionId)
                    }
                ) {
                    Text("Finish")
                }
            }
        )
    }
}

@Composable
private fun ExerciseSectionHeader(
    exercise: Exercise,
    previousPerformance: String,
    onRemove: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleSmall
                )
                if (previousPerformance.isNotEmpty()) {
                    Text(
                        text = previousPerformance,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove exercise",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun LoggedSetRow(
    set: LoggedSet,
    weightUnit: String,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { },
                onLongClick = { onDelete }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Set ${set.setNumber}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(60.dp)
        )

        val displayWeight = if (weightUnit == "lbs") {
            String.format("%.1f", UnitConverter.kgToLbs(set.weightKg))
        } else {
            String.format("%.1f", set.weightKg)
        }

        Text(
            text = "$displayWeight $weightUnit",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(80.dp)
        )

        Text(
            text = "${set.reps} reps",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "Long-press to delete",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PendingSetRow(
    input: PendingSetInput,
    weightUnit: String,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onLog: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = input.weightDisplay,
            onValueChange = onWeightChange,
            label = { Text("Weight ($weightUnit)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(100.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = input.reps,
            onValueChange = onRepsChange,
            label = { Text("Reps") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(80.dp),
            singleLine = true
        )

        IconButton(onClick = onLog) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Log set",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun RestTimerBanner(
    state: RestTimerState.Running?,
    onSkip: () -> Unit,
    onExtend: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state != null) {
                Text(
                    text = "Rest · ${formatTime(state.remaining)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            TextButton(onClick = onSkip) {
                Text("Skip", color = MaterialTheme.colorScheme.onPrimary)
            }

            TextButton(onClick = onExtend) {
                Text("+30s", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun DoneButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text("Mark Exercise Done")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    onExerciseSelected: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        ExercisePickerContent(
            onExerciseSelected = {
                onExerciseSelected(it)
                onDismiss()
            }
        )
    }
}

@Composable
private fun ExercisePickerContent(
    onExerciseSelected: (Exercise) -> Unit
) {
    val viewModel = koinViewModel<ExerciseViewModel>()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            label = { Text("Search exercises") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Muscle group filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(MUSCLE_GROUPS) { group ->
                FilterChip(
                    selected = selectedMuscleGroup == group,
                    onClick = {
                        viewModel.onMuscleGroupSelected(
                            if (selectedMuscleGroup == group) null else group
                        )
                    },
                    label = { Text(group) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Exercise list
        LazyColumn {
            items(
                count = exercises.size,
                key = { index -> exercises[index].id }
            ) { index ->
                val exercise = exercises[index]
                ListItem(
                    headlineContent = { Text(exercise.name) },
                    supportingContent = { Text(exercise.primaryMuscleGroup) },
                    modifier = Modifier.clickable { onExerciseSelected(exercise) }
                )
            }
        }
    }
}

/**
 * Format elapsed time in seconds to HH:MM:SS format
 */
private fun formatElapsed(ms: Long): String {
    val seconds = ms / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return "%d:%02d:%02d".format(hours, minutes, secs)
}

/**
 * Format time in seconds to MM:SS format
 */
private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}
