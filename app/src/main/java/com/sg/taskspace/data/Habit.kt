package com.sg.taskspace.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("name") val name: String,
    @SerializedName("frequency") val frequency: String = "Daily", // Legacy fallback
    @SerializedName("type") val type: String = "BUILD", // BUILD, QUIT, NUMERIC
    @SerializedName("targetValue") val targetValue: Float = 1f,
    @SerializedName("unit") val unit: String = "",
    @SerializedName("scheduleType") val scheduleType: String = "DAILY", // DAILY, SPECIFIC_DAYS, TIMES_PER_WEEK, INTERVAL
    @SerializedName("selectedDays") val selectedDays: String = "", // e.g. "MON,WED,FRI"
    @SerializedName("weeklyTargetCount") val weeklyTargetCount: Int = 3,
    @SerializedName("intervalDays") val intervalDays: Int = 1,
    @SerializedName("colorHex") val colorHex: String = "#10B981",
    @SerializedName("iconName") val iconName: String = "Check",
    @SerializedName("notes") val notes: String = "",
    @SerializedName("reminderTime") val reminderTime: String? = null,
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @SerializedName("deletedAt") val deletedAt: Long? = null
)

@Entity(tableName = "habit_logs")
data class HabitLog(
    @PrimaryKey @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("habitId") val habitId: String,
    @SerializedName("date") val date: String, // YYYY-MM-DD
    @SerializedName("isCompleted") val isCompleted: Boolean = false,
    @SerializedName("value") val value: Float = 1f,
    @SerializedName("relapseNotes") val relapseNotes: String? = null
)
