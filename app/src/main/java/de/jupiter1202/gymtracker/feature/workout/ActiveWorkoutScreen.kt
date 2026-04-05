@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package de.jupiter1202.gymtracker.feature.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.jupiter1202.gymtracker.core.UnitConverter
import de.jupiter1202.gymtracker.core.constants.MUSCLE_GROUPS
import de.jupiter1202.gymtracker.core.database.entities.Exercise
import de.jupiter1202.gymtracker.feature.exercises.ExerciseViewModel
import de.jupiter1202.gymtracker.ui.theme.EmeraldLabelMuted
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    sessionId: Long,
    onFinished: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: WorkoutLoggingViewModel = koinViewModel()
) {
    LaunchedEffect(sessionId) {
        if (viewModel.activeSession.value == null) viewModel.resumeSession(sessionId)
    }

    val sections by viewModel.exerciseSections.collectAsStateWithLifecycle()
    val elapsed by viewModel.elapsedMs.collectAsStateWithLifecycle()
    val restState by viewModel.restTimerState.collectAsStateWithLifecycle()
    val weightUnit by viewModel.weightUnit.collectAsStateWithLifecycle()

    var showExercisePicker by remember { mutableStateOf(false) }
    var confirmFinishDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    title = {
                        Text(
                            formatElapsed(elapsed),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    actions = {
                        Button(
                            onClick = {
                                scope.launch {
                                    val ok = viewModel.finishSession()
                                    if (ok) onFinished(sessionId)
                                    else confirmFinishDialog = true
                                }
                            },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor   = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("FINISH", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                )

                // Rest timer banner
                AnimatedVisibility(
                    visible = restState is RestTimerState.Running,
                    enter = fadeIn(),
                    exit  = fadeOut()
                ) {
                    RestTimerBanner(
                        state = restState as? RestTimerState.Running,
                        onSkip = { viewModel.skipRestTimer() },
                        onExtend = { viewModel.extendRestTimer(30) }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (sections.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "NO EXERCISES YET",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Add an exercise to begin",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldLabelMuted
                            )
                        }
                    }
                }
            }

            sections.forEach { section ->
                // Exercise header
                item(key = "header_${section.exercise.id}") {
                    Spacer(Modifier.height(8.dp))
                    ExerciseSectionHeader(
                        exercise = section.exercise,
                        previousPerformance = section.previousPerformance,
                        onRemove = { viewModel.removeExercise(section.exercise) }
                    )
                }

                // Logged sets
                items(section.loggedSets, key = { "set_${it.id}" }) { set ->
                    LoggedSetRow(
                        set = set,
                        weightUnit = weightUnit,
                        onDelete = { viewModel.deleteSet(set, section.exercise.id) }
                    )
                }

                // Pending set input
                item(key = "pending_${section.exercise.id}") {
                    PendingSetRow(
                        input = section.pendingInput,
                        weightUnit = weightUnit,
                        onWeightChange = { viewModel.updatePendingWeight(section.exercise.id, it) },
                        onRepsChange   = { viewModel.updatePendingReps(section.exercise.id, it) },
                        onLog          = { viewModel.logSet(section.exercise.id) }
                    )
                }

                // Mark done / add set row
                item(key = "actions_${section.exercise.id}") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { viewModel.markExerciseDone(section.exercise.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "MARK DONE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = { /* extra set – currently handled via LOG SET */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "ADD SET",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Add exercise button
            item(key = "add_exercise") {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showExercisePicker = true },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ADD EXERCISE", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Exercise picker
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(4.dp),
            title = { Text("EMPTY EXERCISES", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "Some exercises have no sets logged. Finish anyway?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            dismissButton = {
                TextButton(onClick = { confirmFinishDialog = false }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmFinishDialog = false
                    scope.launch {
                        viewModel.finishSession()
                        onFinished(sessionId)
                    }
                }) {
                    Text("FINISH ANYWAY", color = MaterialTheme.colorScheme.primary)
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
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exercise.name.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (previousPerformance.isNotEmpty()) {
                    Text(
                        "PREV: ${previousPerformance.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLabelMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun LoggedSetRow(set: LoggedSet, weightUnit: String, onDelete: () -> Unit) {
    val displayWeight = if (weightUnit == "lbs")
        "%.1f".format(UnitConverter.kgToLbs(set.weightKg))
    else
        "%.1f".format(set.weightKg)

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = onDelete)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "SET ${set.setNumber}",
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldLabelMuted,
                modifier = Modifier.width(48.dp)
            )
            Text(
                "$displayWeight $weightUnit",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(88.dp)
            )
            Text(
                "${set.reps} REPS",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
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
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Weight recessed box
                RecessedInputBox(
                    value = input.weightDisplay,
                    onValueChange = onWeightChange,
                    label = weightUnit.uppercase(),
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                // Reps recessed box
                RecessedInputBox(
                    value = input.reps,
                    onValueChange = onRepsChange,
                    label = "REPS",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
            Button(
                onClick = onLog,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor   = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("LOG SET", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun RecessedInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldLabelMuted
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
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
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (state != null) "REST · ${formatTime(state.remaining)}" else "REST",
                style = MaterialTheme.typography.labelLarge,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onSkip) {
                Text("SKIP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            TextButton(onClick = onExtend) {
                Text("+30s", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(onExerciseSelected: (Exercise) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        ExercisePickerContent(onExerciseSelected = { onExerciseSelected(it); onDismiss() })
    }
}

@Composable
private fun ExercisePickerContent(onExerciseSelected: (Exercise) -> Unit) {
    val viewModel = koinViewModel<ExerciseViewModel>()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "ADD EXERCISE",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            placeholder = {
                Text(
                    "SEARCH EXERCISES",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldLabelMuted
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor   = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            modifier = Modifier.fillMaxWidth()
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MUSCLE_GROUPS) { group ->
                FilterChip(
                    selected = selectedMuscleGroup == group,
                    onClick = {
                        viewModel.onMuscleGroupSelected(
                            if (selectedMuscleGroup == group) null else group
                        )
                    },
                    label = {
                        Text(group.uppercase(), style = MaterialTheme.typography.labelSmall)
                    },
                    shape = RoundedCornerShape(4.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor     = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        LazyColumn {
            items(count = exercises.size, key = { exercises[it].id }) { index ->
                val exercise = exercises[index]
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExerciseSelected(exercise) }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                exercise.name.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                exercise.primaryMuscleGroup,
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldLabelMuted
                            )
                        }
                        Text(
                            "+",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private fun formatElapsed(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return "%d:%02d:%02d".format(h, m, sec)
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60; val s = seconds % 60
    return "%d:%02d".format(m, s)
}
