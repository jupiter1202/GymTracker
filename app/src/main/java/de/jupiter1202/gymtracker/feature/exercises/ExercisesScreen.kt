package de.jupiter1202.gymtracker.feature.exercises

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.jupiter1202.gymtracker.core.database.entities.Exercise
import org.koin.androidx.compose.koinViewModel

val MUSCLE_GROUPS = listOf(
    "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Forearms",
    "Quads", "Hamstrings", "Glutes", "Calves", "Core", "Cardio"
)

val EQUIPMENT_TYPES = listOf(
    "Barbell", "Dumbbell", "Cable", "Machine", "Bodyweight",
    "Kettlebell", "Resistance Band", "Other"
)

@Composable
fun ExercisesScreen() {
    val viewModel: ExerciseViewModel = koinViewModel()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val groupedExercises by viewModel.groupedExercises.collectAsStateWithLifecycle()
    val deleteResult by viewModel.deleteResult.collectAsStateWithLifecycle()

    var showBottomSheet by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<Exercise?>(null) }
    var showDeleteBlockedDialog by remember { mutableStateOf(false) }
    var blockedSessionCount by remember { mutableStateOf(0) }

    LaunchedEffect(deleteResult) {
        when (val result = deleteResult) {
            is DeleteResult.Blocked -> {
                blockedSessionCount = result.sessionCount
                showDeleteBlockedDialog = true
            }
            is DeleteResult.Deleted -> {
                viewModel.clearDeleteResult()
            }
            null -> { /* no-op */ }
        }
    }

    if (showDeleteBlockedDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteBlockedDialog = false
                viewModel.clearDeleteResult()
            },
            title = { Text("Cannot delete") },
            text = {
                Text(
                    "This exercise was used in $blockedSessionCount sessions. " +
                        "Remove it from all sessions first."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteBlockedDialog = false
                    viewModel.clearDeleteResult()
                }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                exerciseToEdit = null
                showBottomSheet = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add exercise")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder = { Text("Search exercises") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearSearch) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filter chip row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(listOf("All") + MUSCLE_GROUPS) { group ->
                    FilterChip(
                        selected = (group == "All" && selectedMuscleGroup == null) ||
                            (group == selectedMuscleGroup),
                        onClick = {
                            viewModel.onMuscleGroupSelected(if (group == "All") null else group)
                        },
                        label = { Text(group) }
                    )
                }
            }

            // Exercise list
            ExerciseList(
                searchQuery = searchQuery,
                exercises = exercises,
                groupedExercises = groupedExercises,
                onEditExercise = { exercise ->
                    exerciseToEdit = exercise
                    showBottomSheet = true
                },
                onDeleteExercise = viewModel::deleteExercise
            )
        }

        // Bottom sheet will be added in Task 2
        if (showBottomSheet) {
            ExerciseFormSheet(
                exercise = exerciseToEdit,
                showBottomSheet = showBottomSheet,
                onDismiss = { showBottomSheet = false },
                onSave = { name, muscle, equipment ->
                    viewModel.saveExercise(name, muscle, equipment, exerciseToEdit)
                    showBottomSheet = false
                },
                onCancel = { showBottomSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExerciseList(
    searchQuery: String,
    exercises: List<Exercise>,
    groupedExercises: Map<String, List<Exercise>>,
    onEditExercise: (Exercise) -> Unit,
    onDeleteExercise: (Exercise) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (searchQuery.isNotBlank()) {
            items(exercises) { exercise ->
                ExerciseRow(
                    exercise = exercise,
                    onEditExercise = onEditExercise,
                    onDeleteExercise = onDeleteExercise
                )
            }
        } else {
            groupedExercises.forEach { (group, groupExercises) ->
                stickyHeader {
                    MuscleGroupHeader(muscleGroup = group)
                }
                items(groupExercises) { exercise ->
                    ExerciseRow(
                        exercise = exercise,
                        onEditExercise = onEditExercise,
                        onDeleteExercise = onDeleteExercise
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExerciseRow(
    exercise: Exercise,
    onEditExercise: (Exercise) -> Unit,
    onDeleteExercise: (Exercise) -> Unit
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    if (exercise.isCustom) showContextMenu = true
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "${exercise.primaryMuscleGroup} · ${exercise.equipmentType}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (exercise.isCustom) {
            androidx.compose.material3.SuggestionChip(
                onClick = {},
                label = { Text("Custom") }
            )
        }
    }

    if (exercise.isCustom) {
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit exercise") },
                onClick = {
                    showContextMenu = false
                    onEditExercise(exercise)
                }
            )
            DropdownMenuItem(
                text = { Text("Delete exercise") },
                onClick = {
                    showContextMenu = false
                    onDeleteExercise(exercise)
                }
            )
        }
    }
}

@Composable
private fun MuscleGroupHeader(muscleGroup: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text = muscleGroup,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ExerciseFormSheet(
    exercise: Exercise?,
    showBottomSheet: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, muscle: String, equipment: String) -> Unit,
    onCancel: () -> Unit
) {
    // Placeholder — full implementation in Task 2
}
