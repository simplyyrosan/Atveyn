package com.atveyn.app.feature.subjects

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
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
import com.atveyn.app.core.designsystem.theme.AtveynTheme
import com.atveyn.app.core.designsystem.theme.BrandGradient
import com.atveyn.app.core.designsystem.theme.BrandViolet
import com.atveyn.app.core.designsystem.theme.BrandVioletLight
import com.atveyn.app.core.designsystem.theme.BrandVioletSubtle
import com.atveyn.app.core.designsystem.theme.DarkSurfaceBorder
import com.atveyn.app.core.designsystem.theme.DarkSurfaceHighlight
import com.atveyn.app.core.designsystem.theme.GreenGradient
import com.atveyn.app.core.designsystem.theme.LightSurfaceBorder
import com.atveyn.app.core.designsystem.theme.RedGradient
import com.atveyn.app.core.designsystem.theme.StatusAmber
import com.atveyn.app.core.designsystem.theme.StatusGreen
import com.atveyn.app.core.designsystem.theme.StatusPending
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
                    Column {
                        Text(
                            text = "Subjects & Courses",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${uiState.summaries.size} active courses tracked",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (uiState.showArchived) BrandVioletSubtle else AtveynTheme.glass.cardBackgroundHighlight)
                            .border(
                                1.dp,
                                if (uiState.showArchived) BrandViolet.copy(alpha = 0.4f) else if (AtveynTheme.glass.isDark) DarkSurfaceBorder else LightSurfaceBorder,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleShowArchived() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "Toggle Archived",
                                tint = if (uiState.showArchived) BrandVioletLight else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BrandViolet,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp)
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
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
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
                ) {
                    Text("Add First Subject", fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
        BrandViolet
    }

    val progress = summary.attendancePercentage?.let {
        (it / 100.0).toFloat().coerceIn(0f, 1f)
    } ?: 0f

    val (_, progressColor) = when (summary.statusType) {
        AttendanceStatusType.ON_TRACK -> Pair(GreenGradient, StatusGreen)
        AttendanceStatusType.WARNING -> Pair(BrandGradient, StatusAmber)
        AttendanceStatusType.SHORTAGE -> Pair(RedGradient, StatusRed)
        AttendanceStatusType.NO_DATA -> Pair(BrandGradient, Color.Gray)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(subjectColor.copy(alpha = 0.16f))
                            .border(1.dp, subjectColor.copy(alpha = 0.38f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = summary.subjectCode.take(3).ifEmpty { summary.subjectName.take(2) }.uppercase(),
                            color = subjectColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = summary.subjectName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
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
                            modifier = Modifier.size(30.dp)
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

            Spacer(modifier = Modifier.height(16.dp))

            // Percentage & Target Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = summary.attendancePercentage?.let {
                        String.format(Locale.US, "%.1f%%", it)
                    } ?: "N/A",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = progressColor,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Target: ${String.format(Locale.US, "%.0f%%", summary.targetThreshold)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.5.dp)
                    .clip(RoundedCornerShape(3.5.dp)),
                color = progressColor,
                trackColor = Color(0xFF1E202E)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Stats row (Present, Absent, Conducted, Pending)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatCounterChip(label = "Present", count = summary.presentCount, color = StatusGreen, modifier = Modifier.weight(1f))
                StatCounterChip(label = "Absent", count = summary.absentCount, color = StatusRed, modifier = Modifier.weight(1f))
                StatCounterChip(label = "Total", count = summary.conductedCount, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                if (summary.pendingCount > 0) {
                    StatCounterChip(label = "Pending", count = summary.pendingCount, color = StatusPending, modifier = Modifier.weight(1f))
                }
            }

            // Shortage / Safe margin alert pill
            if (summary.statusType == AttendanceStatusType.SHORTAGE && summary.classesNeeded > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(StatusRed.copy(alpha = 0.12f))
                        .border(1.dp, StatusRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⚠️ Must attend next ${summary.classesNeeded} class${if (summary.classesNeeded > 1) "es" else ""} to reach ${String.format(Locale.US, "%.0f%%", summary.targetThreshold)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (summary.statusType == AttendanceStatusType.ON_TRACK && summary.marginClasses > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(StatusGreen.copy(alpha = 0.12f))
                        .border(1.dp, StatusGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "✓ Can safely miss up to ${summary.marginClasses} class${if (summary.marginClasses > 1) "es" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun StatCounterChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AtveynTheme.glass.cardBackgroundHighlight)
            .border(1.dp, if (AtveynTheme.glass.isDark) DarkSurfaceBorder else LightSurfaceBorder, RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$count",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
