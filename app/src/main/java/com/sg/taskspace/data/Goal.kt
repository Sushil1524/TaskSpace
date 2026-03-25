package com.sg.taskspace.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("title") val title: String,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("deadlineDate") val deadlineDate: String, // e.g., YYYY-MM-DD
    @SerializedName("isCompleted") val isCompleted: Boolean = false,
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis()
)
