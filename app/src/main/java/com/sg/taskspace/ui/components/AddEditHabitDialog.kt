package com.sg.taskspace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sg.taskspace.data.Habit

private val ColorOptions = listOf(
    "#10B981", // Emerald
    "#6366F1", // Indigo
    "#F43F5E", // Rose
    "#F59E0B", // Amber
    "#06B6D4", // Cyan
    "#8B5CF6", // Violet
    "#F97316"  // Orange
)

private val DaysOfWeek = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitDialog(
    initialHabit: Habit? = null,
    onSave: (Habit) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialHabit?.name ?: "") }
    var notes by remember { mutableStateOf(initialHabit?.notes ?: "") }
    var type by remember { mutableStateOf(initialHabit?.type ?: "BUILD") }
    var targetValueText by remember { mutableStateOf((initialHabit?.targetValue ?: 1f).toInt().toString()) }
    var unit by remember { mutableStateOf(initialHabit?.unit ?: "") }
    var scheduleType by remember { mutableStateOf(initialHabit?.scheduleType ?: "DAILY") }

    val initialDaysList = remember(initialHabit) {
        initialHabit?.selectedDays?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }
    var selectedDaysList by remember { mutableStateOf(initialDaysList) }
    var weeklyTargetCount by remember { mutableStateOf(initialHabit?.weeklyTargetCount ?: 3) }
    var intervalDays by remember { mutableStateOf(initialHabit?.intervalDays ?: 2) }
    var selectedColor by remember { mutableStateOf(initialHabit?.colorHex ?: "#10B981") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialHabit == null) "New Habit" else "Edit Habit",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Habit Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Habit Name") },
                        placeholder = { Text("e.g. Morning Workout, Drink Water") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Habit Type Selector
                    Column {
                        Text(
                            text = "Habit Type",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = type == "BUILD",
                                onClick = { type = "BUILD" },
                                label = { Text("🟢 Build Habit") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = type == "QUIT",
                                onClick = { type = "QUIT" },
                                label = { Text("🛑 Quit Habit") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = type == "NUMERIC",
                                onClick = { type = "NUMERIC" },
                                label = { Text("📊 Target Goal") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Numeric Target Inputs (only if type == "NUMERIC")
                    if (type == "NUMERIC") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = targetValueText,
                                onValueChange = { targetValueText = it },
                                label = { Text("Target Goal") },
                                placeholder = { Text("8") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = unit,
                                onValueChange = { unit = it },
                                label = { Text("Unit") },
                                placeholder = { Text("Glasses, Pages, Mins") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Schedule Picker
                    Column {
                        Text(
                            text = "Schedule",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val options = listOf(
                                "DAILY" to "Daily",
                                "SPECIFIC_DAYS" to "Days",
                                "TIMES_PER_WEEK" to "X/Week",
                                "INTERVAL" to "Interval"
                            )
                            options.forEach { (key, label) ->
                                FilterChip(
                                    selected = scheduleType == key,
                                    onClick = { scheduleType = key },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        when (scheduleType) {
                            "SPECIFIC_DAYS" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    DaysOfWeek.forEach { day ->
                                        val isSelected = day in selectedDaysList
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedDaysList = if (isSelected) {
                                                    selectedDaysList - day
                                                } else {
                                                    selectedDaysList + day
                                                }
                                            },
                                            label = { Text(day.take(2), fontSize = 11.sp) },
                                            modifier = Modifier.size(38.dp, 36.dp)
                                        )
                                    }
                                }
                            }
                            "TIMES_PER_WEEK" -> {
                                Column {
                                    Text(
                                        text = "$weeklyTargetCount times per week",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Slider(
                                        value = weeklyTargetCount.toFloat(),
                                        onValueChange = { weeklyTargetCount = it.toInt() },
                                        valueRange = 1f..7f,
                                        steps = 5
                                    )
                                }
                            }
                            "INTERVAL" -> {
                                Column {
                                    Text(
                                        text = "Every $intervalDays days",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Slider(
                                        value = intervalDays.toFloat(),
                                        onValueChange = { intervalDays = it.toInt() },
                                        valueRange = 2f..14f,
                                        steps = 11
                                    )
                                }
                            }
                        }
                    }

                    // Color Picker
                    Column {
                        Text(
                            text = "Color Theme",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ColorOptions.forEach { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = selectedColor.equals(hex, ignoreCase = true)

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Motivation (Optional)") },
                        placeholder = { Text("Why do you want to build or quit this habit?") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Action Buttons
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    val targetVal = targetValueText.toFloatOrNull()?.coerceAtLeast(1f) ?: 1f
                                    val habitToSave = (initialHabit ?: Habit(name = name)).copy(
                                        name = name.trim(),
                                        type = type,
                                        targetValue = targetVal,
                                        unit = unit.trim(),
                                        scheduleType = scheduleType,
                                        selectedDays = selectedDaysList.joinToString(","),
                                        weeklyTargetCount = weeklyTargetCount,
                                        intervalDays = intervalDays,
                                        colorHex = selectedColor,
                                        notes = notes.trim()
                                    )
                                    onSave(habitToSave)
                                }
                            },
                            enabled = name.isNotBlank(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Habit")
                        }
                    }
                }
            }
        }
    }
}
