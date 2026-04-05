package de.jupiter1202.gymtracker.feature.workout

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
import de.jupiter1202.gymtracker.core.UnitConverter
import de.jupiter1202.gymtracker.ui.theme.EmeraldLabelMuted
import kotlin.math.roundToInt
import org.koin.androidx.compose.koinViewModel

@Composable
fun WorkoutSummaryScreen(
    sessionId: Long,
    onDismiss: () -> Unit,
    viewModel: WorkoutLoggingViewModel = koinViewModel()
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val weightUnit by viewModel.weightUnit.collectAsStateWithLifecycle()

    if (summary == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val s = summary!!
    val totalSeconds = s.durationMs / 1000
    val hours   = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val durationStr = if (hours > 0)
        "%d:%02d:%02d".format(hours, minutes, seconds)
    else
        "%d:%02d".format(minutes, seconds)

    val displayVolume = if (weightUnit == "lbs")
        UnitConverter.kgToLbs(s.totalVolumeKg).roundToInt()
    else
        s.totalVolumeKg.roundToInt()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    "WORKOUT",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "COMPLETE",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    s.sessionName.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Stats grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SummaryStatCard(
                        label = "DURATION",
                        value = durationStr,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        label = "EXERCISES",
                        value = s.exerciseCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SummaryStatCard(
                        label = "TOTAL SETS",
                        value = s.totalSets.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        label = "VOLUME ($weightUnit)",
                        value = displayVolume.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Done button
        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor   = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("DONE", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SummaryStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldLabelMuted
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
