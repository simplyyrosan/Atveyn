package com.atveyn.app.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atveyn.app.core.designsystem.components.GlassCard
import com.atveyn.app.core.designsystem.components.StatusBadge
import com.atveyn.app.core.designsystem.theme.Cyan500
import com.atveyn.app.core.designsystem.theme.Indigo500
import com.atveyn.app.core.designsystem.theme.StatusAmber
import com.atveyn.app.core.designsystem.theme.StatusGreen
import com.atveyn.app.core.designsystem.theme.StatusRed
import com.atveyn.app.core.model.AttendanceStatusType
import com.atveyn.app.core.model.SubjectAttendanceSummary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onNavigateToSubjectDetail: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var simulatorSubjectId by remember(uiState.subjectSummaries) {
        mutableLongStateOf(uiState.subjectSummaries.firstOrNull()?.subjectId ?: -1L)
    }
    var futureAttendCount by remember { mutableIntStateOf(3) }
    var futureMissCount by remember { mutableIntStateOf(0) }
    var subjectDropdownExpanded by remember { mutableStateOf(false) }

    val selectedSub = uiState.subjectSummaries.firstOrNull { it.subjectId == simulatorSubjectId }
        ?: uiState.subjectSummaries.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics & Stats", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Trend Chart Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    TrendChart(
                        weeklyTrends = uiState.weeklyTrends,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Interactive What-If Attendance Simulator
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Attendance Simulator (What-If)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Simulate how future attendance impacts your percentage and target goals.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Subject Selector
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { subjectDropdownExpanded = true }
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = selectedSub?.let { "${it.subjectName} (${it.subjectCode})" } ?: "Select Subject",
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            DropdownMenu(
                                expanded = subjectDropdownExpanded,
                                onDismissRequest = { subjectDropdownExpanded = false }
                            ) {
                                for (sub in uiState.subjectSummaries) {
                                    DropdownMenuItem(
                                        text = { Text("${sub.subjectName} (${sub.subjectCode})") },
                                        onClick = {
                                            simulatorSubjectId = sub.subjectId
                                            subjectDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (selectedSub != null) {
                            Spacer(modifier = Modifier.height(14.dp))

                            // Counter: Attend Next N classes
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Attend Next Classes:", style = MaterialTheme.typography.bodyMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (futureAttendCount > 0) futureAttendCount-- },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                    }
                                    Text(
                                        text = "$futureAttendCount",
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    IconButton(
                                        onClick = { futureAttendCount++ },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase")
                                    }
                                }
                            }

                            // Counter: Miss Next N classes
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Miss Next Classes:", style = MaterialTheme.typography.bodyMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (futureMissCount > 0) futureMissCount-- },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                    }
                                    Text(
                                        text = "$futureMissCount",
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    IconButton(
                                        onClick = { futureMissCount++ },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Simulation Result
                            val simPresent = selectedSub.presentCount + futureAttendCount
                            val simConducted = selectedSub.conductedCount + futureAttendCount + futureMissCount
                            val simPct = if (simConducted == 0) null else (simPresent.toDouble() / simConducted) * 100.0
                            val simColor = when {
                                simPct == null -> Color.Gray
                                simPct >= selectedSub.targetThreshold -> StatusGreen
                                simPct >= selectedSub.targetThreshold - 5.0 -> StatusAmber
                                else -> StatusRed
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(simColor.copy(alpha = 0.12f))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Simulated Attendance:",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = simPct?.let { String.format(Locale.US, "%.1f%%", it) } ?: "N/A",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = simColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Current: ${selectedSub.attendancePercentage?.let { String.format(Locale.US, "%.1f%%", it) } ?: "N/A"}  (Target: ${String.format(Locale.US, "%.0f%%", selectedSub.targetThreshold)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Subject Breakdown Header
            item {
                Text(
                    text = "Subject Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(uiState.subjectSummaries, key = { it.subjectId }) { sub ->
                SubjectStatsBreakdownCard(
                    summary = sub,
                    onClick = { onNavigateToSubjectDetail(sub.subjectId) }
                )
            }
        }
    }
}

@Composable
fun SubjectStatsBreakdownCard(
    summary: SubjectAttendanceSummary,
    onClick: () -> Unit
) {
    val subjectColor = try {
        Color(android.graphics.Color.parseColor(summary.colorHex))
    } catch (e: Exception) {
        Indigo500
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(subjectColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = summary.subjectName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                StatusBadge(status = summary.statusType)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Attendance: ${summary.attendancePercentage?.let { String.format(Locale.US, "%.1f%%", it) } ?: "N/A"}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${summary.presentCount} / ${summary.conductedCount} Conducted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (summary.statusType == AttendanceStatusType.SHORTAGE && summary.classesNeeded > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Need ${summary.classesNeeded} consecutive classes to reach ${String.format(Locale.US, "%.0f%%", summary.targetThreshold)}",
                    color = StatusRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
