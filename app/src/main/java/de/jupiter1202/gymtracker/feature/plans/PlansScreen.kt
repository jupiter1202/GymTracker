package de.jupiter1202.gymtracker.feature.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.jupiter1202.gymtracker.core.database.entities.WorkoutPlan
import de.jupiter1202.gymtracker.feature.workout.WorkoutLoggingViewModel
import de.jupiter1202.gymtracker.ui.theme.EmeraldLabelMuted
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun PlansScreen(
    onPlanClick: (Long) -> Unit,
    onTemplateClick: (String) -> Unit,
    onStartPlan: (Long) -> Unit = {}
) {
    val viewModel: WorkoutPlanViewModel = koinViewModel()
    val workoutViewModel: WorkoutLoggingViewModel = koinViewModel()
    val planRepository: WorkoutPlanRepository = koinInject()
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val activeSession by workoutViewModel.activeSession.collectAsStateWithLifecycle()

    var showCreateSheet by remember { mutableStateOf(false) }
    var planToEdit by remember { mutableStateOf<WorkoutPlan?>(null) }
    var showActiveSessionDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                shape = RoundedCornerShape(4.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create plan")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Page header
            item {
                Text(
                    "PROGRAMS",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
            }

            // My Plans section
            item {
                Text(
                    "MY PLANS",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldLabelMuted
                )
            }

            if (plans.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "NO PLANS YET",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Tap + to create your first plan",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldLabelMuted
                            )
                        }
                    }
                }
            } else {
                items(plans, key = { it.id }) { plan ->
                    PlanCard(
                        plan = plan,
                        onClick = { onPlanClick(plan.id) },
                        onEditClick = { planToEdit = plan },
                        onDeleteClick = { viewModel.deletePlan(plan) },
                        onStartClick = {
                            if (activeSession != null) {
                                showActiveSessionDialog = true
                            } else {
                                scope.launch {
                                    try {
                                        val planExercises = planRepository.getPlanExercises(plan.id).first()
                                            .map { it.exercise }
                                        val sessionId = workoutViewModel.startSessionAndGetId(
                                            name = plan.name,
                                            planId = plan.id,
                                            exercises = planExercises
                                        )
                                        onStartPlan(sessionId)
                                    } catch (e: Exception) {
                                        // fallback: start without exercises
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Pre-built Programs section
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "PRE-BUILT PROGRAMS",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldLabelMuted
                )
            }

            items(templates, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    onClick = { onTemplateClick(template.id) }
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Create plan sheet
    if (showCreateSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
        ) {
            CreatePlanSheet(
                onSave = { name, description ->
                    viewModel.createPlan(name, description) { newPlanId ->
                        showCreateSheet = false
                        onPlanClick(newPlanId)
                    }
                },
                onCancel = { showCreateSheet = false }
            )
        }
    }

    // Edit plan sheet
    planToEdit?.let { plan ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { planToEdit = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
        ) {
            EditPlanSheet(
                plan = plan,
                onSave = { newName, newDescription ->
                    viewModel.updatePlan(plan.copy(name = newName, description = newDescription))
                    planToEdit = null
                },
                onCancel = { planToEdit = null }
            )
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
                    "You have an active workout. Resume it or discard to start a new plan.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showActiveSessionDialog = false
                    activeSession?.let { onStartPlan(it.id) }
                }) {
                    Text("RESUME", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showActiveSessionDialog = false
                    workoutViewModel.discardSession()
                }) {
                    Text("DISCARD", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

@Composable
fun PlanCard(
    plan: WorkoutPlan,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onStartClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        plan.name.uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "CREATED ${formatDate(plan.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLabelMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onStartClick,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor   = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("START", style = MaterialTheme.typography.labelMedium)
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        DropdownMenuItem(
                            text = { Text("View details", style = MaterialTheme.typography.bodyMedium) },
                            onClick = { showMenu = false; onClick() }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit", style = MaterialTheme.typography.bodyMedium) },
                            onClick = { showMenu = false; onEditClick() }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Delete",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = { showMenu = false; onDeleteClick() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateCard(template: TemplateProgram, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    template.name.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Text(
                    "TEMPLATE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlanSheet(onSave: (String, String?) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "CREATE PLAN",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; nameError = false },
            label = { Text("PLAN NAME *", style = MaterialTheme.typography.labelSmall) },
            isError = nameError,
            singleLine = true,
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("DESCRIPTION (OPTIONAL)", style = MaterialTheme.typography.labelSmall) },
            maxLines = 3,
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1f)
            ) { Text("CANCEL", style = MaterialTheme.typography.labelMedium) }
            Button(
                onClick = {
                    if (name.isBlank()) nameError = true
                    else onSave(name, description.ifBlank { null })
                },
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor   = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f)
            ) { Text("SAVE", style = MaterialTheme.typography.labelMedium) }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.ime))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlanSheet(plan: WorkoutPlan, onSave: (String, String?) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf(plan.name) }
    var description by remember { mutableStateOf(plan.description.orEmpty()) }
    var nameError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "EDIT PLAN",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; nameError = false },
            label = { Text("PLAN NAME *", style = MaterialTheme.typography.labelSmall) },
            isError = nameError,
            singleLine = true,
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("DESCRIPTION (OPTIONAL)", style = MaterialTheme.typography.labelSmall) },
            maxLines = 3,
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1f)
            ) { Text("CANCEL", style = MaterialTheme.typography.labelMedium) }
            Button(
                onClick = {
                    if (name.isBlank()) nameError = true
                    else onSave(name, description.ifBlank { null })
                },
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor   = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f)
            ) { Text("SAVE", style = MaterialTheme.typography.labelMedium) }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.ime))
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date(millis))
