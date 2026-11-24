package com.atveyn.app.feature.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atveyn.app.R
import com.atveyn.app.core.designsystem.components.CircularAttendanceProgress
import com.atveyn.app.core.designsystem.components.GlassCard
import com.atveyn.app.core.designsystem.components.SessionStatusBadge
import com.atveyn.app.core.designsystem.components.StatusBadge
import com.atveyn.app.core.designsystem.theme.Cyan500
import com.atveyn.app.core.designsystem.theme.Indigo500
import com.atveyn.app.core.designsystem.theme.StatusAmber
import com.atveyn.app.core.designsystem.theme.StatusCancelled
import com.atveyn.app.core.designsystem.theme.StatusGreen
import com.atveyn.app.core.designsystem.theme.StatusPending
import com.atveyn.app.core.designsystem.theme.StatusRed
import com.atveyn.app.core.model.AttendanceStatusType
import com.atveyn.app.core.model.ClassScheduleItem
import com.atveyn.app.core.model.SessionStatus
import com.atveyn.app.core.model.SubjectAttendanceSummary
import com.atveyn.app.core.util.DateTimeUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToScanner: () -> Unit,
    onNavigateToSubjectDetail: (Long) -> Unit,
    onNavigateToHolidays: () -> Unit,
    onNavigateToTimetable: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()

    val overall = uiState.overallSummary
    val shortageSubjects = uiState.subjectSummaries.filter { it.statusType == AttendanceStatusType.SHORTAGE }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_atveyn_logo),
                            contentDescription = "Atveyn Logo",
                            modifier = Modifier
                                .size(34.dp)
                                .padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = "Atveyn",
                                fontWeight = FontWeight.Bold,
                                fontSize = 23.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = DateTimeUtils.formatDisplayDate(DateTimeUtils.todayDate()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    AnimatedVisibility(visible = canUndo) {
                        OutlinedButton(
                            onClick = { viewModel.undoLastAction() },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = "Undo", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Undo", fontSize = 12.sp)
                        }
                    }
                },
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
            // Overall Attendance Glass Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Overall Attendance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (overall != null) {
                                StatusBadge(status = overall.statusType)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        CircularAttendanceProgress(
                            percentage = overall?.overallPercentage,
                            targetThreshold = overall?.overallTarget ?: 75.0,
                            size = 140.dp,
                            strokeWidth = 13.dp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            DashboardStatChip("Conducted", overall?.totalConducted ?: 0, MaterialTheme.colorScheme.onSurface)
                            DashboardStatChip("Present", overall?.totalPresent ?: 0, StatusGreen)
                            DashboardStatChip("Absent", overall?.totalAbsent ?: 0, StatusRed)
                            DashboardStatChip("Pending", overall?.totalPending ?: 0, StatusPending)
                        }
                    }
                }
            }

            // Shortage Alerts Banner
            if (shortageSubjects.isNotEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = StatusRed.copy(alpha = 0.12f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = StatusRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Attendance Shortage Alert",
                                    color = StatusRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            for (s in shortageSubjects) {
                                Text(
                                    text = "• ${s.subjectName}: ${s.attendancePercentage?.let { String.format(Locale.US, "%.1f%%", it) } ?: "0%"} (Need ${s.classesNeeded} more class${if (s.classesNeeded > 1) "es" else ""} for ${String.format(Locale.US, "%.0f%%", s.targetThreshold)})",
                                    color = StatusRed,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionPill(
                        label = "Scan",
                        icon = Icons.Default.CameraAlt,
                        tint = Indigo500,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToScanner
                    )
                    QuickActionPill(
                        label = "Schedule",
                        icon = Icons.Default.CalendarMonth,
                        tint = Cyan500,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToTimetable
                    )
                    QuickActionPill(
                        label = "Holidays",
                        icon = Icons.Default.DateRange,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToHolidays
                    )
                }
            }

            // Today's Classes Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Classes (${uiState.todayClasses.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "End-of-class alarms active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Today's Classes Items
            if (uiState.todayClasses.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No classes scheduled for today!",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Enjoy your day off or add extra sessions in Timetable.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.todayClasses, key = { it.sessionId }) { item ->
                    TodayClassCard(
                        item = item,
                        onMarkStatus = { status -> viewModel.markSessionStatus(item.sessionId, status) }
                    )
                }
            }

            // Subject-Wise Progress Section
            item {
                Text(
                    text = "Subject Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.subjectSummaries.isEmpty()) {
                item {
                    Text(
                        text = "No subjects added yet. Tap 'Scan' above to import your timetable.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(uiState.subjectSummaries, key = { it.subjectId }) { sub ->
                    MiniSubjectProgressCard(
                        summary = sub,
                        onClick = { onNavigateToSubjectDetail(sub.subjectId) }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardStatChip(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun QuickActionPill(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

@Composable
fun TodayClassCard(
    item: ClassScheduleItem,
    onMarkStatus: (SessionStatus) -> Unit
) {
    val subjectColor = try {
        Color(android.graphics.Color.parseColor(item.colorHex))
    } catch (e: Exception) {
        Indigo500
    }

    val (badgeText, badgeColor, badgeBg) = when (item.status) {
        SessionStatus.PRESENT -> Triple("Present", StatusGreen, StatusGreen.copy(alpha = 0.15f))
        SessionStatus.ABSENT -> Triple("Absent", StatusRed, StatusRed.copy(alpha = 0.15f))
        SessionStatus.CANCELLED -> Triple("Cancelled", StatusCancelled, StatusCancelled.copy(alpha = 0.15f))
        SessionStatus.PENDING -> {
            if (item.isCurrent) {
                Triple("In Progress", Indigo500, Indigo500.copy(alpha = 0.15f))
            } else if (item.isPast) {
                Triple("Pending", StatusPending, StatusPending.copy(alpha = 0.15f))
            } else {
                Triple("Upcoming", StatusCancelled, StatusCancelled.copy(alpha = 0.15f))
            }
        }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(subjectColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (item.isCurrent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Indigo500.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE NOW",
                                color = Indigo500,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                SessionStatusBadge(text = badgeText, textColor = badgeColor, backgroundColor = badgeBg)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = DateTimeUtils.formatTimeRange12h(item.startTime, item.endTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.room.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${item.room}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons (Present, Absent, Cancel)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionMarkButton(
                    label = "Present",
                    isSelected = item.status == SessionStatus.PRESENT,
                    activeColor = StatusGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { onMarkStatus(SessionStatus.PRESENT) }
                )
                ActionMarkButton(
                    label = "Absent",
                    isSelected = item.status == SessionStatus.ABSENT,
                    activeColor = StatusRed,
                    modifier = Modifier.weight(1f),
                    onClick = { onMarkStatus(SessionStatus.ABSENT) }
                )
                ActionMarkButton(
                    label = "Cancel",
                    isSelected = item.status == SessionStatus.CANCELLED,
                    activeColor = StatusCancelled,
                    modifier = Modifier.weight(0.8f),
                    onClick = { onMarkStatus(SessionStatus.CANCELLED) }
                )
            }
        }
    }
}

@Composable
fun ActionMarkButton(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) activeColor else activeColor.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else activeColor
        )
    }
}

@Composable
fun MiniSubjectProgressCard(
    summary: SubjectAttendanceSummary,
    onClick: () -> Unit
) {
    val progress = summary.attendancePercentage?.let { (it / 100.0).toFloat().coerceIn(0f, 1f) } ?: 0f
    val color = when (summary.statusType) {
        AttendanceStatusType.ON_TRACK -> StatusGreen
        AttendanceStatusType.WARNING -> StatusAmber
        AttendanceStatusType.SHORTAGE -> StatusRed
        AttendanceStatusType.NO_DATA -> Color.Gray
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = summary.subjectName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = summary.attendancePercentage?.let { String.format(Locale.US, "%.1f%%", it) } ?: "N/A",
                        fontWeight = FontWeight.Bold,
                        color = color,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.5.dp)),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
