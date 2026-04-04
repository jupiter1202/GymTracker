package de.jupiter1202.gymtracker.feature.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.jupiter1202.gymtracker.feature.exercises.ExerciseViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatePreviewScreen(
    templateId: String,
    onNavigateBack: () -> Unit,
    onImported: (Long) -> Unit
) {
    val planViewModel = koinViewModel<WorkoutPlanViewModel>()
    val exerciseViewModel = koinViewModel<ExerciseViewModel>()
    
    val templates by planViewModel.templates.collectAsStateWithLifecycle()
    val exercises by exerciseViewModel.exercises.collectAsStateWithLifecycle()
    
    val program = templates.find { it.id == templateId }
    var importing by remember { mutableStateOf(false) }
    
    // If template not found, navigate back
    LaunchedEffect(program) {
        if (program == null && templates.isNotEmpty()) {
            onNavigateBack()
        }
    }
    
    // Handle loading state
    if (templates.isEmpty()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Loading...") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }
    
    if (program == null) {
        return
    }
    
    // Build exercise lookup map from all exercises
    val exerciseLookup = exercises.associate { it.name.lowercase() to it.id }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(program.name) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Button(
                    onClick = {
                        importing = true
                        planViewModel.importTemplate(program, exerciseLookup) { planId ->
                            onImported(planId)
                        }
                    },
                    enabled = !importing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(if (importing) "Importing..." else "Use this program")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Description item
            item {
                Text(
                    text = program.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Section divider with "Exercises" label
            item {
                HorizontalDivider(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth()
                )
            }
            
            // Days and exercises
            program.days.forEach { day ->
                stickyHeader {
                    Text(
                        text = day.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                items(day.exercises) { exercise ->
                    ListItem(
                        headlineContent = { Text(exercise.exerciseName) },
                        trailingContent = {
                            Text(
                                text = "${exercise.targetSets} × ${exercise.targetReps}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }
            }
            
            // Spacer for button clearance
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
