package de.jupiter1202.gymtracker.feature.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.jupiter1202.gymtracker.core.database.dao.PlanExerciseWithExercise
import de.jupiter1202.gymtracker.core.database.entities.Exercise
import de.jupiter1202.gymtracker.core.database.entities.PlanExercise
import de.jupiter1202.gymtracker.core.database.entities.WorkoutPlan
import de.jupiter1202.gymtracker.core.constants.MUSCLE_GROUPS
import de.jupiter1202.gymtracker.feature.exercises.ExerciseViewModel
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(planId: Long, onNavigateBack: () -> Unit) {
    val viewModel = koinViewModel<WorkoutPlanViewModel>()
    
    // Load the plan exercises
    LaunchedEffect(planId) {
        viewModel.setActivePlan(planId)
    }
    
    val planExercises by viewModel.planExercises.collectAsStateWithLifecycle()
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    
    val plan = plans.find { it.id == planId }
    
    // Local state for drag-and-drop
    var localExercises by remember(planExercises) { mutableStateOf(planExercises) }
    
    // Sheet state management
    var showExercisePicker by remember { mutableStateOf(false) }
    var pendingExercise by remember { mutableStateOf<Exercise?>(null) }
    var editingPlanExercise by remember { mutableStateOf<PlanExerciseWithExercise?>(null) }
    var showRenameSheet by remember { mutableStateOf(false) }
    var showTargetSheet by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plan?.name ?: "Plan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showRenameSheet = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit plan")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Exercise list with drag and drop
            PlanExerciseList(
                exercises = localExercises,
                onReorder = { newExercises ->
                    localExercises = newExercises
                    viewModel.reorderExercises(newExercises.map { it.planExercise })
                },
                onExerciseClick = { exercise ->
                    editingPlanExercise = exercise
                    showTargetSheet = true
                },
                onDeleteExercise = { exercise ->
                    viewModel.removeExercise(exercise.planExercise)
                }
            )
            
            // "+ Add exercise" button at bottom
            ListItem(
                headlineContent = { Text("Add exercise") },
                leadingContent = {
                    Icon(Icons.Default.Add, contentDescription = "Add exercise")
                },
                modifier = Modifier.clickable { showExercisePicker = true }
            )
        }
    }
    
    // Exercise picker sheet
    if (showExercisePicker) {
        ExercisePickerSheet(
            onExerciseSelected = { exercise ->
                showExercisePicker = false
                pendingExercise = exercise
                showTargetSheet = true
            },
            onDismiss = { showExercisePicker = false }
        )
    }
    
    // Target input sheet
    if (showTargetSheet && (pendingExercise != null || editingPlanExercise != null)) {
        val exercise = pendingExercise
        val editing = editingPlanExercise
        
        TargetInputSheet(
            exercise = exercise,
            editingPlanExercise = editing,
            onSave = { sets, reps ->
                if (exercise != null && editing == null) {
                    // Add new exercise
                    viewModel.addExercise(planId, exercise.id, sets, reps)
                } else if (editing != null) {
                    // Edit existing
                    viewModel.updateExerciseTargets(editing.planExercise, sets, reps)
                }
                showTargetSheet = false
                pendingExercise = null
                editingPlanExercise = null
            },
            onCancel = {
                showTargetSheet = false
                pendingExercise = null
                editingPlanExercise = null
            }
        )
    }
    
    // Rename plan sheet
    if (showRenameSheet && plan != null) {
        RenamePlanSheet(
            plan = plan,
            onSave = { newName, newDescription ->
                viewModel.updatePlan(plan.copy(name = newName, description = newDescription))
                showRenameSheet = false
            },
            onCancel = { showRenameSheet = false }
        )
    }
}

