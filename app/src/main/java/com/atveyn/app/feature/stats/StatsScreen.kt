package com.atveyn.app.feature.stats

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Remove
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
import com.atveyn.app.core.designsystem.theme.AtveynTheme
import com.atveyn.app.core.designsystem.theme.BrandGradient
import com.atveyn.app.core.designsystem.theme.BrandViolet
import com.atveyn.app.core.designsystem.theme.BrandVioletLight
import com.atveyn.app.core.designsystem.theme.Cyan500
import com.atveyn.app.core.designsystem.theme.DarkSurfaceBorder
import com.atveyn.app.core.designsystem.theme.DarkSurfaceHighlight
import com.atveyn.app.core.designsystem.theme.GreenGradient
import com.atveyn.app.core.designsystem.theme.LightSurfaceBorder
import com.atveyn.app.core.designsystem.theme.RedGradient
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
                title = {
                    Column {
                        Text(
                            text = "Analytics & Simulator",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Trends and what-if simulation engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            // Trend Chart Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    TrendChart(
                        weeklyTrends = uiState.weeklyTrends,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }

            // Interactive What-If Attendance Simulator
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Attendance Simulator (What-If)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Predict how future classes impact your target",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrandViolet.copy(alpha = 0.15f))
                                    .border(1.dp, BrandViolet.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "AI ENGINE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandVioletLight
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Subject Selector Pill
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AtveynTheme.glass.cardBackgroundHighlight)
                                    .border(BorderStroke(1.dp, if (AtveynTheme.glass.isDark) DarkSurfaceBorder else LightSurfaceBorder), RoundedCornerShape(14.dp))
                                    .clickable { subjectDropdownExpanded = true }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedSub?.let { "${it.subjectName} (${it.subjectCode})" } ?: "Select Subject",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 13.5.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AtveynTheme.glass.cardBackgroundHighlight)
                                    .border(BorderStroke(1.dp, if (AtveynTheme.glass.isDark) DarkSurfaceBorder else LightSurfaceBorder), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(StatusGreen)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Attend Next Classes",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF262838))
                                            .clickable { if (futureAttendCount > 0) futureAttendCount-- },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = "$futureAttendCount",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(StatusGreen.copy(alpha = 0.2f))
                                            .border(1.dp, StatusGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .clickable { futureAttendCount++ },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = StatusGreen, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Counter: Miss Next N classes
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AtveynTheme.glass.cardBackgroundHighlight)
                                    .border(BorderStroke(1.dp, if (AtveynTheme.glass.isDark) DarkSurfaceBorder else LightSurfaceBorder), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(StatusRed)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Miss Next Classes",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF262838))
                                            .clickable { if (futureMissCount > 0) futureMissCount-- },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = "$futureMissCount",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(StatusRed.copy(alpha = 0.2f))
                                            .border(1.dp, StatusRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .clickable { futureMissCount++ },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = StatusRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Simulation Result Box
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
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(simColor.copy(alpha = 0.12f))
                                    .border(1.dp, simColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Simulated Attendance Result:",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = simPct?.let { String.format(Locale.US, "%.1f%%", it) } ?: "N/A",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = simColor,
                                            letterSpacing = (-0.5).sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Current: ${selectedSub.attendancePercentage?.let { String.format(Locale.US, "%.1f%%", it) } ?: "N/A"}  •  Target: ${String.format(Locale.US, "%.0f%%", selectedSub.targetThreshold)}",
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
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
        BrandViolet
    }

    val progress = summary.attendancePercentage?.let { (it / 100.0).toFloat().coerceIn(0f, 1f) } ?: 0f
    val (_, statusColor) = when (summary.statusType) {
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
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(subjectColor.copy(alpha = 0.16f))
                            .border(1.dp, subjectColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = summary.subjectCode.take(2).ifEmpty { summary.subjectName.take(2) }.uppercase(),
                            color = subjectColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = summary.subjectName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                StatusBadge(status = summary.statusType)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = summary.attendancePercentage?.let { String.format(Locale.US, "%.1f%%", it) } ?: "N/A",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = statusColor,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "${summary.presentCount} / ${summary.conductedCount} classes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = Color(0xFF1E202E)
            )

            if (summary.statusType == AttendanceStatusType.SHORTAGE && summary.classesNeeded > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "⚠️ Need ${summary.classesNeeded} consecutive classes for ${String.format(Locale.US, "%.0f%%", summary.targetThreshold)}",
                    color = StatusRed,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else if (summary.statusType == AttendanceStatusType.ON_TRACK && summary.marginClasses > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "✓ Can miss up to ${summary.marginClasses} class${if (summary.marginClasses > 1) "es" else ""}",
                    color = StatusGreen,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
