package com.sg.taskspace.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sg.taskspace.data.UserPreferences
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VacationModeDialog(
    userPreferences: UserPreferences,
    onDismiss: () -> Unit,
    onSave: (enabled: Boolean, startDate: String, endDate: String, reason: String, freezeHabits: Boolean, pauseReminders: Boolean) -> Unit
) {
    var isEnabled by remember { mutableStateOf(if (userPreferences.isVacationModeEnabled) true else true) }
    var reason by remember { mutableStateOf(userPreferences.vacationReason.ifBlank { "Vacation & Rest" }) }
    var startDateStr by remember {
        mutableStateOf(
            userPreferences.vacationStartDate.ifBlank {
                LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            }
        )
    }
    var endDateStr by remember {
        mutableStateOf(
            userPreferences.vacationEndDate.ifBlank {
                LocalDate.now().plusDays(7).format(DateTimeFormatter.ISO_DATE)
            }
        )
    }
    var freezeHabits by remember { mutableStateOf(userPreferences.freezeHabitStreaks) }
    var pauseReminders by remember { mutableStateOf(userPreferences.pauseTaskReminders) }

    val presetReasons = listOf("Holiday", "Sick", "Travel", "Rest & Relax", "Personal Time")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (userPreferences.isVacationModeEnabled) "Vacation Settings" else "Enable Vacation Mode",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Take a break without losing your habit streaks or being spammed with task reminders.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Reason Selection / Custom Input
                Text(
                    text = "Reason / Note",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetReasons.forEach { preset ->
                        FilterChip(
                            selected = reason == preset,
                            onClick = { reason = preset },
                            label = { Text(preset, fontSize = 12.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (Custom)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Options Switches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Freeze Habit Streaks",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Missed days won't reset your streaks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = freezeHabits,
                        onCheckedChange = { freezeHabits = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pause Notifications",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Silence task reminders while away",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = pauseReminders,
                        onCheckedChange = { pauseReminders = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(isEnabled, startDateStr, endDateStr, reason, freezeHabits, pauseReminders)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (userPreferences.isVacationModeEnabled) "Save Settings" else "Start Vacation")
            }
        },
        dismissButton = {
            if (userPreferences.isVacationModeEnabled) {
                OutlinedButton(
                    onClick = {
                        onSave(false, "", "", "", true, true)
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("End Vacation Now")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
