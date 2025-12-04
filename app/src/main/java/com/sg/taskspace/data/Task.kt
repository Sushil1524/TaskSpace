package com.sg.taskspace.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

import com.google.gson.annotations.SerializedName

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("title") val title: String,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("category") val category: String = "General",
    @SerializedName("priority") val priority: String = "Medium", // "High", "Medium", "Low"
    @SerializedName("isCompleted") val isCompleted: Boolean = false,

    // Scheduling & Recurrence
    @SerializedName("createdForDate") val createdForDate: String, // YYYY-MM-DD (The date this task belongs to)
    @SerializedName("time") val time: String? = null, // HH:mm (Optional time)
    @SerializedName("repeat") val repeat: String = "None", // "None", "Daily", "Weekly"
    @SerializedName("repeatDayOfWeek") val repeatDayOfWeek: String? = null, // "Monday", "Tuesday" etc. (For Weekly repeat)
    @SerializedName("parentId") val parentId: String? = null, // ID of the parent repeating task if this is an instance

    // Timestamps
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("completedAt") val completedAt: Long? = null
)