@Composable
private fun PlanExerciseList(
    exercises: List<PlanExerciseWithExercise>,
    onReorder: (List<PlanExerciseWithExercise>) -> Unit,
    onExerciseClick: (PlanExerciseWithExercise) -> Unit,
    onDeleteExercise: (PlanExerciseWithExercise) -> Unit
) {
    var localList by remember(exercises) { mutableStateOf(exercises) }
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localList = localList.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        onReorder(localList)
    }
    
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            count = localList.size,
            key = { index -> localList[index].planExercise.id }
        ) { index ->
            val item = localList[index]
            ReorderableItem(reorderState, key = item.planExercise.id) { isDragging ->
                ExerciseRowWithSwipe(
                    item = item,
                    onExerciseClick = { onExerciseClick(item) },
                    onDeleteExercise = { onDeleteExercise(item) },
                    isDragging = isDragging
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseRowWithSwipe(
    item: PlanExerciseWithExercise,
    onExerciseClick: () -> Unit,
    onDeleteExercise: () -> Unit,
    isDragging: Boolean
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDeleteExercise()
                true
            } else false
        }
    )
    
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error)
                    .padding(16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        }
    ) {
        ExerciseRow(
            item = item,
            onExerciseClick = onExerciseClick,
            isDragging = isDragging
        )
    }
}

@Composable
private fun ExerciseRow(
    item: PlanExerciseWithExercise,
    onExerciseClick: () -> Unit,
    isDragging: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExerciseClick() }
            .background(if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Drag handle indicator
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "⋮⋮",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Exercise info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.exercise.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = item.exercise.primaryMuscleGroup,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Target sets × reps
        Text(
            text = "${item.planExercise.targetSets} × ${item.planExercise.targetReps}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp)
        )
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
            items(count = MUSCLE_GROUPS.size) { index ->
                val group = MUSCLE_GROUPS[index]
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetInputSheet(
    exercise: Exercise?,
    editingPlanExercise: PlanExerciseWithExercise?,
    onSave: (sets: Int, reps: String) -> Unit,
    onCancel: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState
    ) {
        TargetInputContent(
            exercise = exercise ?: editingPlanExercise?.exercise,
            initialSets = editingPlanExercise?.planExercise?.targetSets ?: 3,
            initialReps = editingPlanExercise?.planExercise?.targetReps ?: "8",
            isEditing = editingPlanExercise != null,
            onSave = onSave,
            onCancel = onCancel
        )
    }
}

@Composable
private fun TargetInputContent(
    exercise: Exercise?,
    initialSets: Int,
    initialReps: String,
    isEditing: Boolean,
    onSave: (sets: Int, reps: String) -> Unit,
    onCancel: () -> Unit
) {
    var targetSets by remember { mutableIntStateOf(initialSets) }
    var targetReps by remember { mutableStateOf(initialReps) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = exercise?.name ?: "Exercise",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Sets input
        OutlinedTextField(
            value = targetSets.toString(),
            onValueChange = {
                targetSets = it.toIntOrNull() ?: targetSets
            },
            label = { Text("Target sets") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // Reps input
        OutlinedTextField(
            value = targetReps,
            onValueChange = { targetReps = it },
            label = { Text("Target reps (e.g., 5, 8-12, AMRAP)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = { onSave(targetSets, targetReps) },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isEditing) "Save" else "Add to plan")
            }
        }
        
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.ime))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenamePlanSheet(
    plan: WorkoutPlan,
    onSave: (name: String, description: String?) -> Unit,
    onCancel: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState
    ) {
        RenamePlanContent(
            initialName = plan.name,
            initialDescription = plan.description,
            onSave = onSave,
            onCancel = onCancel
        )
    }
}

@Composable
private fun RenamePlanContent(
    initialName: String,
    initialDescription: String?,
    onSave: (name: String, description: String?) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription ?: "") }
    var nameError by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Rename plan",
            style = MaterialTheme.typography.headlineSmall
        )
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; nameError = false },
            label = { Text("Plan name") },
            isError = nameError,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        onSave(name.trim(), description.takeIf { it.isNotBlank() })
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
        
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.ime))
    }
}
