package com.atveyn.app.feature.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.atveyn.app.core.database.entity.SubjectEntity
import com.atveyn.app.core.designsystem.components.ClockTimePickerDialog
import com.atveyn.app.core.designsystem.theme.Indigo500
import com.atveyn.app.core.model.DayTimetableItem
import com.atveyn.app.core.util.DateTimeUtils

@Composable
fun AddEditTimetableDialog(
    initialDay: Int,
    subjects: List<SubjectEntity>,
    editingItem: DayTimetableItem? = null,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, Int, String, String, String) -> Unit
) {
    var selectedSubjectId by remember {
        mutableLongStateOf(editingItem?.subjectId ?: (subjects.firstOrNull()?.id ?: -1L))
    }
    var dayOfWeek by remember {
        mutableIntStateOf(editingItem?.dayOfWeek ?: initialDay)
    }
    var startTime by remember {
        mutableStateOf(editingItem?.startTime ?: "09:00")
    }
    var endTime by remember {
        mutableStateOf(editingItem?.endTime ?: "10:00")
    }
    var room by remember {
        mutableStateOf(editingItem?.room ?: "")
    }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    var subjectDropdownExpanded by remember { mutableStateOf(false) }
    var dayDropdownExpanded by remember { mutableStateOf(false) }

    val selectedSubject = subjects.firstOrNull { it.id == selectedSubjectId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingItem != null) "Edit Class Slot" else "Add Class Slot",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Subject Picker
                Text("Subject", style = MaterialTheme.typography.labelMedium)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { subjectDropdownExpanded = true }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedSubject != null) {
                                val color = try {
                                    Color(android.graphics.Color.parseColor(selectedSubject.colorHex))
                                } catch (e: Exception) {
                                    Indigo500
                                }
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${selectedSubject.name} (${selectedSubject.code})",
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text("Select Subject", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = subjectDropdownExpanded,
                        onDismissRequest = { subjectDropdownExpanded = false }
                    ) {
                        for (sub in subjects) {
                            DropdownMenuItem(
                                text = { Text("${sub.name} (${sub.code})") },
                                onClick = {
                                    selectedSubjectId = sub.id
                                    subjectDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Day Picker
                Text("Day of Week", style = MaterialTheme.typography.labelMedium)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { dayDropdownExpanded = true }
                            .padding(14.dp)
                    ) {
                        Text(
                            text = DateTimeUtils.dayOfWeekName(dayOfWeek),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    DropdownMenu(
                        expanded = dayDropdownExpanded,
                        onDismissRequest = { dayDropdownExpanded = false }
                    ) {
                        for (d in 1..7) {
                            DropdownMenuItem(
                                text = { Text(DateTimeUtils.dayOfWeekName(d)) },
                                onClick = {
                                    dayOfWeek = d
                                    dayDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Time Slots with Analog Clock Selector (12-hour AM/PM format)
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

                // Room
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room / Location (Optional)") },
                    placeholder = { Text("Room 302, Lab 1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedSubjectId != -1L && startTime.isNotBlank() && endTime.isNotBlank()) {
                        onConfirm(
                            editingItem?.entryId ?: 0L,
                            selectedSubjectId,
                            dayOfWeek,
                            startTime.trim(),
                            endTime.trim(),
                            room.trim()
                        )
                    }
                },
                enabled = selectedSubjectId != -1L,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
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

@Composable
fun AddExtraClassDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String, String, String) -> Unit
) {
    var selectedSubjectId by remember {
        mutableLongStateOf(subjects.firstOrNull()?.id ?: -1L)
    }
    var date by remember {
        mutableStateOf(DateTimeUtils.todayDate())
    }
    var startTime by remember {
        mutableStateOf("11:00")
    }
    var endTime by remember {
        mutableStateOf("12:00")
    }
    var room by remember {
        mutableStateOf("")
    }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    var subjectDropdownExpanded by remember { mutableStateOf(false) }
    val selectedSubject = subjects.firstOrNull { it.id == selectedSubjectId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Schedule Extra Class", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Subject", style = MaterialTheme.typography.labelMedium)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { subjectDropdownExpanded = true }
                            .padding(14.dp)
                    ) {
                        Text(
                            text = selectedSubject?.let { "${it.name} (${it.code})" } ?: "Select Subject",
                            fontWeight = FontWeight.Medium
                        )
                    }

                    DropdownMenu(
                        expanded = subjectDropdownExpanded,
                        onDismissRequest = { subjectDropdownExpanded = false }
                    ) {
                        for (sub in subjects) {
                            DropdownMenuItem(
                                text = { Text("${sub.name} (${sub.code})") },
                                onClick = {
                                    selectedSubjectId = sub.id
                                    subjectDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Time Slots with Analog Clock Selector (12-hour AM/PM format)
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

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedSubjectId != -1L) {
                        onConfirm(selectedSubjectId, date.trim(), startTime.trim(), endTime.trim(), room.trim())
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
            ) {
                Text("Add Class")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
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
