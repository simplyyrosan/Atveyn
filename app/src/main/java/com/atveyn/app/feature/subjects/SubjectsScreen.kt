package com.atveyn.app.feature.subjects

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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import com.atveyn.app.core.database.entity.SubjectEntity
import com.atveyn.app.core.designsystem.components.GlassCard
import com.atveyn.app.core.designsystem.components.StatusBadge
import com.atveyn.app.core.designsystem.theme.Indigo500
import com.atveyn.app.core.designsystem.theme.StatusAmber
import com.atveyn.app.core.designsystem.theme.StatusGreen
import com.atveyn.app.core.designsystem.theme.StatusRed
import com.atveyn.app.core.model.AttendanceStatusType
import com.atveyn.app.core.model.SubjectAttendanceSummary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    viewModel: SubjectsViewModel,
    onNavigateToSubjectDetail: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSubjectSummary by remember { mutableStateOf<SubjectAttendanceSummary?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Subjects", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleShowArchived() }) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Toggle Archived",
                            tint = if (uiState.showArchived) Indigo500 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Indigo500,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (uiState.summaries.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No Subjects Added Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Create subjects manually or import your schedule using the timetable scanner.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
                ) {
                    Text("Add First Subject")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.summaries, key = { it.subjectId }) { summary ->
                    SubjectCard(
                        summary = summary,
                        onClick = { onNavigateToSubjectDetail(summary.subjectId) },
                        onEdit = { editingSubjectSummary = summary }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditSubjectDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, code, color, threshold ->
                viewModel.addSubject(name, code, color, threshold)
                showAddDialog = false
            }
        )
    }

    val editing = editingSubjectSummary
    if (editing != null) {
        AddEditSubjectDialog(
            editingSubject = SubjectEntity(
                id = editing.subjectId,
                name = editing.subjectName,
                code = editing.subjectCode,
                colorHex = editing.colorHex,
                targetAttendancePercentage = editing.targetThreshold
            ),
            onDismiss = { editingSubjectSummary = null },
            onConfirm = { name, code, color, threshold ->
                viewModel.updateSubject(
                    SubjectEntity(
                        id = editing.subjectId,
                        name = name,
                        code = code,
                        colorHex = color,
                        targetAttendancePercentage = threshold
                    )
                )
                editingSubjectSummary = null
            }
        )
    }
}

@Composable
fun SubjectCard(
    summary: SubjectAttendanceSummary,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    val subjectColor = try {
        Color(android.graphics.Color.parseColor(summary.colorHex))
    } catch (e: Exception) {
        Indigo500
    }

    val progress = summary.attendancePercentage?.let {
        (it / 100.0).toFloat().coerceIn(0f, 1f)
    } ?: 0f

    val progressColor = when (summary.statusType) {
        AttendanceStatusType.ON_TRACK -> StatusGreen
        AttendanceStatusType.WARNING -> StatusAmber
        AttendanceStatusType.SHORTAGE -> StatusRed
        AttendanceStatusType.NO_DATA -> Color.Gray
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
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(subjectColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = summary.subjectName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (summary.subjectCode.isNotBlank()) {
                            Text(
                                text = summary.subjectCode,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status = summary.statusType)
                    if (onEdit != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Subject",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Percentage & Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = summary.attendancePercentage?.let {
                        String.format(Locale.US, "%.1f%%", it)
                    } ?: "N/A",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
                Text(
                    text = "Target: ${String.format(Locale.US, "%.0f%%", summary.targetThreshold)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Stats row (Present, Absent, Conducted, Pending)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCounterChip(label = "Present", count = summary.presentCount, color = StatusGreen)
                StatCounterChip(label = "Absent", count = summary.absentCount, color = StatusRed)
                StatCounterChip(label = "Conducted", count = summary.conductedCount, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (summary.pendingCount > 0) {
                    StatCounterChip(label = "Pending", count = summary.pendingCount, color = Color(0xFF8B5CF6))
                }
            }

            // Shortage / Safe margin alert text
            if (summary.statusType == AttendanceStatusType.SHORTAGE && summary.classesNeeded > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "⚠️ Must attend next ${summary.classesNeeded} class${if (summary.classesNeeded > 1) "es" else ""} to reach ${String.format(Locale.US, "%.0f%%", summary.targetThreshold)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusRed,
                    fontWeight = FontWeight.Medium
                )
            } else if (summary.statusType == AttendanceStatusType.ON_TRACK && summary.marginClasses > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "✓ Can safely miss up to ${summary.marginClasses} class${if (summary.marginClasses > 1) "es" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun StatCounterChip(
    label: String,
    count: Int,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
