package com.sg.taskspace.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String? = null,
    val category: String = "General",
    val priority: String = "Medium", // "High", "Medium", "Low"
    val isCompleted: Boolean = false,

    // Scheduling & Recurrence
    val createdForDate: String, // YYYY-MM-DD (The date this task belongs to)
    val time: String? = null, // HH:mm (Optional time)
    val repeat: String = "None", // "None", "Daily", "Weekly"
    val repeatDayOfWeek: String? = null, // "Monday", "Tuesday" etc. (For Weekly repeat)
    val parentId: String? = null, // ID of the parent repeating task if this is an instance

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
