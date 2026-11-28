package com.atveyn.app.feature.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atveyn.app.R
import com.atveyn.app.core.designsystem.components.CircularAttendanceProgress
import com.atveyn.app.core.designsystem.components.GlassCard
import com.atveyn.app.core.designsystem.components.GlassCardHighlight
import com.atveyn.app.core.designsystem.components.SessionStatusBadge
import com.atveyn.app.core.designsystem.components.StatusBadge
import com.atveyn.app.core.designsystem.theme.AtveynTheme
import com.atveyn.app.core.designsystem.theme.BrandGradient
import com.atveyn.app.core.designsystem.theme.BrandViolet
import com.atveyn.app.core.designsystem.theme.BrandVioletLight
import com.atveyn.app.core.designsystem.theme.BrandVioletSubtle
import com.atveyn.app.core.designsystem.theme.Cyan500
import com.atveyn.app.core.designsystem.theme.DarkSurfaceBorder
import com.atveyn.app.core.designsystem.theme.DarkSurfaceHighlight
import com.atveyn.app.core.designsystem.theme.GreenGradient
import com.atveyn.app.core.designsystem.theme.Indigo500
import com.atveyn.app.core.designsystem.theme.LightSurfaceBorder
import com.atveyn.app.core.designsystem.theme.RedGradient
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
                                .size(36.dp)
                                .padding(end = 10.dp)
                        )
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Atveyn",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(BrandVioletSubtle)
                                        .border(1.dp, BrandViolet.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "PRO",
                                        color = BrandVioletLight,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
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
                            border = BorderStroke(1.dp, BrandViolet.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandVioletLight),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Undo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
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
                            Column {
                                Text(
                                    text = "Overall Attendance",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Aggregated across all registered subjects",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (overall != null) {
                                StatusBadge(status = overall.statusType)
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        CircularAttendanceProgress(
                            percentage = overall?.overallPercentage,
                            targetThreshold = overall?.overallTarget ?: 75.0,
                            size = 148.dp,
                            strokeWidth = 13.dp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Fluent Metric Stat Chips (Squircle Card Style)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FluentStatChip(
                                label = "Total",
                                count = overall?.totalConducted ?: 0,
                                accentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            FluentStatChip(
                                label = "Present",
                                count = overall?.totalPresent ?: 0,
                                accentColor = StatusGreen,
                                modifier = Modifier.weight(1f)
                            )
                            FluentStatChip(
                                label = "Absent",
                                count = overall?.totalAbsent ?: 0,
                                accentColor = StatusRed,
                                modifier = Modifier.weight(1f)
                            )
                            FluentStatChip(
                                label = "Pending",
                                count = overall?.totalPending ?: 0,
                                accentColor = StatusPending,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Shortage Alerts Banner
            if (shortageSubjects.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(StatusRed.copy(alpha = 0.12f))
                            .border(BorderStroke(1.dp, StatusRed.copy(alpha = 0.35f)), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StatusRed.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = StatusRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Attendance Shortage Alert",
                                        color = StatusRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${shortageSubjects.size} subject(s) currently below target threshold",
                                        color = StatusRed.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            for (s in shortageSubjects) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(StatusRed)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${s.subjectName}: ${s.attendancePercentage?.let { String.format(Locale.US, "%.1f%%", it) } ?: "0%"} (Need ${s.classesNeeded} more class${if (s.classesNeeded > 1) "es" else ""} for ${String.format(Locale.US, "%.0f%%", s.targetThreshold)})",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
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
                        label = "Scan OCR",
                        icon = Icons.Default.CameraAlt,
                        tint = BrandViolet,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToScanner
                    )
                    QuickActionPill(
                        label = "Timetable",
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Today's Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AtveynTheme.glass.cardBackgroundHighlight)
                                .border(1.dp, if (AtveynTheme.glass.isDark) DarkSurfaceBorder else LightSurfaceBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${uiState.todayClasses.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandVioletLight
                            )
                        }
                    }
                    Text(
                        text = "End-of-class reminders on",
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
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
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
                items(uiState.todayClasses, key = { "class_${it.sessionId}" }) { item ->
                    TodayClassCard(
                        item = item,
                        onMarkStatus = { status -> viewModel.markSessionStatus(item.sessionId, status) }
                    )
                }
            }

            // Subject-Wise Progress Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subject Performance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${uiState.subjectSummaries.size} Subjects",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.subjectSummaries.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No subjects added yet. Tap 'Scan OCR' above to import your timetable.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                items(uiState.subjectSummaries, key = { "subj_${it.subjectId}" }) { sub ->
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
fun FluentStatChip(
    label: String,
    count: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AtveynTheme.glass.cardBackgroundHighlight)
            .border(BorderStroke(1.dp, if (AtveynTheme.glass.isDark) DarkSurfaceBorder else LightSurfaceBorder), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$count",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(AtveynTheme.glass.cardBackgroundHighlight)
            .border(BorderStroke(1.dp, if (AtveynTheme.glass.isDark) DarkSurfaceBorder else LightSurfaceBorder), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
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
        BrandViolet
    }

    val (badgeText, badgeColor, badgeBg) = when (item.status) {
        SessionStatus.PRESENT -> Triple("Present", StatusGreen, StatusGreen.copy(alpha = 0.15f))
        SessionStatus.ABSENT -> Triple("Absent", StatusRed, StatusRed.copy(alpha = 0.15f))
        SessionStatus.CANCELLED -> Triple("Cancelled", StatusCancelled, StatusCancelled.copy(alpha = 0.15f))
        SessionStatus.PENDING -> {
            if (item.isCurrent) {
                Triple("In Progress", BrandViolet, BrandVioletSubtle)
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
                .padding(16.dp)
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(subjectColor.copy(alpha = 0.18f))
                            .border(1.dp, subjectColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.subjectCode.take(3).ifEmpty { item.subjectName.take(2) }.uppercase(),
                            color = subjectColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.subjectName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (item.isCurrent) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(BrandVioletSubtle)
                                        .border(1.dp, BrandViolet.copy(alpha = 0.35f), RoundedCornerShape(5.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        color = BrandVioletLight,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = DateTimeUtils.formatTimeRange12h(item.startTime, item.endTime),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (item.room.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${item.room}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                SessionStatusBadge(text = badgeText, textColor = badgeColor, backgroundColor = badgeBg)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons (Present, Absent, Cancel) - Fluent Capsule Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FluentActionButton(
                    label = "Present",
                    icon = Icons.Default.Check,
                    isSelected = item.status == SessionStatus.PRESENT,
                    accentColor = StatusGreen,
                    modifier = Modifier.weight(1.1f),
                    onClick = { onMarkStatus(SessionStatus.PRESENT) }
                )
                FluentActionButton(
                    label = "Absent",
                    icon = Icons.Default.Close,
                    isSelected = item.status == SessionStatus.ABSENT,
                    accentColor = StatusRed,
                    modifier = Modifier.weight(1.1f),
                    onClick = { onMarkStatus(SessionStatus.ABSENT) }
                )
                FluentActionButton(
                    label = "Cancel",
                    icon = null,
                    isSelected = item.status == SessionStatus.CANCELLED,
                    accentColor = StatusCancelled,
                    modifier = Modifier.weight(0.9f),
                    onClick = { onMarkStatus(SessionStatus.CANCELLED) }
                )
            }
        }
    }
}

@Composable
fun FluentActionButton(
    label: String,
    icon: ImageVector?,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (isSelected) accentColor else AtveynTheme.glass.cardBackgroundHighlight
    val border = if (isSelected) accentColor else if (AtveynTheme.glass.isDark) DarkSurfaceBorder else LightSurfaceBorder
    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(BorderStroke(1.dp, border), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

@Composable
fun MiniSubjectProgressCard(
    summary: SubjectAttendanceSummary,
    onClick: () -> Unit
) {
    val progress = summary.attendancePercentage?.let { (it / 100.0).toFloat().coerceIn(0f, 1f) } ?: 0f
    val (_, color) = when (summary.statusType) {
        AttendanceStatusType.ON_TRACK -> Pair(GreenGradient, StatusGreen)
        AttendanceStatusType.WARNING -> Pair(BrandGradient, StatusAmber)
        AttendanceStatusType.SHORTAGE -> Pair(RedGradient, StatusRed)
        AttendanceStatusType.NO_DATA -> Pair(Brush.horizontalGradient(listOf(Color.Gray, Color.DarkGray)), Color.Gray)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.subjectName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${summary.presentCount} / ${summary.conductedCount} attended • Target ${String.format(Locale.US, "%.0f%%", summary.targetThreshold)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.14f))
                        .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = summary.attendancePercentage?.let { String.format(Locale.US, "%.1f%%", it) } ?: "N/A",
                        fontWeight = FontWeight.Bold,
                        color = color,
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = Color(0xFF1E202E)
            )
        }
    }
}
