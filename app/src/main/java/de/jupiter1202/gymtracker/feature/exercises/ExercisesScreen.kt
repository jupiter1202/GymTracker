package de.jupiter1202.gymtracker.feature.exercises

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.jupiter1202.gymtracker.core.constants.EQUIPMENT_TYPES
import de.jupiter1202.gymtracker.core.constants.MUSCLE_GROUPS
import de.jupiter1202.gymtracker.core.database.entities.Exercise
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

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

        // Bottom sheet for create / edit
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                ExerciseFormSheet(
                    exercise = exerciseToEdit,
                    onSave = { name, muscle, equipment ->
                        scope.launch {
                            viewModel.saveExercise(name, muscle, equipment, exerciseToEdit)
                            sheetState.hide()
                        }.invokeOnCompletion { showBottomSheet = false }
                    },
                    onCancel = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showBottomSheet = false
                        }
                    }
                )
            }
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
            SuggestionChip(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseFormSheet(
    exercise: Exercise?,
    onSave: (name: String, muscle: String, equipment: String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(exercise?.name ?: "") }
    var selectedMuscle by remember { mutableStateOf(exercise?.primaryMuscleGroup ?: MUSCLE_GROUPS[0]) }
    var selectedEquipment by remember { mutableStateOf(exercise?.equipmentType ?: EQUIPMENT_TYPES[0]) }
    var nameError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Drag handle
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp, 4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }

        // Title
        Text(
            text = if (exercise != null) "Edit Exercise" else "New Exercise",
            style = MaterialTheme.typography.titleLarge
        )

        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = false
            },
            label = { Text("Exercise name") },
            isError = nameError,
            supportingText = if (nameError) {
                { Text("Name is required") }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Muscle group dropdown
        ExposedDropdownField(
            label = "Primary Muscle Group",
            selectedItem = selectedMuscle,
            items = MUSCLE_GROUPS,
            onItemSelected = { selectedMuscle = it }
        )

        // Equipment type dropdown
        ExposedDropdownField(
            label = "Equipment Type",
            selectedItem = selectedEquipment,
            items = EQUIPMENT_TYPES,
            onItemSelected = { selectedEquipment = it }
        )

        // Footer buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        onSave(name.trim(), selectedMuscle, selectedEquipment)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }

        // IME spacer for keyboard
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.ime))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownField(
    label: String,
    selectedItem: String,
    items: List<String>,
    onItemSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedItem,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
