package com.atveyn.app.feature.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atveyn.app.core.database.entity.ClassSessionEntity
import com.atveyn.app.core.designsystem.components.CircularAttendanceProgress
import com.atveyn.app.core.designsystem.components.ClockTimePickerDialog
import com.atveyn.app.core.designsystem.components.GlassCard
import com.atveyn.app.core.designsystem.components.SessionStatusBadge
import com.atveyn.app.core.designsystem.components.StatusBadge
import com.atveyn.app.core.designsystem.theme.Indigo500
import com.atveyn.app.core.designsystem.theme.StatusAmber
import com.atveyn.app.core.designsystem.theme.StatusCancelled
import com.atveyn.app.core.designsystem.theme.StatusGreen
import com.atveyn.app.core.designsystem.theme.StatusPending
import com.atveyn.app.core.designsystem.theme.StatusRed
import com.atveyn.app.core.model.AttendanceStatusType
import com.atveyn.app.core.model.SessionStatus
import com.atveyn.app.core.util.AttendanceCalculator
import com.atveyn.app.core.util.DateTimeUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subjectId: Long,
    viewModel: SubjectsViewModel,
    subjectRepository: com.atveyn.app.domain.repository.SubjectRepository,
    attendanceRepository: com.atveyn.app.domain.repository.AttendanceRepository,
    onNavigateBack: () -> Unit
) {
    val subject by subjectRepository.getSubjectById(subjectId).collectAsState(initial = null)
    val sessions by attendanceRepository.getSessionsForSubject(subjectId).collectAsState(initial = emptyList())
    val scheduledDays by viewModel.getScheduledDaysForSubject(subjectId).collectAsState(initial = emptySet())

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddPastClassDialog by remember { mutableStateOf(false) }
    var prefillDateForAddDialog by remember { mutableStateOf(DateTimeUtils.todayDate()) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Calendar state
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDateForAction by remember { mutableStateOf<LocalDate?>(null) }
    var showMoreInfo by remember { mutableStateOf(false) }

    val currentSubject = subject
    if (currentSubject == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading Subject...")
        }
        return
    }

    // Dynamic overall stats
    var present = 0
    var absent = 0
    var pending = 0
    var cancelled = 0
    for (s in sessions) {
        when (s.status) {
            SessionStatus.PRESENT -> present++
            SessionStatus.ABSENT -> absent++
            SessionStatus.PENDING -> pending++
            SessionStatus.CANCELLED -> cancelled++
        }
    }
    val conducted = AttendanceCalculator.calculateConducted(present, absent)
    val percentage = AttendanceCalculator.calculatePercentage(present, absent)
    val status = AttendanceCalculator.calculateStatusType(percentage, currentSubject.targetAttendancePercentage)
    val classesNeeded = AttendanceCalculator.calculateClassesNeeded(currentSubject.targetAttendancePercentage, present, conducted)
    val marginClasses = AttendanceCalculator.calculateMarginClasses(currentSubject.targetAttendancePercentage, present, conducted)
    val historySessions = sessions.filter { it.status != SessionStatus.PENDING || DateTimeUtils.isPastTime(it.date, it.endTime) }

    // Monthly stats calculation
    val monthSessions = sessions.filter { s ->
        try {
            val date = LocalDate.parse(s.date)
            YearMonth.from(date) == currentYearMonth
        } catch (e: Exception) {
            false
        }
    }
    val monthPresent = monthSessions.count { it.status == SessionStatus.PRESENT }
    val monthAbsent = monthSessions.count { it.status == SessionStatus.ABSENT }
    val monthCancelled = monthSessions.count { it.status == SessionStatus.CANCELLED }
    val monthConducted = monthPresent + monthAbsent
    val monthPercentageStr = if (monthConducted > 0) {
        String.format(Locale.US, "%.1f%%", (monthPresent.toDouble() / monthConducted) * 100.0)
    } else {
        "N/A"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentSubject.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            maxLines = 1
                        )
                        if (currentSubject.code.isNotBlank() && currentSubject.code != currentSubject.name) {
                            Text(
                                text = "Code: ${currentSubject.code}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Subject") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    showEditDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Log Custom Class") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    prefillDateForAddDialog = DateTimeUtils.todayDate()
                                    showAddPastClassDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (currentSubject.isArchived) "Unarchive Subject" else "Archive Subject") },
                                leadingIcon = {
                                    Icon(
                                        if (currentSubject.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.setArchived(currentSubject.id, !currentSubject.isArchived)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Subject", color = StatusRed) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusRed) },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteConfirm = true
                                }
                            )
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Calendar Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Month Selector Header: < PREV    NOV 2026    NEXT >
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(com.atveyn.app.core.designsystem.theme.BrandVioletSubtle)
                                    .border(1.dp, com.atveyn.app.core.designsystem.theme.BrandViolet.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                    .clickable { currentYearMonth = currentYearMonth.minusMonths(1) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ChevronLeft,
                                        contentDescription = "Previous Month",
                                        tint = com.atveyn.app.core.designsystem.theme.BrandVioletLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "PREV",
                                        color = com.atveyn.app.core.designsystem.theme.BrandVioletLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }

                            Text(
                                text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(com.atveyn.app.core.designsystem.theme.BrandVioletSubtle)
                                    .border(1.dp, com.atveyn.app.core.designsystem.theme.BrandViolet.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                    .clickable { currentYearMonth = currentYearMonth.plusMonths(1) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "NEXT",
                                        color = com.atveyn.app.core.designsystem.theme.BrandVioletLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Next Month",
                                        tint = com.atveyn.app.core.designsystem.theme.BrandVioletLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Days of Week Row: SUN MON TUE WED THU FRI SAT
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val daysOfWeekInfo = listOf(
                                "SUN" to 7,
                                "MON" to 1,
                                "TUE" to 2,
                                "WED" to 3,
                                "THU" to 4,
                                "FRI" to 5,
                                "SAT" to 6
                            )
                            for ((dayName, dayVal) in daysOfWeekInfo) {
                                val isScheduled = dayVal in scheduledDays
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .then(
                                            if (isScheduled) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                            else Modifier
                                        )
                                        .padding(vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayName,
                                        textAlign = TextAlign.Center,
                                        fontWeight = if (isScheduled) FontWeight.ExtraBold else FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isScheduled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Calendar Grid with Swipe Gestures
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(currentYearMonth) {
                                    var totalDrag = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = { totalDrag = 0f },
                                        onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                                        onDragEnd = {
                                            if (totalDrag > 60f) {
                                                currentYearMonth = currentYearMonth.minusMonths(1)
                                            } else if (totalDrag < -60f) {
                                                currentYearMonth = currentYearMonth.plusMonths(1)
                                            }
                                        }
                                    )
                                }
                        ) {
                            CalendarGridView(
                                currentYearMonth = currentYearMonth,
                                sessions = sessions,
                                scheduledDays = scheduledDays,
                                onDateClick = { date ->
                                    selectedDateForAction = date
                                }
                            )
                        }

                        if (scheduledDays.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                                        .border(
                                            0.5.dp,
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Dimmed days have scheduled classes in timetable",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Overall Attendance Summary Card (Default View)
            item {
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
                            Column {
                                Text(
                                    text = "Overall Attendance",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Target: ${String.format(Locale.US, "%.0f%%", currentSubject.targetAttendancePercentage)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            StatusBadge(status = status)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CircularAttendanceProgress(
                                percentage = percentage,
                                targetThreshold = currentSubject.targetAttendancePercentage,
                                size = 96.dp,
                                strokeWidth = 9.dp
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MonthlyStatChip(
                                        label = "Present",
                                        count = present,
                                        color = StatusGreen
                                    )
                                    MonthlyStatChip(
                                        label = "Absent",
                                        count = absent,
                                        color = StatusRed
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MonthlyStatChip(
                                        label = "Cancelled",
                                        count = cancelled,
                                        color = StatusCancelled
                                    )
                                    MonthlyStatChip(
                                        label = "Conducted",
                                        count = conducted,
                                        color = Indigo500
                                    )
                                }
                            }
                        }

                        if (status == AttendanceStatusType.SHORTAGE && classesNeeded > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "⚠️ Attend next $classesNeeded class${if (classesNeeded > 1) "es" else ""} consecutively to reach ${String.format(Locale.US, "%.0f%%", currentSubject.targetAttendancePercentage)} target.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusRed,
                                fontWeight = FontWeight.Medium
                            )
                        } else if (status == AttendanceStatusType.ON_TRACK && marginClasses > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "✓ You can miss $marginClasses class${if (marginClasses > 1) "es" else ""} and still remain at or above target.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Overall : ${percentage?.let { String.format(Locale.US, "%.1f%%", it) } ?: "N/A"}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = when {
                                    conducted == 0 || percentage == null -> MaterialTheme.colorScheme.onSurface
                                    percentage >= currentSubject.targetAttendancePercentage -> StatusGreen
                                    else -> StatusRed
                                }
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(com.atveyn.app.core.designsystem.theme.AtveynTheme.glass.cardBackgroundHighlight)
                                    .border(
                                        1.dp,
                                        if (com.atveyn.app.core.designsystem.theme.AtveynTheme.glass.isDark)
                                            com.atveyn.app.core.designsystem.theme.DarkSurfaceBorder
                                        else
                                            com.atveyn.app.core.designsystem.theme.LightSurfaceBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { showMoreInfo = !showMoreInfo }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (showMoreInfo) Icons.Default.KeyboardArrowUp else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (showMoreInfo) "Hide Info" else "More Info",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Expanded "More Info" Section: Attendance for this month
            if (showMoreInfo) {
                item {
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
                                Text(
                                    text = "Attendance for ${currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = monthPercentageStr,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        monthConducted == 0 -> MaterialTheme.colorScheme.onSurface
                                        (monthPresent.toDouble() / monthConducted) * 100.0 >= currentSubject.targetAttendancePercentage -> StatusGreen
                                        else -> StatusRed
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MonthlyStatChip(
                                    label = "Present",
                                    count = monthPresent,
                                    color = StatusGreen
                                )
                                MonthlyStatChip(
                                    label = "Absent",
                                    count = monthAbsent,
                                    color = StatusRed
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MonthlyStatChip(
                                    label = "Cancelled",
                                    count = monthCancelled,
                                    color = StatusCancelled
                                )
                                MonthlyStatChip(
                                    label = "Conducted",
                                    count = monthConducted,
                                    color = Indigo500
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Tip : Swipe left-right on the calendar to change month",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Historical Sessions List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Session History (${historySessions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedButton(
                        onClick = {
                            prefillDateForAddDialog = DateTimeUtils.todayDate()
                            showAddPastClassDialog = true
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Past Class", fontSize = 12.sp)
                    }
                }
            }

            // Sessions List
            if (historySessions.isEmpty()) {
                item {
                    Text(
                        text = "No sessions logged yet for this subject. Tap any date above to mark attendance.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(historySessions, key = { it.id }) { session ->
                    HistoricalSessionItem(
                        session = session,
                        onStatusChange = { newStatus ->
                            viewModel.updateSessionStatus(session.id, newStatus)
                        },
                        onDeleteSession = {
                            viewModel.deleteSession(session.id)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Date Action Dialog (Present, Absent, Cancelled, Clear)
    selectedDateForAction?.let { date ->
        val dateStr = DateTimeUtils.formatDate(date)
        val daySessions = sessions.filter { it.date == dateStr }
        val currentDayStatus = when {
            daySessions.any { it.status == SessionStatus.PRESENT } -> SessionStatus.PRESENT
            daySessions.any { it.status == SessionStatus.ABSENT } -> SessionStatus.ABSENT
            daySessions.any { it.status == SessionStatus.CANCELLED } -> SessionStatus.CANCELLED
            daySessions.any { it.status == SessionStatus.PENDING } -> SessionStatus.PENDING
            else -> null
        }

        AttendanceActionDialog(
            date = date,
            currentStatus = currentDayStatus,
            sessions = daySessions,
            onDismiss = { selectedDateForAction = null },
            onMarkStatus = { status ->
                viewModel.markDateAttendance(currentSubject.id, dateStr, status)
                selectedDateForAction = null
            },
            onClear = {
                viewModel.clearDateAttendance(currentSubject.id, dateStr)
                selectedDateForAction = null
            },
            onAddCustomSession = {
                prefillDateForAddDialog = dateStr
                selectedDateForAction = null
                showAddPastClassDialog = true
            }
        )
    }

    if (showEditDialog) {
        AddEditSubjectDialog(
            editingSubject = currentSubject,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, code, color, threshold ->
                viewModel.updateSubject(
                    currentSubject.copy(
                        name = name,
                        code = code,
                        colorHex = color,
                        targetAttendancePercentage = threshold
                    )
                )
                showEditDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Subject") },
            text = { Text("Are you sure you want to delete '${currentSubject.name}'? Timetable slots and past sessions will be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubject(currentSubject.id)
                        showDeleteConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddPastClassDialog) {
        AddPastClassDialog(
            initialDate = prefillDateForAddDialog,
            onDismiss = { showAddPastClassDialog = false },
            onConfirm = { date, start, end, status ->
                viewModel.logPastClass(currentSubject.id, date, start, end, status)
                showAddPastClassDialog = false
            }
        )
    }
}

@Composable
fun CalendarGridView(
    currentYearMonth: YearMonth,
    sessions: List<ClassSessionEntity>,
    scheduledDays: Set<Int> = emptySet(),
    onDateClick: (LocalDate) -> Unit
) {
    val daysInMonth = currentYearMonth.lengthOfMonth()
    val firstOfMonth = currentYearMonth.atDay(1)
    // Sunday-first: Sunday = 0, Monday = 1, ..., Saturday = 6
    val startDayOffset = if (firstOfMonth.dayOfWeek == DayOfWeek.SUNDAY) 0 else firstOfMonth.dayOfWeek.value
    val totalSlots = startDayOffset + daysInMonth
    val numRows = (totalSlots + 6) / 7
    val today = LocalDate.now()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (r in 0 until numRows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (c in 0 until 7) {
                    val slotIndex = r * 7 + c
                    val dayNumber = slotIndex - startDayOffset + 1

                    if (dayNumber in 1..daysInMonth) {
                        val date = currentYearMonth.atDay(dayNumber)
                        val dateStr = DateTimeUtils.formatDate(date)
                        val isToday = date == today
                        val isScheduledDay = date.dayOfWeek.value in scheduledDays

                        val daySessions = sessions.filter { it.date == dateStr }
                        val status = when {
                            daySessions.any { it.status == SessionStatus.PRESENT } -> SessionStatus.PRESENT
                            daySessions.any { it.status == SessionStatus.ABSENT } -> SessionStatus.ABSENT
                            daySessions.any { it.status == SessionStatus.CANCELLED } -> SessionStatus.CANCELLED
                            daySessions.any { it.status == SessionStatus.PENDING } -> SessionStatus.PENDING
                            else -> null
                        }

                        CalendarDayCell(
                            dayNumber = dayNumber,
                            status = status,
                            isToday = isToday,
                            isScheduledDay = isScheduledDay,
                            modifier = Modifier.weight(1f),
                            onClick = { onDateClick(date) }
                        )
                    } else {
                        // Empty slot
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                                .border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    dayNumber: Int,
    status: SessionStatus?,
    isToday: Boolean,
    isScheduledDay: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor = when (status) {
        SessionStatus.PRESENT -> StatusGreen
        SessionStatus.ABSENT -> StatusRed
        SessionStatus.CANCELLED -> StatusCancelled
        SessionStatus.PENDING -> StatusPending.copy(alpha = 0.5f)
        null -> if (isScheduledDay) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        }
    }

    val textColor = when (status) {
        SessionStatus.PRESENT, SessionStatus.ABSENT, SessionStatus.CANCELLED -> Color.White
        else -> if (isScheduledDay) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
    }

    val borderColor = when {
        isToday -> com.atveyn.app.core.designsystem.theme.BrandVioletLight
        status == null && isScheduledDay -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
        status == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        else -> Color.Transparent
    }

    val borderWidth = if (isToday) 2.dp else if (status == null && isScheduledDay) 1.dp else 0.5.dp

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$dayNumber",
                color = textColor,
                fontWeight = if (status != null || isToday || isScheduledDay) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
            if (isScheduledDay && status == null) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(3.5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
                )
            }
        }
    }
}

@Composable
fun AttendanceActionDialog(
    date: LocalDate,
    currentStatus: SessionStatus?,
    sessions: List<ClassSessionEntity>,
    onDismiss: () -> Unit,
    onMarkStatus: (SessionStatus) -> Unit,
    onClear: () -> Unit,
    onAddCustomSession: () -> Unit
) {
    val displayDate = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.US))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (currentStatus != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Current: ${currentStatus.name}",
                        fontSize = 12.sp,
                        color = when (currentStatus) {
                            SessionStatus.PRESENT -> StatusGreen
                            SessionStatus.ABSENT -> StatusRed
                            SessionStatus.CANCELLED -> StatusCancelled
                            SessionStatus.PENDING -> StatusPending
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (sessions.isNotEmpty()) {
                    Text(
                        text = "Classes on this day:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    sessions.forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = DateTimeUtils.formatTimeRange12h(s.startTime, s.endTime),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = s.status.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (s.status) {
                                    SessionStatus.PRESENT -> StatusGreen
                                    SessionStatus.ABSENT -> StatusRed
                                    SessionStatus.CANCELLED -> StatusCancelled
                                    SessionStatus.PENDING -> StatusPending
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Action: Present
                ActionRowButton(
                    icon = Icons.Default.Check,
                    text = "Present",
                    color = Color(0xFF16A34A),
                    isSelected = currentStatus == SessionStatus.PRESENT,
                    onClick = { onMarkStatus(SessionStatus.PRESENT) }
                )

                // Action: Absent
                ActionRowButton(
                    icon = Icons.Default.Close,
                    text = "Absent",
                    color = Color(0xFFDC2626),
                    isSelected = currentStatus == SessionStatus.ABSENT,
                    onClick = { onMarkStatus(SessionStatus.ABSENT) }
                )

                // Action: Cancelled
                ActionRowButton(
                    icon = Icons.Default.Block,
                    text = "Cancelled / Holiday",
                    color = Color(0xFFF97316),
                    isSelected = currentStatus == SessionStatus.CANCELLED,
                    onClick = { onMarkStatus(SessionStatus.CANCELLED) }
                )

                // Action: Clear
                ActionRowButton(
                    icon = Icons.Default.DeleteOutline,
                    text = "Clear Attendance",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    isSelected = false,
                    onClick = onClear
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Custom extra session
                TextButton(
                    onClick = onAddCustomSession,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Custom Time Class", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ActionRowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) color.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) color else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun MonthlyStatChip(
    label: String,
    count: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label : $count",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun HistoricalSessionItem(
    session: ClassSessionEntity,
    onStatusChange: (SessionStatus) -> Unit,
    onDeleteSession: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val (badgeText, badgeColor, badgeBg) = when (session.status) {
        SessionStatus.PRESENT -> Triple("Present", StatusGreen, StatusGreen.copy(alpha = 0.15f))
        SessionStatus.ABSENT -> Triple("Absent", StatusRed, StatusRed.copy(alpha = 0.15f))
        SessionStatus.CANCELLED -> Triple("Cancelled", StatusCancelled, StatusCancelled.copy(alpha = 0.15f))
        SessionStatus.PENDING -> Triple("Pending", StatusPending, StatusPending.copy(alpha = 0.15f))
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DateTimeUtils.formatDisplayDate(session.date),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${DateTimeUtils.formatTimeRange12h(session.startTime, session.endTime)}  •  ${session.source.name.replace('_', ' ')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SessionStatusBadge(text = badgeText, textColor = badgeColor, backgroundColor = badgeBg)

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Change Status")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mark Present") },
                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = StatusGreen) },
                            onClick = {
                                menuExpanded = false
                                onStatusChange(SessionStatus.PRESENT)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mark Absent") },
                            leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = StatusRed) },
                            onClick = {
                                menuExpanded = false
                                onStatusChange(SessionStatus.ABSENT)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mark Cancelled") },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = StatusCancelled) },
                            onClick = {
                                menuExpanded = false
                                onStatusChange(SessionStatus.CANCELLED)
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete Session", color = StatusRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusRed) },
                            onClick = {
                                menuExpanded = false
                                onDeleteSession()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleLarge,
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
fun AddPastClassDialog(
    initialDate: String = DateTimeUtils.todayDate(),
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, SessionStatus) -> Unit
) {
    var date by remember { mutableStateOf(initialDate) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var status by remember { mutableStateOf(SessionStatus.PRESENT) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Past Class", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Class Timing", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showStartTimePicker = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Column {
                            Text(
                                "Start Time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Indigo500
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    DateTimeUtils.format12Hour(startTime),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showEndTimePicker = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Column {
                            Text(
                                "End Time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Indigo500
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    DateTimeUtils.format12Hour(endTime),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Text("Status", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (s in listOf(SessionStatus.PRESENT, SessionStatus.ABSENT, SessionStatus.CANCELLED)) {
                        val isSelected = status == s
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Indigo500 else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { status = s }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = s.name,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(date.trim(), startTime.trim(), endTime.trim(), status) },
                colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
            ) {
                Text("Log Class")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showStartTimePicker) {
        ClockTimePickerDialog(
            title = "Select Start Time",
            initialTime = startTime,
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = {
                startTime = it
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        ClockTimePickerDialog(
            title = "Select End Time",
            initialTime = endTime,
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = {
                endTime = it
                showEndTimePicker = false
            }
        )
    }
}
