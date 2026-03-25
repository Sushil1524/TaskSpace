package com.sg.taskspace.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("name") val name: String,
    @SerializedName("frequency") val frequency: String = "Daily", // e.g., "Daily", "Weekly"
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "habit_logs")
data class HabitLog(
    @PrimaryKey @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("habitId") val habitId: String,
    @SerializedName("date") val date: String, // YYYY-MM-DD
    @SerializedName("isCompleted") val isCompleted: Boolean = false
)
