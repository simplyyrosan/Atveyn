package com.atveyn.app.feature.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.atveyn.app.core.designsystem.components.ClockTimePickerDialog
import com.atveyn.app.core.designsystem.components.GlassCard
import com.atveyn.app.core.designsystem.theme.Indigo500
import com.atveyn.app.core.designsystem.theme.StatusAmber
import com.atveyn.app.core.designsystem.theme.StatusAmberBg
import com.atveyn.app.core.designsystem.theme.StatusRed
import com.atveyn.app.core.designsystem.theme.StatusRedBg
import com.atveyn.app.core.util.DateTimeUtils

@Composable
fun TimetableReviewScreen(
    entries: List<ParsedTimetableEntry>,
    onUpdateEntry: (Int, ParsedTimetableEntry) -> Unit,
    onAddEntry: () -> Unit,
    onRemoveEntry: (Int) -> Unit,
    onSave: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var validationErrorDialogMessage by remember { mutableStateOf<String?>(null) }

    val report = remember(entries) {
        TimetableValidator.validateEntries(entries)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Review & Correct",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${entries.size} classes detected. Review before saving.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = onAddEntry,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Slot", fontSize = 13.sp)
            }
        }

        // Validation issues banner (if any errors or warnings exist)
        if (report.issues.isNotEmpty()) {
            val isError = !report.isValid
            val bgColor = if (isError) StatusRedBg else StatusAmberBg
            val iconTint = if (isError) StatusRed else StatusAmber

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        val headerText = if (isError) {
                            "Please resolve errors before saving (${report.errors.size})"
                        } else {
                            "Notice (${report.warnings.size} warning(s))"
                        }
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = iconTint
                        )
                        val sampleMessage = (report.errors.firstOrNull() ?: report.warnings.firstOrNull())?.message ?: ""
                        Text(
                            text = sampleMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(entries, key = { _, item -> item.id }) { index, entry ->
                ReviewEntryCard(
                    entry = entry,
                    onUpdate = { updated -> onUpdateEntry(index, updated) },
                    onDelete = { onRemoveEntry(index) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Button(
                onClick = {
                    if (!report.isValid) {
                        validationErrorDialogMessage = report.errors.firstOrNull()?.message
                            ?: "Please fix invalid entries before saving."
                    } else {
                        showConfirmDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
            ) {
                Text(
                    text = "Confirm & Save Timetable",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }

    // Confirmation Dialog before saving
    if (showConfirmDialog) {
        val validCount = entries.filter { it.subjectName.isNotBlank() }.size
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(text = "Save Timetable?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Are you sure you want to save these $validCount classes to your weekly timetable schedule?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onSave()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Validation Error Dialog if user attempts to save invalid entries
    validationErrorDialogMessage?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = { validationErrorDialogMessage = null },
            title = {
                Text(text = "Cannot Save Timetable", fontWeight = FontWeight.Bold, color = StatusRed)
            },
            text = {
                Text(text = errorMsg)
            },
            confirmButton = {
                TextButton(onClick = { validationErrorDialogMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ReviewEntryCard(
    entry: ParsedTimetableEntry,
    onUpdate: (ParsedTimetableEntry) -> Unit,
    onDelete: () -> Unit
) {
    var dayDropdownExpanded by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val startMin = TimeNormalizer.timeToMinutes(entry.startTime)
    val endMin = TimeNormalizer.timeToMinutes(entry.endTime)
    val hasTimeError = startMin == null || endMin == null || startMin >= endMin
    val hasNameError = entry.subjectName.isBlank()

    val cardBorderModifier = if (hasTimeError || hasNameError) {
        Modifier.border(1.dp, StatusRed.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
    } else {
        Modifier
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(cardBorderModifier)
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
                // Day selector
                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Indigo500.copy(alpha = 0.15f))
                            .clickable { dayDropdownExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = DateTimeUtils.dayOfWeekName(entry.dayOfWeek),
                            color = Indigo500,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
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
                                    dayDropdownExpanded = false
                                    onUpdate(entry.copy(dayOfWeek = d))
                                }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = StatusRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subject Name & Code
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = entry.subjectName,
                    onValueChange = { onUpdate(entry.copy(subjectName = it)) },
                    label = { Text("Subject Name") },
                    isError = hasNameError,
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = entry.subjectCode,
                    onValueChange = { onUpdate(entry.copy(subjectCode = it)) },
                    label = { Text("Code") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time Slots (Clock Picker in 12-hour AM/PM) & Room
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showStartTimePicker = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            "Start Time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Indigo500
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                DateTimeUtils.format12Hour(entry.startTime),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showEndTimePicker = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            "End Time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (hasTimeError) StatusRed else Indigo500
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                DateTimeUtils.format12Hour(entry.endTime),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (hasTimeError) StatusRed else Color.Unspecified
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = entry.room,
                    onValueChange = { onUpdate(entry.copy(room = it)) },
                    label = { Text("Room") },
                    placeholder = { Text("301") },
                    modifier = Modifier.weight(0.9f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (hasTimeError) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Start time must be earlier than end time",
                    color = StatusRed,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp
                )
            }
        }
    }

    if (showStartTimePicker) {
        ClockTimePickerDialog(
            title = "Select Start Time",
            initialTime = entry.startTime,
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = {
                onUpdate(entry.copy(startTime = it))
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        ClockTimePickerDialog(
            title = "Select End Time",
            initialTime = entry.endTime,
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = {
                onUpdate(entry.copy(endTime = it))
                showEndTimePicker = false
            }
        )
    }
}
