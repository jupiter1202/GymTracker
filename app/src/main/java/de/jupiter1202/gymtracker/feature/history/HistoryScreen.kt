package de.jupiter1202.gymtracker.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.jupiter1202.gymtracker.core.database.entities.WorkoutSession
import de.jupiter1202.gymtracker.ui.theme.EmeraldLabelMuted
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = koinViewModel()) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle(emptyList())

    // Group sessions by month
    val grouped = remember(sessions) {
        sessions.groupBy { session ->
            SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                .format(Date(session.startedAt))
                .uppercase()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                "HISTORY",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (sessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "NO SESSIONS YET",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Complete a workout to see it here",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldLabelMuted
                        )
                    }
                }
            }
        } else {
            // Stats banner
            item {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatChip(
                            label = "TOTAL SESSIONS",
                            value = sessions.size.toString()
                        )
                        VerticalDividerLine()
                        StatChip(
                            label = "THIS MONTH",
                            value = sessions.count { session ->
                                val cal = Calendar.getInstance()
                                val sessionCal = Calendar.getInstance().apply {
                                    timeInMillis = session.startedAt
                                }
                                cal.get(Calendar.MONTH) == sessionCal.get(Calendar.MONTH) &&
                                        cal.get(Calendar.YEAR) == sessionCal.get(Calendar.YEAR)
                            }.toString()
                        )
                    }
                }
            }

            // Grouped sessions
            grouped.forEach { (month, monthSessions) ->
                item(key = month) {
                    Text(
                        month,
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLabelMuted
                    )
                }
                items(monthSessions, key = { it.id }) { session ->
                    SessionCard(session)
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: WorkoutSession) {
    val dayFmt  = SimpleDateFormat("d",    Locale.getDefault())
    val dayName = SimpleDateFormat("EEE",  Locale.getDefault())
    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val date = Date(session.startedAt)

    val durationStr = session.finishedAt?.let { finished ->
        val totalSeconds = (finished - session.startedAt) / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        if (h > 0) "${h}h ${m}m" else "${m}m"
    } ?: "—"

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date block
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(40.dp)
            ) {
                Text(
                    dayFmt.format(date),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    dayName.format(date).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldLabelMuted
                )
            }

            // Session details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    session.name.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetaChip("⏱", durationStr)
                    MetaChip("🕐", timeFmt.format(date))
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = EmeraldLabelMuted
        )
    }
}

@Composable
private fun VerticalDividerLine() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun MetaChip(icon: String, text: String) {
    Text(
        "$icon $text",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = FontFamily.Monospace
    )
}
