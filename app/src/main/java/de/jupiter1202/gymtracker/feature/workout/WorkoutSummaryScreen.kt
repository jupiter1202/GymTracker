package de.jupiter1202.gymtracker.feature.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.jupiter1202.gymtracker.core.UnitConverter
import kotlin.math.roundToInt
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummaryScreen(
    sessionId: Long,
    onDismiss: () -> Unit,
    viewModel: WorkoutLoggingViewModel = koinViewModel()
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val weightUnit by viewModel.weightUnit.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Workout Complete") })
        }
    ) { paddingValues ->
        if (summary == null) {
            // Show loading while summary is being computed
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Session name
                Text(
                    text = summary!!.sessionName,
                    style = MaterialTheme.typography.headlineMedium
                )

                // Duration
                val totalSeconds = summary!!.durationMs / 1000
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60
                Text(
                    text = "Duration: $hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
                )

                // Exercise count
                Text(text = "Exercises: ${summary!!.exerciseCount}")

                // Total sets
                Text(text = "Total sets: ${summary!!.totalSets}")

                // Volume
                val displayVolume = if (weightUnit == "lbs") {
                    UnitConverter.kgToLbs(summary!!.totalVolumeKg).roundToInt()
                } else {
                    summary!!.totalVolumeKg.roundToInt()
                }
                Text(text = "Volume: $displayVolume $weightUnit")

                Spacer(modifier = Modifier.height(32.dp))

                // Done button
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }
}
