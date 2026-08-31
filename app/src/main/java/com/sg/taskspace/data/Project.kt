package com.sg.taskspace.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("colorHex") val colorHex: String = "#6366F1", // Default: Indigo
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("dueDate") val dueDate: String? = null,
    @SerializedName("reminderTimeMillis") val reminderTimeMillis: Long? = null,
    @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @SerializedName("deletedAt") val deletedAt: Long? = null
)
