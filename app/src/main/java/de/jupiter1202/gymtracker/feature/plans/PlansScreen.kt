package de.jupiter1202.gymtracker.feature.plans

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.jupiter1202.gymtracker.core.database.entities.WorkoutPlan
import de.jupiter1202.gymtracker.feature.workout.WorkoutLoggingViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalCoroutinesApi::class)
@Composable
fun PlansScreen(
    onPlanClick: (Long) -> Unit,
    onTemplateClick: (String) -> Unit,
    onStartPlan: (Long) -> Unit = {}
) {
    val viewModel: WorkoutPlanViewModel = koinViewModel()
    val workoutViewModel: WorkoutLoggingViewModel = koinViewModel()
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val activeSession by workoutViewModel.activeSession.collectAsStateWithLifecycle()

    var showCreateSheet by remember { mutableStateOf(false) }
    var planToEdit by remember { mutableStateOf<WorkoutPlan?>(null) }
    var showActiveSessionDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create plan")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // My Plans section header
            stickyHeader(key = "header_my_plans") {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Text(
                        "My Plans",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }

            if (plans.isEmpty()) {
                item(key = "empty_state") {
                    EmptyPlansState(
                        onCreateClick = { showCreateSheet = true },
                        onBrowseTemplatesClick = {
                            // User scrolls to template section manually for now
                        }
                    )
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
                                        val sessionId = workoutViewModel.startSessionAndGetId(
                                            name = plan.name,
                                            planId = plan.id,
                                            exercises = emptyList()
                                        )
                                        onStartPlan(sessionId)
                                    } catch (e: Exception) {
                                        // Handle error silently for now
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Pre-built Programs section header
            stickyHeader(key = "header_templates") {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Text(
                        "Pre-built Programs",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }

            items(templates, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    onClick = { onTemplateClick(template.id) }
                )
            }

            item { Spacer(Modifier.height(80.dp)) } // FAB clearance
        }

        // Create plan bottom sheet
        if (showCreateSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showCreateSheet = false },
                sheetState = sheetState
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

        // Edit plan bottom sheet
        planToEdit?.let { plan ->
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { planToEdit = null },
                sheetState = sheetState
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
                                onStartPlan(activeSession!!.id)
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
                            workoutViewModel.discardSession()
                        }
                    ) {
                        Text("Discard")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlanCard(
    plan: WorkoutPlan,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onStartClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        plan.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "0 exercises · ${formatDate(plan.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Box(modifier = Modifier.width(100.dp)) {
                    OutlinedButton(
                        onClick = {
                            onStartClick()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start", maxLines = 1)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit plan") },
                            onClick = {
                                showMenu = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete plan") },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onClick() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Details")
            }

            TextButton(
                onClick = { showMenu = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("More options")
            }
        }
    }
}

@Composable
fun TemplateCard(
    template: TemplateProgram,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        template.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Template") },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Text(
                    template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyPlansState(
    onCreateClick: () -> Unit,
    onBrowseTemplatesClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "No plans yet",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "Create your first workout plan",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Button(onClick = onCreateClick) {
            Text("Create your first plan")
        }
        TextButton(onClick = onBrowseTemplatesClick) {
            Text("Browse templates below")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlanSheet(
    onSave: (String, String?) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Drag handle bar
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(2.dp)
                )
                .height(4.dp)
                .width(32.dp)
        )

        Text(
            "Create Plan",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = false
            },
            label = { Text("Plan name *") },
            isError = nameError,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            maxLines = 3
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        onSave(name, description.ifBlank { null })
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }

        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.ime))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlanSheet(
    plan: WorkoutPlan,
    onSave: (String, String?) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(plan.name) }
    var description by remember { mutableStateOf(plan.description.orEmpty()) }
    var nameError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Drag handle bar
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(2.dp)
                )
                .height(4.dp)
                .width(32.dp)
        )

        Text(
            "Edit Plan",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = false
            },
            label = { Text("Plan name *") },
            isError = nameError,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            maxLines = 3
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        onSave(name, description.ifBlank { null })
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }

        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.ime))
    }
}

private fun formatDate(millis: Long): String {
    val dateFormat = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
    return dateFormat.format(Date(millis))
}
